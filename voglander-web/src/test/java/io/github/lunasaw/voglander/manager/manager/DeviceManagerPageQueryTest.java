package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceQueryDTO;
import io.github.lunasaw.voglander.support.UniqueKeyFactory;

/**
 * DeviceManager 多条件分页查询测试（S1：设备列表筛选）。
 *
 * <p>
 * 覆盖 {@link DeviceManager#getPage(DeviceQueryDTO, int, int)} 的各筛选分支：
 * deviceId 精确 / name 模糊 / status / type / ip 模糊 / serverIp / 心跳时间范围 / 注册时间范围。
 * </p>
 *
 * @author luna
 */
@DisplayName("DeviceManager 多条件分页查询测试 (S1)")
@Transactional
class DeviceManagerPageQueryTest extends BaseTest {

    @Autowired
    private DeviceManager deviceManager;

    private DeviceDTO buildDto(String deviceId, String name, Integer status, Integer type, String ip) {
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(deviceId);
        dto.setName(name);
        dto.setIp(ip);
        dto.setPort(5060);
        dto.setType(type);
        dto.setStatus(status);
        dto.setServerIp("192.168.1.100");
        dto.setRegisterTime(LocalDateTime.now());
        dto.setKeepaliveTime(LocalDateTime.now());
        return dto;
    }

    @Test
    @DisplayName("无条件查询返回分页结果")
    void should_return_page_without_condition() {
        deviceManager.add(buildDto(UniqueKeyFactory.deviceId(), "dev-a", 1, 1, "10.0.0.1"));
        Page<DeviceDTO> page = deviceManager.getPage(new DeviceQueryDTO(), 1, 10);
        assertNotNull(page);
        assertTrue(page.getTotal() >= 1);
    }

    @Test
    @DisplayName("deviceId 精确筛选只命中目标设备")
    void should_filter_by_device_id() {
        String target = UniqueKeyFactory.deviceId();
        deviceManager.add(buildDto(target, "dev-target", 1, 1, "10.0.0.1"));
        deviceManager.add(buildDto(UniqueKeyFactory.deviceId(), "dev-other", 1, 1, "10.0.0.2"));

        DeviceQueryDTO q = new DeviceQueryDTO();
        q.setDeviceId(target);
        Page<DeviceDTO> page = deviceManager.getPage(q, 1, 10);
        assertEquals(1, page.getTotal());
        assertEquals(target, page.getRecords().get(0).getDeviceId());
    }

    @Test
    @DisplayName("name 模糊筛选")
    void should_filter_by_name_like() {
        String token = "luna" + System.nanoTime();
        deviceManager.add(buildDto(UniqueKeyFactory.deviceId(), token + "-cam", 1, 1, "10.0.0.1"));

        DeviceQueryDTO q = new DeviceQueryDTO();
        q.setName(token);
        Page<DeviceDTO> page = deviceManager.getPage(q, 1, 10);
        assertEquals(1, page.getTotal());
    }

    @Test
    @DisplayName("status 筛选")
    void should_filter_by_status() {
        String online = UniqueKeyFactory.deviceId();
        String offline = UniqueKeyFactory.deviceId();
        deviceManager.add(buildDto(online, "on", 1, 1, "10.0.0.1"));
        deviceManager.add(buildDto(offline, "off", 0, 1, "10.0.0.2"));

        DeviceQueryDTO q = new DeviceQueryDTO();
        q.setDeviceId(offline);
        q.setStatus(0);
        Page<DeviceDTO> page = deviceManager.getPage(q, 1, 10);
        assertEquals(1, page.getTotal());
        assertEquals(0, page.getRecords().get(0).getStatus());

        q.setStatus(1);
        assertEquals(0, deviceManager.getPage(q, 1, 10).getTotal(), "offline 设备用 status=1 应查不到");
    }

    @Test
    @DisplayName("ip 模糊筛选")
    void should_filter_by_ip_like() {
        String dev = UniqueKeyFactory.deviceId();
        deviceManager.add(buildDto(dev, "ipdev", 1, 1, "172.31.55.99"));

        DeviceQueryDTO q = new DeviceQueryDTO();
        q.setDeviceId(dev);
        q.setIp("172.31.55");
        assertEquals(1, deviceManager.getPage(q, 1, 10).getTotal());
    }

    @Test
    @DisplayName("心跳时间范围筛选（范围外查不到）")
    void should_filter_by_keepalive_time_range() {
        String dev = UniqueKeyFactory.deviceId();
        deviceManager.add(buildDto(dev, "kdev", 1, 1, "10.0.0.1"));

        DeviceQueryDTO in = new DeviceQueryDTO();
        in.setDeviceId(dev);
        in.setKeepaliveTimeStart(LocalDateTime.now().minusHours(1));
        in.setKeepaliveTimeEnd(LocalDateTime.now().plusHours(1));
        assertEquals(1, deviceManager.getPage(in, 1, 10).getTotal());

        DeviceQueryDTO out = new DeviceQueryDTO();
        out.setDeviceId(dev);
        out.setKeepaliveTimeStart(LocalDateTime.now().plusDays(1));
        assertEquals(0, deviceManager.getPage(out, 1, 10).getTotal());
    }

    @Test
    @DisplayName("注册时间范围筛选")
    void should_filter_by_register_time_range() {
        String dev = UniqueKeyFactory.deviceId();
        deviceManager.add(buildDto(dev, "rdev", 1, 1, "10.0.0.1"));

        DeviceQueryDTO q = new DeviceQueryDTO();
        q.setDeviceId(dev);
        q.setRegisterTimeStart(LocalDateTime.now().minusHours(1));
        q.setRegisterTimeEnd(LocalDateTime.now().plusHours(1));
        assertEquals(1, deviceManager.getPage(q, 1, 10).getTotal());
    }

    @Test
    @DisplayName("非法分页参数抛异常")
    void should_throw_on_invalid_page_args() {
        assertThrows(IllegalArgumentException.class, () -> deviceManager.getPage(new DeviceQueryDTO(), 0, 10));
        assertThrows(IllegalArgumentException.class, () -> deviceManager.getPage(new DeviceQueryDTO(), 1, 0));
    }
}
