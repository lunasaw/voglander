package io.github.lunasaw.voglander.manager.manager;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import io.github.lunasaw.voglander.common.enums.task.TaskEventTypeEnum;
import io.github.lunasaw.voglander.manager.assembler.BizTaskEventAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskAccessScopeDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskEventDTO;
import io.github.lunasaw.voglander.repository.entity.BizTaskEventDO;
import io.github.lunasaw.voglander.repository.mapper.BizTaskEventMapper;

/** Append-only Manager boundary for durable task audit events. */
@Component
public class BizTaskEventManager {

    @Autowired
    private BizTaskEventMapper bizTaskEventMapper;

    @Autowired
    private BizTaskEventAssembler bizTaskEventAssembler;

    @Autowired
    private BizTaskManager bizTaskManager;

    /** Appends one sanitized event fact; duplicate identities are idempotent. */
    public boolean append(BizTaskEventDTO event) {
        validateEvent(event);
        BizTaskEventDO row = bizTaskEventAssembler.dtoToSafeDo(event);
        row.setCreateTime(row.getCreateTime() == null ? LocalDateTime.now() : row.getCreateTime());
        return bizTaskEventMapper.insertIfAbsent(row) == 1;
    }

    /** Returns an access-scoped, chronological, sanitized event timeline. */
    public List<BizTaskEventDTO> getTimeline(String taskId, String executionId, BizTaskAccessScopeDTO scope,
        int limit) {
        Assert.hasText(taskId, "taskId不能为空");
        Assert.notNull(scope, "任务访问范围不能为空");
        Assert.isTrue(limit > 0 && limit <= 1000, "事件时间线大小必须在1-1000之间");
        if (bizTaskManager.getByTaskId(taskId, scope) == null) {
            return Collections.emptyList();
        }
        return bizTaskEventMapper.selectTimeline(taskId, executionId, limit).stream()
            .map(bizTaskEventAssembler::doToSafeDto)
            .collect(Collectors.toList());
    }

    private void validateEvent(BizTaskEventDTO event) {
        Assert.notNull(event, "业务任务事件不能为空");
        Assert.hasText(event.getEventId(), "eventId不能为空");
        Assert.hasText(event.getTaskId(), "taskId不能为空");
        Assert.hasText(event.getEventType(), "eventType不能为空");
        TaskEventTypeEnum.valueOf(event.getEventType());
        Assert.notNull(event.getOccurredAt(), "occurredAt不能为空");
    }
}
