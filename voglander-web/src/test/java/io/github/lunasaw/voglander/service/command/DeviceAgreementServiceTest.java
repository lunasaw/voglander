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
import io.github.lunasaw.voglander.common.enums.DeviceProtocolEnum;

/**
 * S1+S4（PROTOCOL-ARCHITECTURE-GENERICITY）出站 {@code DeviceCommandService} SPI 化红线测试。
 * <p>
 * 验证 {@link DeviceAgreementService} 由「硬编码 if」改为「按协议自动注册的路由表」后：
 * <ul>
 * <li>S4：路由键统一为<strong>纯协议</strong>（{@link DeviceProtocolEnum} 的 type）；agreement→protocol
 * 折算由调用方完成，本服务只认纯协议；</li>
 * <li>按纯协议路由到声明了对应协议的 {@link DeviceCommandService} 实现；</li>
 * <li>未注册协议 / 入参为空 抛异常；</li>
 * <li>构造期两个实现声明同协议 → 启动即报错（防止静默覆盖）。</li>
 * </ul>
 * 新增协议只需新增一个声明 {@code supportProtocols()} 的实现，本服务零改动。
 *
 * @author luna
 */
@DisplayName("S1+S4 — 出站命令服务 SPI 路由（纯协议键）")
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
    @DisplayName("GB28181 纯协议路由到 GB28181 命令服务")
    void getCommandService_gb28181_routesToGbService() {
        DeviceAgreementService service = new DeviceAgreementService(List.of(gbService));

        DeviceCommandService resolved = service.getCommandService(DeviceProtocolEnum.GB28181.getType());

        assertNotNull(resolved);
        assertSame(gbService, resolved);
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

        assertThrows(RuntimeException.class,
            () -> service.getCommandService(DeviceProtocolEnum.ONVIF.getType()));
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

        // 新协议 ONVIF 命中新注册实现
        assertSame(onvifService, service.getCommandService(DeviceProtocolEnum.ONVIF.getType()));
        // GB28181 仍命中原实现
        assertSame(gbService, service.getCommandService(DeviceProtocolEnum.GB28181.getType()));
    }
}
