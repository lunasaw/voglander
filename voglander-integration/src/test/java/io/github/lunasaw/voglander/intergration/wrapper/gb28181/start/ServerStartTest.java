package io.github.lunasaw.voglander.intergration.wrapper.gb28181.start;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.luna.common.os.SystemInfoUtil;

import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.layer.SipLayer;
import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.manager.manager.DeviceConfigManager;

/**
 * 服务器启动测试类
 * 
 * @author luna
 * @since 2025/8/2
 */
@ExtendWith(MockitoExtension.class)
class ServerStartTest {

    @Mock
    private SipServerConfig      sipServerConfig;

    @Mock
    private DeviceConfigManager  deviceConfigManager;

    @Mock
    private SipLayer             sipLayer;

    @InjectMocks
    private ServerStart          serverStart;

    private static final String  TEST_IP       = "192.168.1.100";
    private static final String  TEST_SIP      = "34020000002000000001";
    private static final String  TEST_PASSWORD = "12345678";
    private static final Integer TEST_PORT     = 5060;

    @BeforeEach
    void setUp() {
        when(sipServerConfig.getPort()).thenReturn(TEST_PORT);
        when(sipServerConfig.getEnableLog()).thenReturn(true);
    }

    @Test
    void run_WithConfiguredIp_Success() throws Exception {
        // Given
        when(sipServerConfig.getIp()).thenReturn(TEST_IP);
        when(deviceConfigManager.getSystemValueWithDefault(
            DeviceConstant.LocalConfig.DEVICE_GB_SIP,
            DeviceConstant.LocalConfig.DEVICE_GB_SIP_DEFAULT))
            .thenReturn(TEST_SIP);
        when(deviceConfigManager.getSystemValueWithDefault(
            DeviceConstant.LocalConfig.DEVICE_GB_PASSWORD,
            DeviceConstant.LocalConfig.DEVICE_GB_PASSWORD_DEFAULT))
            .thenReturn(TEST_PASSWORD);

        // When
        serverStart.run();

        // Then
        verify(sipLayer).addListeningPoint(TEST_IP, TEST_PORT, true);
        verify(deviceConfigManager).getSystemValueWithDefault(
            DeviceConstant.LocalConfig.DEVICE_GB_SIP,
            DeviceConstant.LocalConfig.DEVICE_GB_SIP_DEFAULT);
        verify(deviceConfigManager).getSystemValueWithDefault(
            DeviceConstant.LocalConfig.DEVICE_GB_PASSWORD,
            DeviceConstant.LocalConfig.DEVICE_GB_PASSWORD_DEFAULT);

        // Verify device map contains server_from
        Map<String, Device> deviceMap = ServerStart.DEVICE_MAP;
        assertTrue(deviceMap.containsKey("server_from"));
        Device serverFrom = deviceMap.get("server_from");
        assertNotNull(serverFrom);
        assertTrue(serverFrom instanceof FromDevice);
    }

    @Test
    void run_WithBlankIp_UsesSystemIp() throws Exception {
        // Given
        String systemIp = "10.0.0.1";
        when(sipServerConfig.getIp()).thenReturn(null);
        when(deviceConfigManager.getSystemValueWithDefault(anyString(), anyString()))
            .thenReturn(TEST_SIP).thenReturn(TEST_PASSWORD);

        try (MockedStatic<SystemInfoUtil> mockedSystemInfo = mockStatic(SystemInfoUtil.class)) {
            mockedSystemInfo.when(SystemInfoUtil::getIpv4).thenReturn(systemIp);

            // When
            serverStart.run();

            // Then
            verify(sipLayer).addListeningPoint(systemIp, TEST_PORT, true);
            mockedSystemInfo.verify(SystemInfoUtil::getIpv4);
        }
    }

    @Test
    void run_WithEmptyIp_UsesSystemIp() throws Exception {
        // Given
        String systemIp = "172.16.0.1";
        when(sipServerConfig.getIp()).thenReturn("");
        when(deviceConfigManager.getSystemValueWithDefault(anyString(), anyString()))
            .thenReturn(TEST_SIP).thenReturn(TEST_PASSWORD);

        try (MockedStatic<SystemInfoUtil> mockedSystemInfo = mockStatic(SystemInfoUtil.class)) {
            mockedSystemInfo.when(SystemInfoUtil::getIpv4).thenReturn(systemIp);

            // When
            serverStart.run();

            // Then
            verify(sipLayer).addListeningPoint(systemIp, TEST_PORT, true);
        }
    }

    @Test
    void run_WithWhitespaceIp_UsesSystemIp() throws Exception {
        // Given
        String systemIp = "192.168.0.1";
        when(sipServerConfig.getIp()).thenReturn("   ");
        when(deviceConfigManager.getSystemValueWithDefault(anyString(), anyString()))
            .thenReturn(TEST_SIP).thenReturn(TEST_PASSWORD);

        try (MockedStatic<SystemInfoUtil> mockedSystemInfo = mockStatic(SystemInfoUtil.class)) {
            mockedSystemInfo.when(SystemInfoUtil::getIpv4).thenReturn(systemIp);

            // When
            serverStart.run();

            // Then
            verify(sipLayer).addListeningPoint(systemIp, TEST_PORT, true);
        }
    }

    @Test
    void run_EnableLogFalse_PassesFalseToSipLayer() throws Exception {
        // Given
        when(sipServerConfig.getIp()).thenReturn(TEST_IP);
        when(sipServerConfig.getEnableLog()).thenReturn(false);
        when(deviceConfigManager.getSystemValueWithDefault(anyString(), anyString()))
            .thenReturn(TEST_SIP).thenReturn(TEST_PASSWORD);

        // When
        serverStart.run();

        // Then
        verify(sipLayer).addListeningPoint(TEST_IP, TEST_PORT, false);
    }

