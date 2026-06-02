package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.support.UniqueKeyFactory;

/**
 * DeviceManager 标准模板方法回归测试
 *
 * @author luna
 */
@DisplayName("DeviceManager 模板方法测试")
@Transactional
class DeviceManagerTemplateTest extends BaseTest {

    @Autowired
    private DeviceManager deviceManager;

    private DeviceDTO buildDto(String deviceId) {
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(deviceId);
        dto.setIp("127.0.0.1");
        dto.setPort(5060);
        dto.setType(1);
        dto.setServerIp("127.0.0.1");
        dto.setName("test-device");
        return dto;
    }

    @Nested
    @DisplayName("add")
    class Add {

        @Test
        @DisplayName("add 应落库并返回ID")
        void should_save_and_return_id() {
            Long id = deviceManager.add(buildDto(UniqueKeyFactory.deviceId()));
            assertNotNull(id);
            assertTrue(id > 0);
        }

        @Test
        @DisplayName("deviceId 重复 add 应抛异常")
        void should_throw_on_duplicate_device_id() {
            String deviceId = UniqueKeyFactory.deviceId();
            deviceManager.add(buildDto(deviceId));
            assertThrows(Exception.class, () -> deviceManager.add(buildDto(deviceId)));
        }

        @Test
        @DisplayName("缺少必填字段 add 应抛异常")
        void should_throw_when_required_field_missing() {
            DeviceDTO dto = new DeviceDTO();
            dto.setDeviceId(UniqueKeyFactory.deviceId());
            // 缺少 ip/port/type
            assertThrows(Exception.class, () -> deviceManager.add(dto));
        }
    }

    @Nested
    @DisplayName("updateById")
    class Update {

        @Test
        @DisplayName("updateById 应只更新指定字段，未传字段保持原值")
        void should_update_only_specified_fields() {
            String deviceId = UniqueKeyFactory.deviceId();
            Long id = deviceManager.add(buildDto(deviceId));

            DeviceDTO update = new DeviceDTO();
            update.setName("updated-name");
            deviceManager.updateById(id, update);

            DeviceDTO query = new DeviceDTO();
            query.setDeviceId(deviceId);
            DeviceDTO result = deviceManager.get(query);
            assertEquals("updated-name", result.getName());
            assertEquals("127.0.0.1", result.getIp(), "ip 不应被修改");
        }
    }

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        @DisplayName("get 按 deviceId 应返回对应记录")
        void should_get_by_device_id() {
            String deviceId = UniqueKeyFactory.deviceId();
            Long id = deviceManager.add(buildDto(deviceId));

            DeviceDTO query = new DeviceDTO();
            query.setDeviceId(deviceId);
            DeviceDTO result = deviceManager.get(query);
            assertNotNull(result);
            assertEquals(id, result.getId());
        }

        @Test
        @DisplayName("get 不存在的 deviceId 应返回 null")
        void should_return_null_when_not_found() {
            DeviceDTO query = new DeviceDTO();
            query.setDeviceId("nonexistent-" + UniqueKeyFactory.deviceId());
            assertNull(deviceManager.get(query));
        }
    }

    @Nested
    @DisplayName("deleteOne")
    class Delete {

        @Test
        @DisplayName("deleteOne 应删除记录")
        void should_delete_record() {
            String deviceId = UniqueKeyFactory.deviceId();
            deviceManager.add(buildDto(deviceId));

            DeviceDTO deleteDto = new DeviceDTO();
            deleteDto.setDeviceId(deviceId);
            deviceManager.deleteOne(deleteDto);

            assertNull(deviceManager.get(deleteDto));
        }
    }

    @Nested
    @DisplayName("getPage")
    class GetPage {

        @Test
        @DisplayName("getPage 应按 createTime 降序，最新在首位")
        void should_order_by_create_time_desc() throws InterruptedException {
            deviceManager.add(buildDto(UniqueKeyFactory.deviceId()));
            Thread.sleep(5);
            String latestId = UniqueKeyFactory.deviceId();
            deviceManager.add(buildDto(latestId));

            DeviceDTO query = new DeviceDTO();
            Page<DeviceDTO> page = deviceManager.getPage(query, 1, 10);
            assertFalse(page.getRecords().isEmpty());
            // 最新插入应在前
            assertEquals(latestId, page.getRecords().get(0).getDeviceId());
        }
    }
}
