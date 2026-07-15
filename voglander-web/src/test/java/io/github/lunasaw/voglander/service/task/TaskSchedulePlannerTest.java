package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;

@DisplayName("Task schedule cursor planner")
class TaskSchedulePlannerTest {

    @Test
    @DisplayName("FIXED_RATE 下一点必须由理论 cursor 推进，不能使用实际 now 造成漂移")
    void plan_shouldAdvanceFromFixedCursorWithoutDrift() {
        LocalDateTime cursor = LocalDateTime.of(2026, 7, 15, 10, 0);
        TaskSchedulePlanner planner = planner("2026-07-15T10:01:30Z", 30);

        TaskSchedulePoint point = planner.plan(fixedTask(cursor, cursor.plusMinutes(5), 60L, 3));

        assertEquals(cursor, point.plannedAt());
        assertEquals(cursor.plusMinutes(1), point.nextPlanTime());
        assertEquals(cursor.plusSeconds(30), point.deadlineAt());
        assertTrue(point.missed());
        assertEquals(3, point.scheduleVersion());
    }

    @Test
    @DisplayName("scheduleEndTime 本身是可物化的 inclusive 最后一点")
    void plan_shouldIncludeExactScheduleEnd() {
        LocalDateTime end = LocalDateTime.of(2026, 7, 15, 10, 5);
        TaskSchedulePlanner planner = planner("2026-07-15T10:05:00Z", 30);
        BizTaskDTO task = fixedTask(end, end, 60L, 1);

        TaskSchedulePoint point = planner.plan(task);

        assertEquals(end, point.plannedAt());
        assertNull(point.nextPlanTime());
        assertFalse(point.missed());
    }

    @Test
    @DisplayName("allowed-delay 等号边界仍可执行，只有严格超时才 MISSED")
    void plan_shouldApplyAllowedDelayAsStrictBoundary() {
        LocalDateTime plannedAt = LocalDateTime.of(2026, 7, 15, 10, 0);
        BizTaskDTO task = fixedTask(plannedAt, plannedAt.plusMinutes(1), 60L, 1);

        TaskSchedulePoint atDeadline = planner("2026-07-15T10:00:30Z", 30).plan(task);
        TaskSchedulePoint afterDeadline = planner("2026-07-15T10:00:30.001Z", 30).plan(task);

        assertFalse(atDeadline.missed());
        assertTrue(afterDeadline.missed());
    }

    @Test
    @DisplayName("AT_TIME 物化后 cursor 应清空且 plannedCount 固定为一")
    void plan_shouldFinishAtTimeCursorAfterOnePoint() {
        LocalDateTime plannedAt = LocalDateTime.of(2026, 7, 15, 10, 0);
        BizTaskDTO task = new BizTaskDTO();
        task.setTaskId("btask_at_time");
        task.setTaskMode("AT_TIME");
        task.setNextPlanTime(plannedAt);
        task.setScheduleVersion(2);

        TaskSchedulePoint point = planner("2026-07-15T10:00:00Z", 30).plan(task);

        assertEquals(plannedAt, point.plannedAt());
        assertNull(point.nextPlanTime());
        assertEquals(2, point.scheduleVersion());
    }

    @Test
    @DisplayName("非正 scheduleVersion 不得跨入 execution 唯一事实边界")
    void plan_shouldRejectNonPositiveScheduleVersion() {
        LocalDateTime cursor = LocalDateTime.of(2026, 7, 15, 10, 0);
        BizTaskDTO task = fixedTask(cursor, cursor.plusMinutes(1), 60L, 0);

        ServiceException error = assertThrows(ServiceException.class,
            () -> planner("2026-07-15T10:00:00Z", 30).plan(task));

        assertEquals(ServiceExceptionEnum.TASK_SCHEDULE_INVALID.getCode(), error.getCode());
    }

    private TaskSchedulePlanner planner(String instant, int allowedDelaySeconds) {
        return new TaskSchedulePlanner(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC), allowedDelaySeconds);
    }

    private BizTaskDTO fixedTask(LocalDateTime cursor, LocalDateTime end, Long intervalSeconds,
        int scheduleVersion) {
        BizTaskDTO task = new BizTaskDTO();
        task.setTaskId("btask_fixed");
        task.setTaskMode("FIXED_RATE");
        task.setScheduleStartTime(cursor);
        task.setScheduleEndTime(end);
        task.setIntervalSeconds(intervalSeconds);
        task.setNextPlanTime(cursor);
        task.setScheduleVersion(scheduleVersion);
        return task;
    }
}
