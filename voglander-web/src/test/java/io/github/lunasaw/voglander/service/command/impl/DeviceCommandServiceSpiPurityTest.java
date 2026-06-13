package io.github.lunasaw.voglander.service.command.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.client.service.device.DeviceCommandService;
import io.github.lunasaw.voglander.client.service.device.Gb28181DeviceCommandService;

/**
 * 架构红线守卫（S2，ARCHITECTURE.md 1.0.7 §8.1）：
 * 协议无关 SPI {@link DeviceCommandService} 不得被 GB 专属方法污染。
 *
 * @author luna
 */
@DisplayName("SPI 协议无关性守卫 (S2)")
class DeviceCommandServiceSpiPurityTest {

    /**
     * 协议无关 SPI 应恰好保留这 11 个方法，GB 专属动作一律落子接口。
     */
    private static final Set<String> EXPECTED_SPI_METHODS = Set.of(
        "supportProtocols", "queryChannel", "queryDevice", "queryDeviceInfo", "queryCatalog",
        "ptzControl", "startPlay", "startPlayback", "stopPlay", "reboot", "controlPlayback");

    @Test
    @DisplayName("DeviceCommandService 仍只含 11 个协议无关方法（未被 GB 专属方法污染）")
    void spi_not_polluted_by_gb_methods() {
        Set<String> actual = Arrays.stream(DeviceCommandService.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());
        assertEquals(EXPECTED_SPI_METHODS, actual,
            "协议无关 SPI 方法集发生变化——GB 专属方法必须落 Gb28181DeviceCommandService 子接口，禁止上提 SPI");
    }

    @Test
    @DisplayName("GB 专属方法全部声明在 Gb28181DeviceCommandService 子接口")
    void gb_methods_on_subinterface() {
        Set<String> subMethods = Arrays.stream(Gb28181DeviceCommandService.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());
        assertTrue(subMethods.containsAll(Set.of(
            "queryDeviceStatus", "queryPreset", "queryMobilePosition", "downloadConfig", "setDeviceConfig",
            "controlRecord", "queryRecord", "queryAlarm", "controlAlarm", "broadcast")));
        assertTrue(DeviceCommandService.class.isAssignableFrom(Gb28181DeviceCommandService.class),
            "Gb28181DeviceCommandService 应继承协议无关 SPI，保证 DeviceAgreementService 仍能路由");
    }
}
