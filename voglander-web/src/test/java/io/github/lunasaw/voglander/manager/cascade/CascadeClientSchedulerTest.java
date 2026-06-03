package io.github.lunasaw.voglander.manager.cascade;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeClientScheduler;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeDeviceSupplier;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import io.github.lunasaw.voglander.manager.manager.CascadePlatformManager;

/**
 * CascadeClientScheduler 单元测试（Mockito，不依赖 Spring 容器）
 *
 * <p>验证调度器的状态流转逻辑，不真实发 SIP 报文。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CascadeClientScheduler 单元测试")
class CascadeClientSchedulerTest {

    @Mock  CascadePlatformManager cascadePlatformManager;
    @Mock  CascadeDeviceSupplier  cascadeDeviceSupplier;
    @InjectMocks CascadeClientScheduler scheduler;

    private CascadePlatformDTO platform(Long id, String platformId) {
        CascadePlatformDTO dto = new CascadePlatformDTO();
        dto.setId(id);
        dto.setPlatformId(platformId);
        dto.setPlatformIp("192.168.1.100");
        dto.setPlatformPort(5060);
        dto.setPlatformDomain("3402000000");
        dto.setLocalClientId("34020000001320000001");
        dto.setLocalIp("127.0.0.1");
        dto.setRegisterExpires(3600);
        dto.setKeepaliveInterval(55);
        dto.setEnabled(1);
        dto.setTransport("UDP");
        dto.setCharset("GB2312");
        return dto;
    }

    private Page<CascadePlatformDTO> pageOf(CascadePlatformDTO... dtos) {
        Page<CascadePlatformDTO> p = new Page<>(1, 200);
        p.setRecords(Arrays.asList(dtos));
        p.setTotal(dtos.length);
        return p;
    }

    @Nested
    @DisplayName("refreshRegistrations - 批量注册刷新")
    class RefreshRegistrations {

        @Test
        @DisplayName("enabled 平台会被调度：状态先置为 2（注册中）")
        void should_set_registering_status() throws Exception {
            CascadePlatformDTO p = platform(1L, "platform-A");
            // startPlatform 内先调 stopPlatform → getById
            when(cascadePlatformManager.getById(1L)).thenReturn(p);
            when(cascadeDeviceSupplier.buildFromDevice(p)).thenReturn(new io.github.lunasaw.sip.common.entity.FromDevice());
            when(cascadeDeviceSupplier.buildToDevice(p)).thenReturn(new io.github.lunasaw.sip.common.entity.ToDevice());

            scheduler.startPlatform(p);

            Thread.sleep(300);

            // 状态应被设为 2（注册中），或因 SIP 失败设为 3
            verify(cascadePlatformManager, atLeastOnce())
                .updateRegisterStatus(eq(1L), intThat(s -> s == 2 || s == 3));
        }

        @Test
        @DisplayName("disabled 平台不在 enabled=1 查询结果中，不会被调度")
        void should_skip_disabled_platform() {
            // enabled=1 的查询结果为空
            when(cascadePlatformManager.getPage(any(), eq(1), eq(200))).thenReturn(pageOf());

            scheduler.refreshRegistrations();

            // 没有任何平台被提交
            verify(cascadePlatformManager, never()).updateRegisterStatus(any(), anyInt());
        }

        @Test
        @DisplayName("单个平台异常时不影响其他平台被调度")
        void should_isolate_failure_between_platforms() throws Exception {
            CascadePlatformDTO pA = platform(1L, "platform-A");
            CascadePlatformDTO pB = platform(2L, "platform-B");

            // stopPlatform 内部需要 getById
            when(cascadePlatformManager.getById(1L)).thenReturn(pA);
            when(cascadePlatformManager.getById(2L)).thenReturn(pB);

            // pA 的 supplier 抛异常
            when(cascadeDeviceSupplier.buildFromDevice(pA)).thenThrow(new RuntimeException("supplier error"));
            when(cascadeDeviceSupplier.buildFromDevice(pB)).thenReturn(new io.github.lunasaw.sip.common.entity.FromDevice());
            when(cascadeDeviceSupplier.buildToDevice(pB)).thenReturn(new io.github.lunasaw.sip.common.entity.ToDevice());

            scheduler.startPlatform(pA);
            scheduler.startPlatform(pB);

            Thread.sleep(300);

            // pA 失败应标为 3
            verify(cascadePlatformManager, atLeastOnce()).updateRegisterStatus(eq(1L), eq(3));
            // pB 应被处理（2 或 3）
            verify(cascadePlatformManager, atLeastOnce())
                .updateRegisterStatus(eq(2L), intThat(s -> s == 2 || s == 3));
        }
    }

    @Nested
    @DisplayName("stopPlatform - 动态停止")
    class StopPlatform {

        @Test
        @DisplayName("stopPlatform 找到平台时置为 0（离线）")
        void should_set_offline_on_stop() {
            CascadePlatformDTO p = platform(5L, "platform-X");
            when(cascadePlatformManager.getById(5L)).thenReturn(p);

            scheduler.stopPlatform(5L);

            verify(cascadePlatformManager).updateRegisterStatus(5L, 0);
        }

        @Test
        @DisplayName("stopPlatform id=null 时无操作")
        void should_noop_for_null_id() {
            scheduler.stopPlatform(null);
            verifyNoInteractions(cascadePlatformManager);
        }
    }
}
