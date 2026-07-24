package io.github.lunasaw.voglander.web.api.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionQueryDTO;
import io.github.lunasaw.voglander.web.api.task.assembler.BusinessTaskWebAssembler;
import io.github.lunasaw.voglander.web.api.task.req.BusinessTaskPageReq;
import io.github.lunasaw.voglander.web.api.task.req.BusinessTaskExecutionPageReq;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskExecutionVO;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskConstraintsVO;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskVO;

@DisplayName("Business-task Web contract assembler")
class BusinessTaskWebAssemblerTest {

    private final BusinessTaskWebAssembler assembler = new BusinessTaskWebAssembler();

    @Test
    @DisplayName("task page request maps Unix milliseconds to LocalDateTime and fixed query fields")
    void pageRequest_shouldMapEpochMillisAndFilters() {
        BusinessTaskPageReq request = new BusinessTaskPageReq();
        request.setTaskId("btask_1");
        request.setTaskType("IMAGE_COLLECTION");
        request.setState("FAILED");
        request.setCreateStartTime(1_752_556_800_000L);
        request.setCreateEndTime(1_752_556_860_000L);
        request.setSortField("createTime");
        request.setSortDirection("DESC");

        io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskQueryDTO query =
            assembler.pageReqToQuery(request);

        assertEquals("btask_1", query.getTaskId());
        assertEquals("IMAGE_COLLECTION", query.getTaskType());
        assertEquals("FAILED", query.getState());
        assertEquals(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(1_752_556_800_000L),
            ZoneId.systemDefault()), query.getCreateStartTime());
        assertEquals("createTime", query.getSortField());
        assertEquals("DESC", query.getSortDirection());
    }

    @Test
    @DisplayName("execution page request maps execution filters without accepting sensitive fields")
    void executionPageRequest_shouldMapSafeFilters() {
        BusinessTaskExecutionPageReq request = new BusinessTaskExecutionPageReq();
        request.setTaskId("btask_1");
        request.setState("FAILED");
        request.setPlannedStartTime(1_752_556_800_000L);
        request.setRetryable(Boolean.TRUE);

        BizTaskExecutionQueryDTO query = assembler.executionPageReqToQuery(request);

        assertEquals("btask_1", query.getTaskId());
        assertEquals("FAILED", query.getState());
        assertEquals(Boolean.TRUE, query.getRetryable());
        assertEquals(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(1_752_556_800_000L),
            ZoneId.systemDefault()), query.getPlannedStartTime());
    }

    @Test
    @DisplayName("task VO uses Unix milliseconds and omits payload")
    void taskVo_shouldUseEpochMillisAndHidePayload() {
        BizTaskDTO task = new BizTaskDTO();
        task.setTaskId("btask_1");
        task.setTaskType("IMAGE_COLLECTION");
        task.setState("FAILED");
        task.setVersion(7);
        task.setCreateTime(LocalDateTime.of(2026, 7, 15, 10, 0));
        task.setUpdateTime(LocalDateTime.of(2026, 7, 15, 10, 1));
        task.setPayload("{\"secret\":\"do-not-expose\"}");
        task.setResultSummary("{\"message\":\"safe\",\"path\":\"/private/a.jpg\"}");

        BusinessTaskVO vo = assembler.toTaskVO(task);

        assertEquals("btask_1", vo.getTaskId());
        assertEquals(7, vo.getVersion());
        assertEquals(LocalDateTime.of(2026, 7, 15, 10, 0).atZone(ZoneId.systemDefault()).toInstant()
            .toEpochMilli(), vo.getCreateTime());
        String serialized = JSON.toJSONString(vo);
        assertFalse(serialized.contains("payload"));
        assertFalse(vo.getResultSummary().contains("path"));
        assertFalse(vo.getResultSummary().contains("/private"));
    }

    @Test
    @DisplayName("execution VO omits claim token and sanitizes result")
    void executionVo_shouldHideClaimAndSanitizeResult() {
        BizTaskExecutionDTO execution = new BizTaskExecutionDTO();
        execution.setExecutionId("bexec_1");
        execution.setTaskId("btask_1");
        execution.setState("FAILED");
        execution.setClaimToken("claim-secret");
        execution.setWorkerNode("worker-a");
        execution.setStartedAt(LocalDateTime.of(2026, 7, 15, 10, 0));
        execution.setResultSummary("{\"error\":\"bad\",\"stack\":\"java.lang.Error\",\"path\":\"/private/a\"}");

        BusinessTaskExecutionVO vo = assembler.toExecutionVO(execution);

        assertEquals("bexec_1", vo.getExecutionId());
        String serialized = JSON.toJSONString(vo);
        assertFalse(serialized.contains("claimToken"));
        assertFalse(serialized.contains("workerNode"));
        assertEquals(LocalDateTime.of(2026, 7, 15, 10, 0).atZone(ZoneId.systemDefault()).toInstant()
            .toEpochMilli(), vo.getStartedAt());
        assertFalse(vo.getResultSummary().contains("stack"));
        assertFalse(vo.getResultSummary().contains("path"));
    }

    @Test
    @DisplayName("constraints and statistics expose stable codes without internal fields")
    void constraintsAndStatistics_shouldUseStableCodes() {
        BusinessTaskConstraintsVO constraints = assembler.toConstraintsVO(Arrays.asList("IMAGE_COLLECTION"),
            Collections.singletonMap("IMAGE_COLLECTION", Arrays.asList("MANUAL_RETRY")));

        assertEquals(Arrays.asList("IMAGE_COLLECTION"), constraints.getTaskTypes());
        assertEquals(Arrays.asList("MANUAL_RETRY"), constraints.getCapabilities().get("IMAGE_COLLECTION"));
        assertFalse(constraints.getTaskStates().contains("PAYLOAD"));
    }
}
