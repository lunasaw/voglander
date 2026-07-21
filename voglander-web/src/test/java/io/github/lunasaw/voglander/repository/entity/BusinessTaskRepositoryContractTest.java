package io.github.lunasaw.voglander.repository.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

/** Static field and mapping contracts for the durable business-task Repository layer. */
@DisplayName("Durable business-task Repository contracts")
class BusinessTaskRepositoryContractTest {

    private static final List<String> TASK_FIELDS = Arrays.asList(
        "id", "createTime", "updateTime", "taskId", "taskType", "taskName", "description", "taskMode",
        "scheduleStartTime", "scheduleEndTime", "intervalSeconds", "nextPlanTime", "scheduleVersion", "state",
        "priority", "lastExecutionId", "lastExecuteTime", "completedTime", "plannedCount", "successCount",
        "failedCount", "missedCount", "cancelledCount", "progressCurrent", "progressTotal", "progressMessage",
        "progressRevision", "bizKey", "subjectType", "subjectId", "payload", "payloadVersion", "resultRefType",
        "resultRefId", "resultSummary", "lastFailureCode", "lastFailureMessage", "originTaskId",
        "originExecutionId", "ownerType", "ownerId", "organizationId", "idempotencyKey", "version");

    private static final List<String> EXECUTION_FIELDS = Arrays.asList(
        "id", "createTime", "updateTime", "executionId", "taskId", "scheduleVersion", "plannedAt", "deadlineAt",
        "state", "attemptCount", "maxAttempts", "nextAttemptTime", "startedAt", "heartbeatAt", "finishedAt",
        "claimToken", "workerNode", "leaseUntil", "progressCurrent", "progressTotal", "progressMessage",
        "progressRevision", "resultRefType", "resultRefId", "resultSummary", "failureCode", "failureMessage",
        "retryable", "retryOriginExecutionId", "version");

    private static final List<String> EVENT_FIELDS = Arrays.asList(
        "id", "createTime", "eventId", "taskId", "executionId", "eventType", "fromState", "toState",
        "attemptNo", "progressCurrent", "progressTotal", "progressMessage", "failureCode", "failureMessage",
        "actorType", "actorId", "workerNode", "traceId", "dedupeKey", "eventData", "occurredAt");

    static Stream<Arguments> repositoryModels() {
        return Stream.of(
            Arguments.of(BizTaskDO.class, TASK_FIELDS, "voglander-repository/src/main/resources/mapper/BizTaskMapper.xml"),
            Arguments.of(BizTaskExecutionDO.class, EXECUTION_FIELDS,
                "voglander-repository/src/main/resources/mapper/BizTaskExecutionMapper.xml"),
            Arguments.of(BizTaskEventDO.class, EVENT_FIELDS,
                "voglander-repository/src/main/resources/mapper/BizTaskEventMapper.xml"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("repositoryModels")
    @DisplayName("DO 与显式 ResultMap 应完整覆盖设计字段")
    void modelsAndResultMaps_shouldCoverAllFields(Class<?> modelType, List<String> fields, String mapperPath)
        throws Exception {
        Field idField = modelType.getDeclaredField("id");
        TableId tableId = idField.getAnnotation(TableId.class);
        assertNotNull(tableId, modelType.getSimpleName() + ".id 缺少 @TableId");
        assertEquals(IdType.AUTO, tableId.type());

        String xml = readProjectFile(mapperPath);
        assertTrue(xml.contains("<resultMap id=\"BaseResultMap\""), mapperPath + " 缺少 BaseResultMap");
        for (String field : fields) {
            assertNotNull(modelType.getDeclaredField(field), modelType.getSimpleName() + " 缺少字段 " + field);
            assertTrue(xml.contains("property=\"" + field + "\""), mapperPath + " 未映射字段 " + field);
        }
        assertFalse(xml.contains("${"), mapperPath + " 禁止原始 SQL 插值");
        assertFalse(xml.toUpperCase().contains("SELECT *"), mapperPath + " 查询必须使用固定列清单");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("repositoryModels")
    @DisplayName("Repository 时间字段使用 LocalDateTime")
    void repositoryTimeFields_shouldUseLocalDateTime(Class<?> modelType, List<String> fields, String mapperPath)
        throws NoSuchFieldException {
        assertEquals(LocalDateTime.class, modelType.getDeclaredField("createTime").getType());
        if (fields.contains("updateTime")) {
            assertEquals(LocalDateTime.class, modelType.getDeclaredField("updateTime").getType());
        }
        if (fields.contains("occurredAt")) {
            assertEquals(LocalDateTime.class, modelType.getDeclaredField("occurredAt").getType());
        }
    }

    private String readProjectFile(String relativePath) throws IOException {
        return Files.readString(projectRoot().resolve(relativePath), StandardCharsets.UTF_8);
    }

    private Path projectRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("sql/voglander-sqlite.sql"))) {
            current = current.getParent();
        }
        assertNotNull(current, "无法定位 voglander 项目根目录");
        return current;
    }
}
