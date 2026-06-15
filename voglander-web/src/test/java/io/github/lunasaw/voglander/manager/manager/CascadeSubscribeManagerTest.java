package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.common.constant.cascade.CascadeConstant;
import io.github.lunasaw.voglander.common.constant.cascade.CascadeConstant.SubType;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeSubscribeDTO;
import io.github.lunasaw.voglander.manager.service.CascadeSubscribeService;
import io.github.lunasaw.voglander.repository.entity.CascadeSubscribeDO;

/**
 * CascadeSubscribeManager 集成测试
 *
 * @author luna
 */
@DisplayName("CascadeSubscribeManager 集成测试")
@Transactional
class CascadeSubscribeManagerTest extends BaseTest {

    @Autowired
    private CascadeSubscribeManager cascadeSubscribeManager;

    @Autowired
    private CascadeSubscribeService cascadeSubscribeService;

    private String platformId() {
        return "PF" + System.nanoTime();
    }

    @AfterEach
    void cleanup() {
        // @Transactional 回滚兜底，这里不额外清理
    }

    @Nested
    @DisplayName("upsertActive")
    class Upsert {

        @Test
        @DisplayName("首次登记应插入 ACTIVE 记录并回填 expireTime")
        void should_insert_active_and_fill_expire_time() {
            String pf = platformId();
            Long id = cascadeSubscribeManager.upsertActive(pf, SubType.CATALOG, "100", 3600, null);
            assertNotNull(id);

            CascadeSubscribeDTO saved = cascadeSubscribeManager.getByPlatformAndType(pf, SubType.CATALOG);
            assertNotNull(saved);
            assertEquals(CascadeConstant.SubStatus.ACTIVE, saved.getStatus());
            assertEquals(3600, saved.getExpires());
            assertNotNull(saved.getExpireTime());
            assertTrue(saved.getExpireTime().isAfter(LocalDateTime.now()));
        }

        @Test
        @DisplayName("同 platform+type 再次登记应更新而非新增(UNIQUE)")
        void should_update_not_insert_on_same_key() {
            String pf = platformId();
            cascadeSubscribeManager.upsertActive(pf, SubType.CATALOG, "100", 3600, null);
            cascadeSubscribeManager.upsertActive(pf, SubType.CATALOG, "200", 1800, null);

            LambdaQueryWrapper<CascadeSubscribeDO> qw = new LambdaQueryWrapper<>();
            qw.eq(CascadeSubscribeDO::getPlatformId, pf).eq(CascadeSubscribeDO::getSubType, SubType.CATALOG.name());
            assertEquals(1, cascadeSubscribeService.count(qw), "应只有一条记录");

            CascadeSubscribeDTO saved = cascadeSubscribeManager.getByPlatformAndType(pf, SubType.CATALOG);
            assertEquals("200", saved.getSn());
            assertEquals(1800, saved.getExpires());
        }

        @Test
        @DisplayName("expires<=0 或 null 应回落默认值")
        void should_fallback_default_expires() {
            String pf = platformId();
            cascadeSubscribeManager.upsertActive(pf, SubType.ALARM, "1", null, null);
            CascadeSubscribeDTO saved = cascadeSubscribeManager.getByPlatformAndType(pf, SubType.ALARM);
            assertEquals(CascadeConstant.DEFAULT_SUBSCRIBE_EXPIRES, saved.getExpires());
        }

        @Test
        @DisplayName("MOBILE_POSITION 应保存 interval")
        void should_save_interval_for_mobile() {
            String pf = platformId();
            cascadeSubscribeManager.upsertActive(pf, SubType.MOBILE_POSITION, "1", 3600, 5);
            CascadeSubscribeDTO saved = cascadeSubscribeManager.getByPlatformAndType(pf, SubType.MOBILE_POSITION);
            assertEquals(5, saved.getIntervalSec());
        }
    }

    @Nested
    @DisplayName("expire")
    class Expire {

        @Test
        @DisplayName("退订应将 status 置为 EXPIRED")
        void should_set_expired() {
            String pf = platformId();
            cascadeSubscribeManager.upsertActive(pf, SubType.CATALOG, "1", 3600, null);
            assertTrue(cascadeSubscribeManager.expire(pf, SubType.CATALOG));

            CascadeSubscribeDTO saved = cascadeSubscribeManager.getByPlatformAndType(pf, SubType.CATALOG);
            assertEquals(CascadeConstant.SubStatus.EXPIRED, saved.getStatus());
        }

        @Test
        @DisplayName("退订不存在的订阅应返回 false")
        void should_return_false_when_not_exists() {
            assertFalse(cascadeSubscribeManager.expire(platformId(), SubType.CATALOG));
        }
    }

    @Nested
    @DisplayName("listActiveByType")
    class ListByType {

        @Test
        @DisplayName("应只返回指定类型的 ACTIVE 订阅")
        void should_return_only_active_of_type() {
            String pf1 = platformId();
            String pf2 = platformId();
            cascadeSubscribeManager.upsertActive(pf1, SubType.CATALOG, "1", 3600, null);
            cascadeSubscribeManager.upsertActive(pf2, SubType.CATALOG, "2", 3600, null);
            cascadeSubscribeManager.upsertActive(pf1, SubType.ALARM, "3", 3600, null);
            cascadeSubscribeManager.expire(pf2, SubType.CATALOG); // pf2 catalog 退订

            List<CascadeSubscribeDTO> catalogs = cascadeSubscribeManager.listActiveByType(SubType.CATALOG);
            assertTrue(catalogs.stream().anyMatch(s -> s.getPlatformId().equals(pf1)));
            assertFalse(catalogs.stream().anyMatch(s -> s.getPlatformId().equals(pf2)), "退订的不应出现");
        }
    }

    @Nested
    @DisplayName("cleanExpired")
    class CleanExpired {

        @Test
        @DisplayName("应将 expireTime 早于 now 的 ACTIVE 标为 EXPIRED")
        void should_expire_overdue() {
            String pf = platformId();
            cascadeSubscribeManager.upsertActive(pf, SubType.CATALOG, "1", 3600, null);
            // 手动把 expireTime 改到过去
            CascadeSubscribeDTO dto = cascadeSubscribeManager.getByPlatformAndType(pf, SubType.CATALOG);
            CascadeSubscribeDO doObj = cascadeSubscribeService.getById(dto.getId());
            doObj.setExpireTime(LocalDateTime.now().minusSeconds(10));
            cascadeSubscribeService.updateById(doObj);

            int cleaned = cascadeSubscribeManager.cleanExpired(LocalDateTime.now());
            assertTrue(cleaned >= 1);

            CascadeSubscribeDTO after = cascadeSubscribeManager.getByPlatformAndType(pf, SubType.CATALOG);
            assertEquals(CascadeConstant.SubStatus.EXPIRED, after.getStatus());
        }

        @Test
        @DisplayName("未过期的不应被清理")
        void should_keep_not_overdue() {
            String pf = platformId();
            cascadeSubscribeManager.upsertActive(pf, SubType.CATALOG, "1", 3600, null);
            cascadeSubscribeManager.cleanExpired(LocalDateTime.now());
            CascadeSubscribeDTO after = cascadeSubscribeManager.getByPlatformAndType(pf, SubType.CATALOG);
            assertEquals(CascadeConstant.SubStatus.ACTIVE, after.getStatus());
        }
    }
}
