package io.github.lunasaw.voglander.web.api.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.web.api.task.req.BusinessTaskPageReq;
import io.github.lunasaw.voglander.web.api.task.resp.BusinessTaskExecutionListResp;
import io.github.lunasaw.voglander.web.api.task.resp.BusinessTaskListResp;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskExecutionVO;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@DisplayName("Business-task backend OpenAPI contract")
class BusinessTaskOpenApiContractTest {

    @Test
    void controller_shouldPublishStableTaggedAjaxEndpoints() {
        Tag tag = BusinessTaskController.class.getAnnotation(Tag.class);
        assertNotNull(tag, "业务任务 Controller 必须注册 OpenAPI tag");
        assertEquals("业务任务", tag.name());
        RequestMapping mapping = BusinessTaskController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);

        List<Method> endpoints = Arrays.stream(BusinessTaskController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(GetMapping.class) || method.isAnnotationPresent(PostMapping.class))
            .toList();
        assertEquals(10, endpoints.size(), "任务查询、详情和控制端点数量发生漂移");
        for (Method endpoint : endpoints) {
            assertNotNull(endpoint.getAnnotation(Operation.class), endpoint.getName() + " 缺少 OpenAPI Operation");
            assertEquals(AjaxResult.class, endpoint.getReturnType(), endpoint.getName() + " 必须返回 AjaxResult");
        }
    }

    @Test
    void contract_shouldKeepUnixTimePaginationAndSafeExecutionFields() {
        assertEquals(Long.class, field(BusinessTaskPageReq.class, "createStartTime").getType());
        assertEquals(Long.class, field(BusinessTaskPageReq.class, "scheduleEndTime").getType());
        assertEquals(Long.class, field(BusinessTaskListResp.class, "total").getType());
        assertEquals(Long.class, field(BusinessTaskExecutionListResp.class, "total").getType());
        assertEquals(Long.class, field(BusinessTaskVO.class, "createTime").getType());
        assertEquals(Long.class, field(BusinessTaskExecutionVO.class, "finishedAt").getType());
        assertTrue(Arrays.stream(BusinessTaskExecutionVO.class.getDeclaredFields())
            .noneMatch(field -> List.of("claimToken", "workerNode", "path", "secret", "stack").contains(field.getName())));
    }

    private Field field(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name);
        } catch (NoSuchFieldException exception) {
            throw new AssertionError(type.getSimpleName() + " 缺少字段 " + name, exception);
        }
    }
}
