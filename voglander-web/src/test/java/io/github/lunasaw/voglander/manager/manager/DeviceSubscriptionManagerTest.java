package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.common.constant.device.SubscriptionConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceSubscriptionDTO;
import io.github.lunasaw.voglander.support.UniqueKeyFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link DeviceSubscriptionManager} 集成测试（BaseTest，真实库事务）。
 * <p>
 * 覆盖：upsertIntent UNIQUE 幂等、markActive/Pending/Failed/Inactive 运行态回填、
 * listEnabledByDevice、listExpiring 边界、touchLastNotify。
 * </p>
 *
 * @author luna
 */
@Slf4j
public class DeviceSubscriptionManagerTest extends BaseTest {

    @Autowired
    private DeviceSubscriptionManager subscriptionManager;

    @Test
    public void testUpsertIntent_InsertThenUpdateIsIdempotent() {
        String deviceId = UniqueKeyFactory.deviceId();

        Long id1 = subscriptionManager.upsertIntent(deviceId, SubscriptionConstant.Type.CATALOG, true);
        assertNotNull(id1);

        // 再次 upsert 同 device+type → 不新建行，返回同一 ID
        Long id2 = subscriptionManager.upsertIntent(deviceId, SubscriptionConstant.Type.CATALOG, false);
        assertEquals(id1, id2, "UNIQUE(device,type) 应复用同一行");

        DeviceSubscriptionDTO sub = subscriptionManager.getByDeviceAndType(deviceId, SubscriptionConstant.Type.CATALOG);
        assertNotNull(sub);
        assertEquals(0, sub.getEnabled(), "第二次 upsert 关闭意图");
        assertEquals(SubscriptionConstant.Status.INACTIVE, sub.getStatus(), "默认运行态 INACTIVE");
    }

    @Test
    public void testMarkActive_BackfillsCallIdAndExpireTime() {
        String deviceId = UniqueKeyFactory.deviceId();
        subscriptionManager.upsertIntent(deviceId, SubscriptionConstant.Type.ALARM, true);

        LocalDateTime before = LocalDateTime.now();
        subscriptionManager.markActive(deviceId, SubscriptionConstant.Type.ALARM, "call-123", 3600);

        DeviceSubscriptionDTO sub = subscriptionManager.getByDeviceAndType(deviceId, SubscriptionConstant.Type.ALARM);
        assertEquals(SubscriptionConstant.Status.ACTIVE, sub.getStatus());
        assertEquals("call-123", sub.getCallId());
        assertEquals(3600, sub.getExpires());
        assertNotNull(sub.getExpireTime());
        assertTrue(sub.getExpireTime().isAfter(before.plusSeconds(3000)), "expireTime ≈ now + expires");
    }

    @Test
    public void testMarkPendingFailedInactive() {
        String deviceId = UniqueKeyFactory.deviceId();
        subscriptionManager.upsertIntent(deviceId, SubscriptionConstant.Type.MOBILE_POSITION, true);

        subscriptionManager.markPending(deviceId, SubscriptionConstant.Type.MOBILE_POSITION);
        assertEquals(SubscriptionConstant.Status.PENDING,
            subscriptionManager.getByDeviceAndType(deviceId, SubscriptionConstant.Type.MOBILE_POSITION).getStatus());

        subscriptionManager.markFailed(deviceId, SubscriptionConstant.Type.MOBILE_POSITION);
        assertEquals(SubscriptionConstant.Status.FAILED,
            subscriptionManager.getByDeviceAndType(deviceId, SubscriptionConstant.Type.MOBILE_POSITION).getStatus());

        subscriptionManager.markActive(deviceId, SubscriptionConstant.Type.MOBILE_POSITION, "c1", 3600);
        subscriptionManager.markInactive(deviceId, SubscriptionConstant.Type.MOBILE_POSITION);
        DeviceSubscriptionDTO sub = subscriptionManager.getByDeviceAndType(deviceId, SubscriptionConstant.Type.MOBILE_POSITION);
        assertEquals(SubscriptionConstant.Status.INACTIVE, sub.getStatus());
        assertNull(sub.getCallId(), "markInactive 清 callId");
        assertNull(sub.getExpireTime(), "markInactive 清 expireTime");
    }

    @Test
    public void testListEnabledByDevice_OnlyReturnsEnabled() {
        String deviceId = UniqueKeyFactory.deviceId();
        subscriptionManager.upsertIntent(deviceId, SubscriptionConstant.Type.CATALOG, true);
        subscriptionManager.upsertIntent(deviceId, SubscriptionConstant.Type.ALARM, true);
        subscriptionManager.upsertIntent(deviceId, SubscriptionConstant.Type.MOBILE_POSITION, false);

        List<DeviceSubscriptionDTO> enabled = subscriptionManager.listEnabledByDevice(deviceId);
        assertEquals(2, enabled.size(), "只返回意图开启的两类");
        assertTrue(enabled.stream().allMatch(s -> s.getEnabled() == 1));
    }

    @Test
    public void testListExpiring_BoundaryByExpireTimeAndStatus() {
        String d1 = UniqueKeyFactory.deviceId();
        String d2 = UniqueKeyFactory.deviceId();
        // d1 CATALOG: ACTIVE 且即将过期（expires 短）
        subscriptionManager.upsertIntent(d1, SubscriptionConstant.Type.CATALOG, true);
        subscriptionManager.markActive(d1, SubscriptionConstant.Type.CATALOG, "c-d1", 1);
        // d2 ALARM: ACTIVE 但远未过期
        subscriptionManager.upsertIntent(d2, SubscriptionConstant.Type.ALARM, true);
        subscriptionManager.markActive(d2, SubscriptionConstant.Type.ALARM, "c-d2", 3600);

        LocalDateTime threshold = LocalDateTime.now().plusSeconds(120);
        List<DeviceSubscriptionDTO> expiring = subscriptionManager.listExpiring(threshold);

        assertTrue(expiring.stream().anyMatch(s -> "c-d1".equals(s.getCallId())), "短过期的应被选中");
        assertFalse(expiring.stream().anyMatch(s -> "c-d2".equals(s.getCallId())), "远过期的不应被选中");
    }

    @Test
    public void testTouchLastNotify() {
        String deviceId = UniqueKeyFactory.deviceId();
        subscriptionManager.upsertIntent(deviceId, SubscriptionConstant.Type.CATALOG, true);

        LocalDateTime when = LocalDateTime.now();
        subscriptionManager.touchLastNotify(deviceId, SubscriptionConstant.Type.CATALOG, when);

        DeviceSubscriptionDTO sub = subscriptionManager.getByDeviceAndType(deviceId, SubscriptionConstant.Type.CATALOG);
        assertNotNull(sub.getLastNotifyTime());
    }

    @Test
    public void testListByDeviceIds_BatchFill() {
        String d1 = UniqueKeyFactory.deviceId();
        String d2 = UniqueKeyFactory.deviceId();
        subscriptionManager.upsertIntent(d1, SubscriptionConstant.Type.CATALOG, true);
        subscriptionManager.upsertIntent(d2, SubscriptionConstant.Type.ALARM, true);

        List<DeviceSubscriptionDTO> all = subscriptionManager.listByDeviceIds(List.of(d1, d2));
        assertTrue(all.size() >= 2);
        assertTrue(all.stream().anyMatch(s -> s.getDeviceId().equals(d1)));
        assertTrue(all.stream().anyMatch(s -> s.getDeviceId().equals(d2)));
    }
}
