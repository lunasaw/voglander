package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.domaon.dto.AlarmDTO;
import io.github.lunasaw.voglander.support.UniqueKeyFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * AlarmManager 集成测试（BaseTest，真实库事务）。
 * <p>
 * 覆盖：新增、按 ID 查询、分页（含时间区间）、确认（ack）、删除。
 * </p>
 *
 * @author luna
 */
@Slf4j
public class AlarmManagerTest extends BaseTest {

    @Autowired
    private AlarmManager alarmManager;

    private AlarmDTO newAlarm(String deviceId) {
        AlarmDTO dto = new AlarmDTO();
        dto.setDeviceId(deviceId);
        dto.setChannelId("ch-1");
        dto.setAlarmType(1);
        dto.setAlarmLevel(3);
        dto.setAlarmTime(LocalDateTime.now());
        dto.setDescription("移动侦测告警");
        return dto;
    }

    @Test
    public void testAdd_DefaultsAckStatusZero() {
        String deviceId = UniqueKeyFactory.deviceId();
        Long id = alarmManager.add(newAlarm(deviceId));

        assertNotNull(id);
        AlarmDTO saved = alarmManager.getById(id);
        assertNotNull(saved);
        assertEquals(deviceId, saved.getDeviceId());
        assertEquals(0, saved.getAckStatus(), "ack_status 默认 0");
        assertEquals(3, saved.getAlarmLevel());
    }

    @Test
    public void testAdd_Validation() {
        assertThrows(IllegalArgumentException.class, () -> alarmManager.add(null));
        AlarmDTO noDevice = new AlarmDTO();
        assertThrows(IllegalArgumentException.class, () -> alarmManager.add(noDevice));
    }

    @Test
    public void testAck_SetsAckStatusOne() {
        String deviceId = UniqueKeyFactory.deviceId();
        Long id = alarmManager.add(newAlarm(deviceId));

        Boolean acked = alarmManager.ack(id);

        assertTrue(acked);
        AlarmDTO after = alarmManager.getById(id);
        assertEquals(1, after.getAckStatus(), "确认后 ack_status=1");
    }

    @Test
    public void testGetPage_FiltersByDevice() {
        String deviceId = UniqueKeyFactory.deviceId();
        alarmManager.add(newAlarm(deviceId));
        alarmManager.add(newAlarm(deviceId));

        AlarmDTO query = new AlarmDTO();
        query.setDeviceId(deviceId);
        Page<AlarmDTO> page = alarmManager.getPage(query, 1, 10);

        assertEquals(2, page.getTotal());
        assertTrue(page.getRecords().stream().allMatch(a -> deviceId.equals(a.getDeviceId())));
    }

    @Test
    public void testGetPageWithTimeRange() {
        String deviceId = UniqueKeyFactory.deviceId();
        AlarmDTO old = newAlarm(deviceId);
        old.setAlarmTime(LocalDateTime.now().minusDays(2));
        alarmManager.add(old);
        AlarmDTO recent = newAlarm(deviceId);
        recent.setAlarmTime(LocalDateTime.now());
        alarmManager.add(recent);

        AlarmDTO query = new AlarmDTO();
        query.setDeviceId(deviceId);
        Page<AlarmDTO> page = alarmManager.getPageWithTimeRange(
            query, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1), 1, 10);

        assertEquals(1, page.getTotal(), "仅命中最近 1 小时内的告警");
    }

    @Test
    public void testDeleteOne() {
        String deviceId = UniqueKeyFactory.deviceId();
        Long id = alarmManager.add(newAlarm(deviceId));

        AlarmDTO del = new AlarmDTO();
        del.setId(id);
        assertTrue(alarmManager.deleteOne(del));
        assertNull(alarmManager.getById(id));
    }
}
