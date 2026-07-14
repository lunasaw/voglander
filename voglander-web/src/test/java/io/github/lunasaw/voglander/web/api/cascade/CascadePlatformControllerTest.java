package io.github.lunasaw.voglander.web.api.cascade;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeClientScheduler;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import io.github.lunasaw.voglander.manager.manager.CascadePlatformManager;
import io.github.lunasaw.voglander.web.api.cascade.assembler.CascadeWebAssembler;

/**
 * CascadePlatformController 纯 Mockito + MockMvc 单元测试。
 *
 * <p>验证 CRUD/分页端点连通，以及启用/停用端点对 manager 与调度器的编排顺序。</p>
 *
 * @author luna
 */
@DisplayName("CascadePlatformController 单元测试")
@ExtendWith(MockitoExtension.class)
class CascadePlatformControllerTest {

    @InjectMocks
    CascadePlatformController controller;
    @Mock
    CascadePlatformManager       cascadePlatformManager;
    @Mock
    CascadeClientScheduler       cascadeClientScheduler;
    @Mock
    VoglanderSipClientProperties sipClientProperties;
    @Spy
    CascadeWebAssembler          cascadeWebAssembler = new CascadeWebAssembler();

    MockMvc                      mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("新增：委托 manager.add 并返回 id")
    void add_ok() throws Exception {
        when(cascadePlatformManager.add(any(CascadePlatformDTO.class))).thenReturn(88L);
        mvc.perform(post("/api/v1/cascade/platform")
            .contentType(MediaType.APPLICATION_JSON).content("{\"platformId\":\"34020000002000000001\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(88));
        verify(cascadePlatformManager).add(any(CascadePlatformDTO.class));
    }

    @Test
    @DisplayName("更新：回填路径 id 后委托 manager.update")
    void update_ok() throws Exception {
        when(cascadePlatformManager.update(any(CascadePlatformDTO.class))).thenReturn(true);
        mvc.perform(put("/api/v1/cascade/platform/5")
            .contentType(MediaType.APPLICATION_JSON).content("{\"platformIp\":\"10.0.0.1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
        verify(cascadePlatformManager).update(argThat(d -> Long.valueOf(5L).equals(d.getId())));
    }

    @Test
    @DisplayName("删除：委托 manager.delete")
    void delete_ok() throws Exception {
        when(cascadePlatformManager.delete(5L)).thenReturn(true);
        mvc.perform(delete("/api/v1/cascade/platform/5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
        verify(cascadePlatformManager).delete(5L);
    }

    @Test
    @DisplayName("详情：委托 manager.getById 返回 VO")
    void getById_ok() throws Exception {
        CascadePlatformDTO dto = new CascadePlatformDTO();
        dto.setId(5L);
        when(cascadePlatformManager.getById(5L)).thenReturn(dto);
        mvc.perform(get("/api/v1/cascade/platform/5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(5));
        verify(cascadePlatformManager).getById(5L);
    }

    @Test
    @DisplayName("分页：POST /getPage 委托 manager.getPage，返回 total + items")
    void page_ok() throws Exception {
        when(cascadePlatformManager.getPage(any(CascadePlatformDTO.class), eq(1), eq(20)))
            .thenReturn(new Page<>(1, 20));
        mvc.perform(post("/api/v1/cascade/platform/getPage")
            .param("page", "1").param("size", "20")
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0))
            .andExpect(jsonPath("$.data.items").isArray());
        verify(cascadePlatformManager).getPage(any(CascadePlatformDTO.class), eq(1), eq(20));
    }

    @Test
    @DisplayName("启用：成功后启动平台注册调度")
    void enable_starts_scheduler() throws Exception {
        CascadePlatformDTO dto = new CascadePlatformDTO();
        dto.setId(5L);
        when(cascadePlatformManager.enablePlatform(5L)).thenReturn(true);
        when(cascadePlatformManager.getById(5L)).thenReturn(dto);

        mvc.perform(post("/api/v1/cascade/platform/5/enable"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));

        verify(cascadePlatformManager).enablePlatform(5L);
        verify(cascadeClientScheduler).startPlatform(dto);
    }

    @Test
    @DisplayName("启用失败：不启动调度")
    void enable_failed_no_scheduler() throws Exception {
        when(cascadePlatformManager.enablePlatform(5L)).thenReturn(false);
        mvc.perform(post("/api/v1/cascade/platform/5/enable"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(false));
        verify(cascadeClientScheduler, never()).startPlatform(any());
    }

    @Test
    @DisplayName("停用：先停调度再停用 manager")
    void disable_stops_scheduler_then_manager() throws Exception {
        when(cascadePlatformManager.disablePlatform(5L)).thenReturn(true);
        mvc.perform(post("/api/v1/cascade/platform/5/disable"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));

        org.mockito.InOrder inOrder = inOrder(cascadeClientScheduler, cascadePlatformManager);
        inOrder.verify(cascadeClientScheduler).stopPlatform(5L);
        inOrder.verify(cascadePlatformManager).disablePlatform(5L);
    }

    @Test
    @DisplayName("刷新：委托调度器 refreshRegistrations")
    void refresh_ok() throws Exception {
        mvc.perform(post("/api/v1/cascade/platform/refresh"))
            .andExpect(status().isOk());
        verify(cascadeClientScheduler).refreshRegistrations();
    }
}
