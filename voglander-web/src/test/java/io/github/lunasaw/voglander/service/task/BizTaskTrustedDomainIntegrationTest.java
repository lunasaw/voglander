package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.client.domain.task.RetryDecision;
import io.github.lunasaw.voglander.client.domain.task.TaskAttemptContext;
import io.github.lunasaw.voglander.client.domain.task.TaskCapabilities;
import io.github.lunasaw.voglander.client.domain.task.TaskCreateCommand;
import io.github.lunasaw.voglander.client.domain.task.TaskCreateContext;
import io.github.lunasaw.voglander.client.domain.task.TaskExecutionResult;
import io.github.lunasaw.voglander.client.service.task.LongTaskContext;
import io.github.lunasaw.voglander.client.service.task.LongTaskHandler;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.service.BizTaskExecutionService;
import io.github.lunasaw.voglander.manager.service.BizTaskService;
import io.github.lunasaw.voglander.repository.entity.BizTaskDO;
import io.github.lunasaw.voglander.repository.entity.BizTaskExecutionDO;

@DisplayName("Trusted domain task creation integration")
@Import(BizTaskTrustedDomainIntegrationTest.TestConfig.class)
class BizTaskTrustedDomainIntegrationTest extends BaseTest {

    @Autowired
    private TrustedFakeDomainService trustedFakeDomainService;

    @Autowired
    private BizTaskService bizTaskService;

    @Autowired
    private BizTaskExecutionService bizTaskExecutionService;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Test
    @DisplayName("受信任领域服务可通过已注册 fake Handler 创建 durable task")
    void trustedDomainService_shouldCreateTaskThroughRegisteredHandler() {
        String idempotencyKey = "trusted-domain-" + UUID.randomUUID().toString().replace("-", "");

        BizTaskDTO accepted = trustedFakeDomainService.create(idempotencyKey);

        assertNotNull(accepted.getTaskId());
        assertEquals("TDD_FAKE_TASK", accepted.getTaskType());
        assertEquals(1L, bizTaskService.count(new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getTaskId, accepted.getTaskId())));
        assertEquals(1L, bizTaskExecutionService.count(new LambdaQueryWrapper<BizTaskExecutionDO>()
            .eq(BizTaskExecutionDO::getTaskId, accepted.getTaskId())));
    }

    @Test
    @DisplayName("Web 层不得暴露接收任意 taskType+payload 的 generic creation endpoint")
    void web_shouldNotExposeGenericTaskCreationEndpoint() {
        boolean genericCreationPresent = false;
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry
            : requestMappingHandlerMapping.getHandlerMethods().entrySet()) {
            if (entry.getKey().getMethodsCondition().getMethods().contains(RequestMethod.POST)
                && entry.getKey().getPatternValues().contains("/api/v1/business-tasks")) {
                genericCreationPresent = true;
                break;
            }
        }
        assertFalse(genericCreationPresent);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        LongTaskHandler trustedFakeLongTaskHandler() {
            return new TrustedFakeLongTaskHandler();
        }

        @Bean
        TrustedFakeDomainService trustedFakeDomainService(BizTaskCreateService createService) {
            return new TrustedFakeDomainService(createService);
        }
    }

    static final class TrustedFakeDomainService {
        private final BizTaskCreateService createService;

        TrustedFakeDomainService(BizTaskCreateService createService) {
            this.createService = createService;
        }

        BizTaskDTO create(String idempotencyKey) {
            JSONObject payload = new JSONObject();
            payload.put("domainValue", "validated");
            TaskCreateCommand command = new TaskCreateCommand("TDD_FAKE_TASK", "trusted fake task", null,
                "ONCE", null, null, null, payload, 1, "trusted-biz", "TEST_SUBJECT", "subject-1", "SYSTEM",
                "trusted-domain-service", null, idempotencyKey, 1);
            return createService.create(command);
        }
    }

    static final class TrustedFakeLongTaskHandler implements LongTaskHandler {
        @Override
        public String taskType() {
            return "TDD_FAKE_TASK";
        }

        @Override
        public int payloadVersion() {
            return 1;
        }

        @Override
        public TaskCapabilities capabilities() {
            return TaskCapabilities.none();
        }

        @Override
        public void validate(TaskCreateContext context, JSONObject payload) {
            if (!"validated".equals(payload.getString("domainValue"))) {
                throw new IllegalArgumentException("domainValue must be validated");
            }
        }

        @Override
        public TaskExecutionResult execute(LongTaskContext context, JSONObject payload) {
            return null;
        }

        @Override
        public RetryDecision classify(Throwable throwable, TaskAttemptContext context) {
            return RetryDecision.permanent("TDD_FAKE_FAILED", "fake handler failure");
        }
    }
}