    @Test
    void run_CreatesCorrectFromDevice() throws Exception {
        // Given
        when(sipServerConfig.getIp()).thenReturn(TEST_IP);
        when(deviceConfigManager.getSystemValueWithDefault(
            DeviceConstant.LocalConfig.DEVICE_GB_SIP,
            DeviceConstant.LocalConfig.DEVICE_GB_SIP_DEFAULT))
            .thenReturn(TEST_SIP);
        when(deviceConfigManager.getSystemValueWithDefault(
            DeviceConstant.LocalConfig.DEVICE_GB_PASSWORD,
            DeviceConstant.LocalConfig.DEVICE_GB_PASSWORD_DEFAULT))
            .thenReturn(TEST_PASSWORD);

        // When
        serverStart.run();

        // Then
        Map<String, Device> deviceMap = ServerStart.DEVICE_MAP;
        FromDevice serverFrom = (FromDevice)deviceMap.get("server_from");

        assertNotNull(serverFrom);
        assertEquals(TEST_PASSWORD, serverFrom.getPassword());
        assertEquals(TEST_SIP.substring(0, 9), serverFrom.getRealm());
    }

    @Test
    void run_WithDifferentPort_PassesCorrectPort() throws Exception {
        // Given
        Integer customPort = 5061;
        when(sipServerConfig.getIp()).thenReturn(TEST_IP);
        when(sipServerConfig.getPort()).thenReturn(customPort);
        when(deviceConfigManager.getSystemValueWithDefault(anyString(), anyString()))
            .thenReturn(TEST_SIP).thenReturn(TEST_PASSWORD);

        // When
        serverStart.run();

        // Then
        verify(sipLayer).addListeningPoint(TEST_IP, customPort, true);
    }

    @Test
    void run_ReplacesExistingDeviceInMap() throws Exception {
        // Given
        // First populate the device map
        when(sipServerConfig.getIp()).thenReturn(TEST_IP);
        when(deviceConfigManager.getSystemValueWithDefault(anyString(), anyString()))
            .thenReturn(TEST_SIP).thenReturn(TEST_PASSWORD);

        serverStart.run();
        Device firstDevice = ServerStart.DEVICE_MAP.get("server_from");

        // When - run again with different config
        String newSip = "34020000002000000002";
        when(deviceConfigManager.getSystemValueWithDefault(
            DeviceConstant.LocalConfig.DEVICE_GB_SIP,
            DeviceConstant.LocalConfig.DEVICE_GB_SIP_DEFAULT))
            .thenReturn(newSip);

        serverStart.run();

        // Then
        Device secondDevice = ServerStart.DEVICE_MAP.get("server_from");
        assertNotEquals(firstDevice, secondDevice);
        assertEquals(1, ServerStart.DEVICE_MAP.size());
    }

    @Test
    void run_WithNullPort_HandledGracefully() throws Exception {
        // Given
        when(sipServerConfig.getIp()).thenReturn(TEST_IP);
        when(sipServerConfig.getPort()).thenReturn(null);
        when(deviceConfigManager.getSystemValueWithDefault(anyString(), anyString()))
            .thenReturn(TEST_SIP).thenReturn(TEST_PASSWORD);

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> serverStart.run());
        verify(sipLayer).addListeningPoint(TEST_IP, 12001, true);
    }

    @Test
    void run_WithArgumentsArray_IgnoresArguments() throws Exception {
        // Given
        String[] args = {"arg1", "arg2", "arg3"};
        when(sipServerConfig.getIp()).thenReturn(TEST_IP);
        when(deviceConfigManager.getSystemValueWithDefault(anyString(), anyString()))
            .thenReturn(TEST_SIP).thenReturn(TEST_PASSWORD);

        // When
        serverStart.run(args);

        // Then - should work normally regardless of arguments
        verify(sipLayer).addListeningPoint(TEST_IP, TEST_PORT, true);
        assertNotNull(ServerStart.DEVICE_MAP.get("server_from"));
    }

    @Test
    void run_SipLayerThrowsException_PropagatesException() throws Exception {
        // Given
        when(sipServerConfig.getIp()).thenReturn(TEST_IP);
        when(deviceConfigManager.getSystemValueWithDefault(anyString(), anyString()))
            .thenReturn(TEST_SIP).thenReturn(TEST_PASSWORD);

        RuntimeException expectedException = new RuntimeException("SipLayer initialization failed");
        doThrow(expectedException).when(sipLayer).addListeningPoint(anyString(), anyInt(), anyBoolean());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> serverStart.run());
        assertEquals("SipLayer initialization failed", exception.getMessage());
    }

    @Test
    void run_DeviceConfigManagerThrowsException_PropagatesException() throws Exception {
        // Given
        when(sipServerConfig.getIp()).thenReturn(TEST_IP);
        RuntimeException expectedException = new RuntimeException("DeviceConfig access failed");
        when(deviceConfigManager.getSystemValueWithDefault(anyString(), anyString()))
            .thenThrow(expectedException);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> serverStart.run());
        assertEquals("DeviceConfig access failed", exception.getMessage());
    }

    @Test
    void deviceMap_IsStaticAndShared() {
        // Given
        Map<String, Device> map1 = ServerStart.DEVICE_MAP;
        Map<String, Device> map2 = ServerStart.DEVICE_MAP;

        // Then
        assertSame(map1, map2);
        assertTrue(map1 instanceof java.util.concurrent.ConcurrentHashMap);
    }
}