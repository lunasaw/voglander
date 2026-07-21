package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.assembler.BizTaskAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskAccessScopeDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskEventDTO;
import io.github.lunasaw.voglander.manager.service.BizTaskEventService;
import io.github.lunasaw.voglander.manager.service.BizTaskService;
import io.github.lunasaw.voglander.repository.entity.BizTaskEventDO;

@DisplayName("BizTaskEventManager append-only event boundary")
class BizTaskEventManagerTest extends BaseTest {

    @Autowired
    private BizTaskEventManager bizTaskEventManager;

    @Autowired
    private BizTaskEventService bizTaskEventService;

    @Autowired
    private BizTaskService bizTaskService;

    @Autowired
    private BizTaskAssembler bizTaskAssembler;

    @Test
    @DisplayName("append 应按 eventId/dedupeKey 幂等并在持久化前清理敏感 eventData")
    void append_shouldBeIdempotentAndPersistSanitizedData() {
        String suffix = suffix();
        String taskId = "btask_event_" + suffix;
        saveTask(task(taskId, "OWNER_A"));
        BizTaskEventDTO event = event("bevt_first_" + suffix, taskId, "terminal-" + suffix,
            LocalDateTime.now().withNano(0));
        event.setEventData("{\"assetId\":\"img-1\",\"secret\":\"hidden\",\"storagePath\":\"/tmp/a\"}");

        assertTrue(bizTaskEventManager.append(event));
        assertFalse(bizTaskEventManager.append(event));

        BizTaskEventDTO duplicateDedupe = event("bevt_second_" + suffix, taskId, "terminal-" + suffix,
            event.getOccurredAt().plusSeconds(1));
        assertFalse(bizTaskEventManager.append(duplicateDedupe));

        BizTaskEventDO stored = bizTaskEventService.getOne(new LambdaQueryWrapper<BizTaskEventDO>()
            .eq(BizTaskEventDO::getEventId, event.getEventId()));
        assertTrue(stored.getEventData().contains("assetId"));
        assertFalse(stored.getEventData().toLowerCase().contains("secret"));
        assertFalse(stored.getEventData().toLowerCase().contains("path"));
    }

    @Test
    @DisplayName("timeline 应校验 task scope、按 occurredAt/id 升序并支持 execution 过滤")
    void timeline_shouldApplyScopeOrderingAndExecutionFilter() {
        String suffix = suffix();
        String visibleTaskId = "btask_visible_event_" + suffix;
        String hiddenTaskId = "btask_hidden_event_" + suffix;
        String executionId = "bexec_event_" + suffix;
        saveTask(task(visibleTaskId, "OWNER_A"));
        saveTask(task(hiddenTaskId, "OWNER_B"));
        LocalDateTime now = LocalDateTime.now().withNano(0);
        BizTaskEventDTO later = event("bevt_later_" + suffix, visibleTaskId, null, now.plusSeconds(1));
        later.setExecutionId(executionId);
        BizTaskEventDTO earlier = event("bevt_earlier_" + suffix, visibleTaskId, null, now);
        BizTaskEventDTO hidden = event("bevt_hidden_" + suffix, hiddenTaskId, null, now);
        assertTrue(bizTaskEventManager.append(later));
        assertTrue(bizTaskEventManager.append(earlier));
        assertTrue(bizTaskEventManager.append(hidden));

        BizTaskAccessScopeDTO ownerScope = BizTaskAccessScopeDTO.owner("USER", "OWNER_A");
        List<BizTaskEventDTO> timeline = bizTaskEventManager.getTimeline(visibleTaskId, null, ownerScope, 100);
        assertEquals(Arrays.asList(earlier.getEventId(), later.getEventId()),
            timeline.stream().map(BizTaskEventDTO::getEventId).toList());
        assertEquals(List.of(later.getEventId()), bizTaskEventManager
            .getTimeline(visibleTaskId, executionId, ownerScope, 100).stream()
            .map(BizTaskEventDTO::getEventId).toList());
        assertTrue(bizTaskEventManager.getTimeline(hiddenTaskId, null, ownerScope, 100).isEmpty());
    }

    @Test
    @DisplayName("EventManager 公开 API 只能追加或读取，不能更新或删除")
    void publicApi_shouldRemainAppendOnly() {
        Set<String> methodNames = Arrays.stream(BizTaskEventManager.class.getDeclaredMethods())
            .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .collect(Collectors.toSet());

        assertEquals(Set.of("append", "getTimeline"), methodNames);
        assertFalse(methodNames.stream().anyMatch(name -> name.startsWith("update") || name.startsWith("delete")
            || name.startsWith("remove")));
    }

    private BizTaskEventDTO event(String eventId, String taskId, String dedupeKey, LocalDateTime occurredAt) {
        BizTaskEventDTO event = new BizTaskEventDTO();
        event.setEventId(eventId);
        event.setTaskId(taskId);
        event.setEventType("CREATED");
        event.setDedupeKey(dedupeKey);
        event.setOccurredAt(occurredAt);
        return event;
    }

    private BizTaskDTO task(String taskId, String ownerId) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        BizTaskDTO task = new BizTaskDTO();
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setTaskId(taskId);
        task.setTaskType("EVENT_TEST");
        task.setTaskName("event test");
        task.setTaskMode("ONCE");
        task.setScheduleVersion(1);
        task.setState("RUNNING");
        task.setPriority(0);
        task.setPlannedCount(1);
        task.setSuccessCount(0);
        task.setFailedCount(0);
        task.setMissedCount(0);
        task.setCancelledCount(0);
        task.setProgressCurrent(0L);
        task.setProgressTotal(0L);
        task.setProgressRevision(0L);
        task.setPayload("{}");
        task.setPayloadVersion(1);
        task.setOwnerType("USER");
        task.setOwnerId(ownerId);
        task.setVersion(0);
        return task;
    }

    private void saveTask(BizTaskDTO task) {
        bizTaskService.save(bizTaskAssembler.dtoToDo(task));
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
