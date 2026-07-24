package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.assembler.BizTaskAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskAccessScopeDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskQueryDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskStatisticsDTO;
import io.github.lunasaw.voglander.manager.service.BizTaskService;
import io.github.lunasaw.voglander.repository.entity.BizTaskDO;

@DisplayName("BizTaskManager query, scope and statistics")
class BizTaskManagerQueryTest extends BaseTest {

    @Autowired
    private BizTaskManager bizTaskManager;

    @Autowired
    private BizTaskService bizTaskService;

    @Autowired
    private BizTaskAssembler bizTaskAssembler;

    private final List<String> taskIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        if (!taskIds.isEmpty()) {
            bizTaskService.remove(new LambdaQueryWrapper<BizTaskDO>().in(BizTaskDO::getTaskId, taskIds));
        }
    }

    @Test
    @DisplayName("分页应使用 access scope、固定筛选和 createTime/taskId 确定排序")
    void getPage_shouldApplyAccessScopeAndDeterministicDefaultSort() {
        String suffix = suffix();
        LocalDateTime sameTime = LocalDateTime.of(2026, 7, 14, 12, 0);
        save(task("btask_a_" + suffix, "OWNER_A", "RUNNING", sameTime));
        save(task("btask_b_" + suffix, "OWNER_A", "RUNNING", sameTime));
        save(task("btask_hidden_" + suffix, "OWNER_B", "RUNNING", sameTime.plusMinutes(1)));

        BizTaskQueryDTO query = new BizTaskQueryDTO();
        query.setTaskType("QUERY_TEST");
        Page<BizTaskDTO> page = bizTaskManager.getPage(query,
            BizTaskAccessScopeDTO.owner("USER", "OWNER_A"), 1, 10);

        assertEquals(2L, page.getTotal());
        assertEquals(Arrays.asList("btask_b_" + suffix, "btask_a_" + suffix),
            page.getRecords().stream().map(BizTaskDTO::getTaskId).toList());
        assertNull(page.getRecords().get(0).getPayload());
        assertFalse(page.getRecords().get(0).getResultSummary().toLowerCase().contains("secret"));
    }

    @Test
    @DisplayName("详情在同一 scope 内可见，scope 外表现为不存在")
    void getByTaskId_shouldHideTasksOutsideScope() {
        String suffix = suffix();
        BizTaskDTO visible = task("btask_visible_" + suffix, "OWNER_A", "RUNNING", LocalDateTime.now());
        BizTaskDTO hidden = task("btask_hidden_" + suffix, "OWNER_B", "RUNNING", LocalDateTime.now());
        save(visible);
        save(hidden);

        BizTaskAccessScopeDTO scope = BizTaskAccessScopeDTO.owner("USER", "OWNER_A");
        BizTaskDTO detail = bizTaskManager.getByTaskId(visible.getTaskId(), scope);
        assertNotNull(detail);
        assertEquals(visible.getTaskId(), detail.getTaskId());
        assertNull(bizTaskManager.getByTaskId(hidden.getTaskId(), scope));
    }

    @Test
    @DisplayName("统计应与查询使用完全相同的 access scope")
    void getStatistics_shouldCountOnlyVisibleTasks() {
        String suffix = suffix();
        String ownerA = "OWNER_A_" + suffix;
        String ownerB = "OWNER_B_" + suffix;
        LocalDateTime now = LocalDateTime.now().withNano(0);
        save(task("btask_scheduled_" + suffix, ownerA, "SCHEDULED", now));
        save(task("btask_running_" + suffix, ownerA, "RUNNING", now));
        save(task("btask_paused_" + suffix, ownerA, "PAUSED", now));
        save(task("btask_cancelling_" + suffix, ownerA, "CANCELLING", now));
        BizTaskDTO completed = task("btask_completed_" + suffix, ownerA, "COMPLETED", now);
        completed.setCompletedTime(now);
        save(completed);
        save(task("btask_failed_" + suffix, ownerA, "FAILED", now));
        save(task("btask_hidden_running_" + suffix, ownerB, "RUNNING", now));

        BizTaskStatisticsDTO statistics = bizTaskManager.getStatistics(
            BizTaskAccessScopeDTO.owner("USER", ownerA));
        assertEquals(1L, statistics.getScheduledCount());
        assertEquals(1L, statistics.getRunningCount());
        assertEquals(1L, statistics.getPausedCount());
        assertEquals(1L, statistics.getCancellingCount());
        assertEquals(1L, statistics.getCompletedTodayCount());
        assertEquals(1L, statistics.getFailedCount());
    }

    @Test
    @DisplayName("任务类型 allowlist 应同时约束分页、详情和统计")
    void allowedTaskTypes_shouldApplyToEveryReadPath() {
        String suffix = suffix();
        BizTaskDTO image = task("btask_image_" + suffix, "OWNER", "RUNNING", LocalDateTime.now());
        image.setTaskType("IMAGE_COLLECTION");
        BizTaskDTO export = task("btask_export_" + suffix, "OWNER", "RUNNING", LocalDateTime.now());
        export.setTaskType("DATA_EXPORT");
        save(image);
        save(export);
        BizTaskAccessScopeDTO scope = BizTaskAccessScopeDTO.global();
        scope.setAllowedTaskTypes(Collections.singleton("IMAGE_COLLECTION"));

        Page<BizTaskDTO> page = bizTaskManager.getPage(new BizTaskQueryDTO(), scope, 1, 10);

        assertEquals(1L, page.getTotal());
        assertEquals(image.getTaskId(), page.getRecords().get(0).getTaskId());
        assertNotNull(bizTaskManager.getByTaskId(image.getTaskId(), scope));
        assertNull(bizTaskManager.getByTaskId(export.getTaskId(), scope));
        assertEquals(1L, bizTaskManager.getStatistics(scope).getRunningCount());
    }

    private BizTaskDTO task(String taskId, String ownerId, String state, LocalDateTime createTime) {
        BizTaskDTO task = new BizTaskDTO();
        task.setCreateTime(createTime.withNano(0));
        task.setUpdateTime(createTime.withNano(0));
        task.setTaskId(taskId);
        task.setTaskType("QUERY_TEST");
        task.setTaskName("query test");
        task.setTaskMode("ONCE");
        task.setScheduleVersion(1);
        task.setState(state);
        task.setPriority(0);
        task.setPlannedCount(1);
        task.setSuccessCount(0);
        task.setFailedCount(0);
        task.setMissedCount(0);
        task.setCancelledCount(0);
        task.setProgressCurrent(0L);
        task.setProgressTotal(0L);
        task.setProgressRevision(0L);
        task.setPayload("{\"internal\":true}");
        task.setPayloadVersion(1);
        task.setResultSummary("{\"result\":\"ok\",\"secret\":\"hidden\"}");
        task.setOwnerType("USER");
        task.setOwnerId(ownerId);
        task.setVersion(0);
        return task;
    }

    private void save(BizTaskDTO task) {
        taskIds.add(task.getTaskId());
        bizTaskService.save(bizTaskAssembler.dtoToDo(task));
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
