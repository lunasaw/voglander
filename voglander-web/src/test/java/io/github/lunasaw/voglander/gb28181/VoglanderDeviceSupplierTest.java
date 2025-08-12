package io.github.lunasaw.voglander.gb28181;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.supplier.VoglanderServerDeviceSupplier;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试VoglanderServerDeviceSupplier的hostAddress设置
 * 注意: testDefaultToDeviceHasHostAddress 测试被暂时跳过，因为它依赖SIP基础设施初始化
 * 在mvn test环境下，SipLayer.getUdpSipProvider()可能未正确初始化
 * 
 * @author luna
 * @since 2025/8/10
 */
@Slf4j
public class VoglanderDeviceSupplierTest extends BaseTest {

    @Autowired
    private VoglanderServerDeviceSupplier deviceSupplier;

    @Test
    public void testServerFromDeviceHasHostAddress() {
        // Given & When
        FromDevice fromDevice = deviceSupplier.getServerFromDevice();

        // Then
        assertNotNull(fromDevice, "FromDevice不能为null");
        assertNotNull(fromDevice.getHostAddress(), "hostAddress不能为null");
        assertNotNull(fromDevice.getFromTag(), "fromTag不能为null");
        assertNotNull(fromDevice.getAgent(), "agent不能为null");

        log.info("FromDevice验证通过: userId={}, hostAddress={}, fromTag={}, agent={}",
            fromDevice.getUserId(), fromDevice.getHostAddress(),
            fromDevice.getFromTag(), fromDevice.getAgent());
    }

    @Test
    public void testConvertToSipDeviceHasHostAddress() {
        // Given
        DeviceDTO deviceDTO = new DeviceDTO();
        deviceDTO.setDeviceId("34020000001320000001");
        deviceDTO.setIp("192.168.1.100");
        deviceDTO.setPort(5060);

        // When
        ToDevice toDevice = (ToDevice)deviceSupplier.convertToSipDevice(deviceDTO);

        // Then
        assertNotNull(toDevice, "ToDevice不能为null");
        assertNotNull(toDevice.getHostAddress(), "hostAddress不能为null");
        assertEquals("192.168.1.100:5060", toDevice.getHostAddress(), "hostAddress格式应为ip:port");
        assertEquals("34020000001320000001", toDevice.getUserId(), "userId应正确设置");

        log.info("ToDevice验证通过: userId={}, hostAddress={}, ip={}, port={}",
            toDevice.getUserId(), toDevice.getHostAddress(), toDevice.getIp(), toDevice.getPort());
    }

    @Test
    public void testConvertToSipDeviceWithNullPort() {
        // Given
        DeviceDTO deviceDTO = new DeviceDTO();
        deviceDTO.setDeviceId("34020000001320000002");
        deviceDTO.setIp("192.168.1.101");
        deviceDTO.setPort(null);

        // When
        ToDevice toDevice = (ToDevice)deviceSupplier.convertToSipDevice(deviceDTO);

        // Then
        assertNotNull(toDevice, "ToDevice不能为null");
        assertNotNull(toDevice.getHostAddress(), "hostAddress不能为null");
        assertEquals("192.168.1.101:5060", toDevice.getHostAddress(), "当port为null时，应使用默认端口5060");

        log.info("ToDevice with null port验证通过: hostAddress={}", toDevice.getHostAddress());
    }

    @Test
    @Disabled("依赖SIP基础设施初始化，需要先启动SIP监听器才能正常运行。该方法调用SipRequestUtils.getNewCallId()需要活跃的SIP监听点")
    public void testDefaultToDeviceHasHostAddress() {
        // Given
        String deviceId = "34020000001320000003";

        // When - 通过getToDevice方法间接调用createDefaultToDevice
        ToDevice toDevice = deviceSupplier.getToDevice(deviceId);

        // Then
        assertNotNull(toDevice, "ToDevice不能为null");
        assertNotNull(toDevice.getHostAddress(), "hostAddress不能为null");
        assertTrue(toDevice.getHostAddress().contains(":"), "hostAddress应包含端口分隔符");
        assertNotNull(toDevice.getLocalIp(), "localIp应设置");
        assertEquals(3600, toDevice.getExpires(), "expires应设置为3600");

        log.info("默认ToDevice验证通过: userId={}, hostAddress={}, localIp={}, expires={}",
            toDevice.getUserId(), toDevice.getHostAddress(), toDevice.getLocalIp(), toDevice.getExpires());
    }
}