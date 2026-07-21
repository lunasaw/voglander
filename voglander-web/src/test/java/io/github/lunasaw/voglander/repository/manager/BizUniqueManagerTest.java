package io.github.lunasaw.voglander.repository.manager;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import io.github.lunasaw.voglander.support.RedisAvailableExtension;
import io.github.lunasaw.voglander.support.UniqueKeyFactory;
import io.github.lunasaw.voglander.web.ApplicationWeb;

/**
 * BizUniqueManager 集成测试（依赖 Redis）
 *
 * @author luna
 */
@DisplayName("BizUniqueManager 幂等管理测试")
@ExtendWith(RedisAvailableExtension.class)
@SpringBootTest(classes = ApplicationWeb.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "local.sip.enable=false",
        "local.sip.server.enabled=false",
        "local.sip.client.enabled=false",
        "sip.enable=false",
        "sse.type=local",
        "voglander.protocol-lab.enabled=false",
        "voglander.test.mock-redis=false"
    })
@ActiveProfiles("test")
class BizUniqueManagerTest {

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
