package io.github.lunasaw.voglander.repository.manager;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.support.RedisAvailableExtension;
import io.github.lunasaw.voglander.support.UniqueKeyFactory;

/**
 * BizUniqueManager 集成测试（依赖 Redis）
 *
 * @author luna
 */
@DisplayName("BizUniqueManager 幂等管理测试")
@ExtendWith(RedisAvailableExtension.class)
class BizUniqueManagerTest extends BaseTest {

    @Autowired
    private BizUniqueManager bizUniqueManager;

    private String lastBizType;
    private String lastBizNo;

    @AfterEach
    void cleanup() {
        if (lastBizType != null && lastBizNo != null) {
            try {
                bizUniqueManager.deleteUniqueRecord(lastBizType, lastBizNo);
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    @DisplayName("首次 check 应通过（不抛异常）")
    void should_pass_check_when_not_exists() {
        String bizType = "test";
        String bizNo = UniqueKeyFactory.callId();
        assertDoesNotThrow(() -> bizUniqueManager.check(bizType, bizNo));
    }

    @Test
    @DisplayName("createUniqueRecord 后二次 check 应抛 ServiceException")
    void should_throw_on_second_check_after_create() {
        String bizType = "test";
        String bizNo = UniqueKeyFactory.callId();
        lastBizType = bizType;
        lastBizNo = bizNo;

        bizUniqueManager.createUniqueRecord(bizType, bizNo);
        assertThrows(Exception.class, () -> bizUniqueManager.check(bizType, bizNo));
    }

    @Test
    @DisplayName("deleteUniqueRecord 后 check 应重新通过")
    void should_pass_check_after_delete() {
        String bizType = "test";
        String bizNo = UniqueKeyFactory.callId();

        bizUniqueManager.createUniqueRecord(bizType, bizNo);
        bizUniqueManager.deleteUniqueRecord(bizType, bizNo);
        assertDoesNotThrow(() -> bizUniqueManager.check(bizType, bizNo));
    }
}
