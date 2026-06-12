package io.github.lunasaw.voglander.service.command;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.client.service.device.DeviceCommandService;
import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import io.github.lunasaw.voglander.common.enums.DeviceProtocolEnum;

/**
 * S1（PROTOCOL-ARCHITECTURE-GENERICITY）出站 {@code DeviceCommandService} SPI 化红线测试。
 * <p>
 * 验证 {@link DeviceAgreementService} 由「硬编码 if」改为「按协议自动注册的路由表」后：
 * <ul>
 * <li>按设备协议（agreement type）路由到声明了对应纯协议的 {@link DeviceCommandService} 实现；</li>
 * <li>GB28181_IPC / GB28181_NVR 两种型态都路由到同一 GB28181 服务（路由键为纯协议）；</li>
 * <li>未注册协议 / 入参为空 抛异常；</li>
 * <li>构造期两个实现声明同协议 → 启动即报错（防止静默覆盖）。</li>
 * </ul>
 * 新增协议只需新增一个声明 {@code supportProtocols()} 的实现，本服务零改动。
 *
 * @author luna
 */
@DisplayName("S1 — 出站命令服务 SPI 路由")
class DeviceAgreementServiceTest {

    private DeviceCommandService gbService;

    @BeforeEach
    void setUp() {
        gbService = mock(DeviceCommandService.class);
        // GB28181 实现声明仅支持 GB28181 纯协议
        lenient().when(gbService.supportProtocols())
            .thenReturn(Set.of(DeviceProtocolEnum.GB28181.getType()));
    }

    @Test
    @DisplayName("GB28181_IPC 路由到 GB28181 命令服务")
    void getCommandService_gb28181Ipc_routesToGbService() {
        DeviceAgreementService service = new DeviceAgreementService(List.of(gbService));

        DeviceCommandService resolved = service.getCommandService(DeviceAgreementEnum.GB28181_IPC.getType());

        assertNotNull(resolved);
        assertSame(gbService, resolved);
    }

    @Test
    @DisplayName("GB28181_NVR 与 IPC 路由到同一 GB28181 服务（键为纯协议）")
    void getCommandService_gb28181Nvr_sameServiceAsIpc() {
        DeviceAgreementService service = new DeviceAgreementService(List.of(gbService));

        DeviceCommandService ipc = service.getCommandService(DeviceAgreementEnum.GB28181_IPC.getType());
        DeviceCommandService nvr = service.getCommandService(DeviceAgreementEnum.GB28181_NVR.getType());

        assertSame(ipc, nvr);
        assertSame(gbService, nvr);
    }

    @Test
    @DisplayName("入参为空抛异常")
    void getCommandService_null_throws() {
        DeviceAgreementService service = new DeviceAgreementService(List.of(gbService));

        assertThrows(RuntimeException.class, () -> service.getCommandService(null));
    }

    @Test
    @DisplayName("未注册协议（ONVIF）抛异常")
    void getCommandService_unregisteredProtocol_throws() {
        DeviceAgreementService service = new DeviceAgreementService(List.of(gbService));

        // ONVIF_IPC 映射到 ONVIF 纯协议，无对应实现
        assertThrows(RuntimeException.class,
            () -> service.getCommandService(DeviceAgreementEnum.ONVIF_IPC.getType()));
    }

    @Test
    @DisplayName("两个实现声明同一协议 → 构造期报错（防静默覆盖）")
    void construct_duplicateProtocol_throws() {
        DeviceCommandService anotherGb = mock(DeviceCommandService.class);
        when(anotherGb.supportProtocols())
            .thenReturn(Set.of(DeviceProtocolEnum.GB28181.getType()));

        assertThrows(IllegalStateException.class,
            () -> new DeviceAgreementService(List.of(gbService, anotherGb)));
    }

    @Test
    @DisplayName("新增空实现声明新协议即可被路由命中（本服务零改动）")
    void getCommandService_newProtocolAutoRegistered() {
        DeviceCommandService onvifService = mock(DeviceCommandService.class);
        lenient().when(onvifService.supportProtocols())
            .thenReturn(Set.of(DeviceProtocolEnum.ONVIF.getType()));

        DeviceAgreementService service = new DeviceAgreementService(List.of(gbService, onvifService));

        // ONVIF_IPC（agreement type=3）映射到 ONVIF 纯协议，命中新注册实现
        assertSame(onvifService, service.getCommandService(DeviceAgreementEnum.ONVIF_IPC.getType()));
        // GB28181 仍命中原实现
        assertSame(gbService, service.getCommandService(DeviceAgreementEnum.GB28181_IPC.getType()));
    }
}
