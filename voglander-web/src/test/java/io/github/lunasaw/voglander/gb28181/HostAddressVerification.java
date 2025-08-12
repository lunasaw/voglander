package io.github.lunasaw.voglander.gb28181;

import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipServerProperties;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.supplier.VoglanderServerDeviceSupplier;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;

/**
 * 简单验证VoglanderServerDeviceSupplier的hostAddress设置
 * 
 * @author luna
 * @since 2025/8/10
 */
public class HostAddressVerification {

    public static void main(String[] args) {
        try {
            // 创建测试用的配置
            VoglanderSipServerProperties properties = new VoglanderSipServerProperties();
            properties.setServerId("34020000002000000001");
            properties.setIp("127.0.0.1");
            properties.setPort(5060);
            properties.setDomain("34020000002000000001");

            // 创建供应器实例
            VoglanderServerDeviceSupplier supplier = new VoglanderServerDeviceSupplier();

            // 使用反射设置私有字段
            java.lang.reflect.Field serverPropertiesField =
                VoglanderServerDeviceSupplier.class.getDeclaredField("serverProperties");
            serverPropertiesField.setAccessible(true);
            serverPropertiesField.set(supplier, properties);

            // 测试1: FromDevice是否正确设置hostAddress
            System.out.println("=== 测试1: FromDevice hostAddress设置 ===");
            FromDevice fromDevice = supplier.getServerFromDevice();
            System.out.println("FromDevice userId: " + fromDevice.getUserId());
            System.out.println("FromDevice hostAddress: " + fromDevice.getHostAddress());
            System.out.println("FromDevice fromTag: " + fromDevice.getFromTag());
            System.out.println("FromDevice agent: " + fromDevice.getAgent());
            System.out.println("FromDevice hostAddress是否为null: " + (fromDevice.getHostAddress() == null));
            System.out.println();

            // 测试2: convertToSipDevice是否正确设置hostAddress
            System.out.println("=== 测试2: convertToSipDevice hostAddress设置 ===");
            DeviceDTO deviceDTO = new DeviceDTO();
            deviceDTO.setDeviceId("34020000001320000001");
            deviceDTO.setIp("192.168.1.100");
            deviceDTO.setPort(5060);

            Device device = supplier.convertToSipDevice(deviceDTO);
            System.out.println("Device userId: " + device.getUserId());
            System.out.println("Device hostAddress: " + device.getHostAddress());
            System.out.println("Device ip: " + device.getIp());
            System.out.println("Device port: " + device.getPort());
            System.out.println("Device hostAddress是否为null: " + (device.getHostAddress() == null));
            System.out.println();

            // 测试3: ToDevice通过getToDevice方法
            System.out.println("=== 测试3: getToDevice方法的ToDevice设置 ===");
            ToDevice toDevice = supplier.getToDevice(device);
            if (toDevice != null) {
                System.out.println("ToDevice userId: " + toDevice.getUserId());
                System.out.println("ToDevice hostAddress: " + toDevice.getHostAddress());
                System.out.println("ToDevice localIp: " + toDevice.getLocalIp());
                System.out.println("ToDevice expires: " + toDevice.getExpires());
                System.out.println("ToDevice hostAddress是否为null: " + (toDevice.getHostAddress() == null));
            } else {
                System.out.println("ToDevice为null");
            }

            System.out.println("\n=== 验证完成 ===");

        } catch (Exception e) {
            System.err.println("验证过程中出现异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}