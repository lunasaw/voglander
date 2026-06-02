package io.github.lunasaw.voglander.dependency;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * sip-gateway 1.8.0 依赖可用性自检测试
 *
 * <p>
 * 此测试不启动 Spring 容器，纯类加载验证。
 * 验证目的：
 * <ul>
 * <li>{@code sip-gateway-spring-boot-starter} 能否传递关键 API（envelope/notifier/store）</li>
 * <li>{@code gb28181-server} 能否经 starter 传递（不显式声明）</li>
 * <li>{@code gb28181-common} 能否经 gb28181-server 二级传递</li>
 * <li>{@code gb28181-client} 能否经 voglander-integration 显式声明传递</li>
 * </ul>
 * </p>
 *
 * <p>
 * 任何一项失败都说明依赖管理出错（pom 缺声明或版本冲突），需立即修复。
 * </p>
 *
 * @author luna
 * @since 2026/06/01
 */
@DisplayName("sip-gateway 1.8.0 依赖可用性")
public class SipGatewayDependencyAvailabilityTest {

    @Test
    @DisplayName("gateway-core envelope/notifier 类可加载")
    public void gatewayCoreClassesAvailable() {
        assertDoesNotThrow(() -> Class.forName("io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommand"));
        assertDoesNotThrow(() -> Class.forName("io.github.lunasaw.sipgateway.core.api.envelope.GatewayEvent"));
        assertDoesNotThrow(() -> Class.forName("io.github.lunasaw.sipgateway.core.api.BusinessNotifier"));
        assertDoesNotThrow(() -> Class.forName("io.github.lunasaw.sipgateway.core.core.CommandHandlerRegistry"));
        assertDoesNotThrow(() -> Class.forName("io.github.lunasaw.sipgateway.core.api.CommandMapping"));
    }

    @Test
    @DisplayName("gateway-gb28181 invite store 类可加载")
    public void gatewayGb28181ClassesAvailable() {
        assertDoesNotThrow(() -> Class.forName("io.github.lunasaw.sipgateway.gb28181.store.InviteContextStore"));
        assertDoesNotThrow(() -> Class.forName("io.github.lunasaw.sipgateway.gb28181.store.InviteContext"));
    }

    @Test
    @DisplayName("gb28181-server 经 starter 传递可加载")
    public void gb28181ServerTransitivelyAvailable() {
        assertDoesNotThrow(() -> Class.forName("io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender"));
        assertDoesNotThrow(() -> Class.forName("io.github.lunasaw.gbproxy.server.config.EnableSipServer"));
    }

    @Test
    @DisplayName("gb28181-common 经 gb28181-server 二级传递可加载")
    public void gb28181CommonTransitivelyAvailable() {
        assertDoesNotThrow(() -> Class.forName("io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify"));
        assertDoesNotThrow(() -> Class.forName("io.github.lunasaw.gb28181.common.entity.response.DeviceItem"));
        Class<?> streamModeEnum =
                assertDoesNotThrow(() -> Class.forName("io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum"));
        assertNotNull(streamModeEnum);
    }

    @Test
    @DisplayName("gb28181-client 由 voglander-integration 显式声明可加载")
    public void gb28181ClientExplicitlyAvailable() {
        assertDoesNotThrow(() -> Class.forName("io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender"));
    }

    @Test
    @DisplayName("sip-common SipLayer 经传递可加载")
    public void sipCommonAvailable() {
        assertDoesNotThrow(() -> Class.forName("io.github.lunasaw.sip.common.layer.SipLayer"));
    }
}
