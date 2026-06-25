package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.voglander.common.constant.cascade.CascadeConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import io.github.lunasaw.voglander.manager.manager.CascadePlatformManager;

/**
 * D3 次生问题红线：空库 + {@code @PostConstruct} 竞态下，{@link CascadeClientScheduler#refreshRegistrations()}
 * 查 {@code tb_cascade_platform} 抛 {@code no such table} 不应导致启动失败。
 * <p>
 * 与 {@code SqliteSchemaInitializer} 无依赖顺序保证，调度器查询必须对「表不存在」容错（跳过 + 告警），
 * 不把次生 DB 异常上抛成启动阻断。
 *
 * <p>另含 C1 拆分（注册续期 + 独立保活）相关行为验证。
 *
 * @author luna
 */
@DisplayName("CascadeClientScheduler — 启动容错 + 注册保活拆分(C1)")
@ExtendWith(MockitoExtension.class)
class CascadeClientSchedulerTest {

    @Mock
    private CascadePlatformManager cascadePlatformManager;

    @Mock
    private CascadeDeviceSupplier   cascadeDeviceSupplier;

    @Test
    @DisplayName("getPage 抛 no such table → refreshRegistrations 不上抛，启动不阻断")
    void refreshRegistrations_whenTableMissing_shouldNotPropagate() {
        when(cascadePlatformManager.getPage(org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new org.springframework.jdbc.BadSqlGrammarException(
                    "select", "SELECT ...", new java.sql.SQLException("[SQLITE_ERROR] SQL error or missing database (no such table: tb_cascade_platform)")));

        CascadeClientScheduler scheduler =
            new CascadeClientScheduler(cascadePlatformManager, cascadeDeviceSupplier);

        assertDoesNotThrow(scheduler::refreshRegistrations,
            "空库下查 tb_cascade_platform 缺表异常应被容错吞掉并告警，不阻断启动");
    }

    @Test
    @DisplayName("stopPlatform 应置 OFFLINE（取消注册 + 保活两套任务）")
    void stopPlatform_should_set_offline() {
        CascadePlatformDTO dto = new CascadePlatformDTO();
        dto.setId(1L);
        dto.setPlatformId("PF1");
        dto.setRegisterExpires(3600);
        dto.setKeepaliveInterval(60);
        lenient().when(cascadePlatformManager.getById(1L)).thenReturn(dto);

        CascadeClientScheduler scheduler =
            new CascadeClientScheduler(cascadePlatformManager, cascadeDeviceSupplier);

        assertDoesNotThrow(() -> scheduler.startPlatform(dto));
        scheduler.stopPlatform(1L);

        verify(cascadePlatformManager, atLeastOnce()).updateRegisterStatus(1L, CascadeConstant.RegisterStatus.OFFLINE);
    }
}
