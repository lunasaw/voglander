package io.github.lunasaw.voglander.web.api.image.assembler;

import java.time.ZoneId;

import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageCollectionEnrichedDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.assembler.BusinessTaskDataSanitizer;
import io.github.lunasaw.voglander.web.api.image.vo.ImageCollectionCreateVO;
import io.github.lunasaw.voglander.web.api.image.vo.ImageCollectionVO;

@Component
public class ImageCollectionWebAssembler {
    public ImageCollectionVO toVO(ImageCollectionEnrichedDTO source) {
        if (source == null || source.getTask() == null || source.getConfig() == null) return null;
        BizTaskDTO task = source.getTask(); ImageCollectionVO vo = new ImageCollectionVO();
        vo.setTaskId(task.getTaskId()); vo.setVersion(task.getVersion()); vo.setScheduleVersion(task.getScheduleVersion());
        vo.setCapabilities(source.getCapabilities()); vo.setTaskName(task.getTaskName()); vo.setTaskMode(task.getTaskMode()); vo.setState(task.getState());
        vo.setScheduleStartTime(epoch(task.getScheduleStartTime())); vo.setScheduleEndTime(epoch(task.getScheduleEndTime()));
        vo.setIntervalSeconds(task.getIntervalSeconds()); vo.setNextPlanTime(epoch(task.getNextPlanTime()));
        vo.setPlannedCount(task.getPlannedCount()); vo.setSuccessCount(task.getSuccessCount()); vo.setFailedCount(task.getFailedCount());
        vo.setMissedCount(task.getMissedCount()); vo.setCancelledCount(task.getCancelledCount());
        vo.setProgressCurrent(task.getProgressCurrent()); vo.setProgressTotal(task.getProgressTotal());
        vo.setProgressMessage(task.getProgressMessage()); vo.setProgressRevision(task.getProgressRevision());
        vo.setLastExecutionId(task.getLastExecutionId()); vo.setResultRefType(task.getResultRefType());
        vo.setResultRefId(task.getResultRefId()); vo.setResultSummary(BusinessTaskDataSanitizer.sanitizeJson(task.getResultSummary()));
        vo.setLastFailureCode(task.getLastFailureCode()); vo.setLastFailureMessage(task.getLastFailureMessage());
        vo.setDeviceId(source.getConfig().getDeviceId()); vo.setChannelId(source.getConfig().getChannelId());
        vo.setDeviceName(source.getConfig().getDeviceNameSnapshot()); vo.setChannelName(source.getConfig().getChannelNameSnapshot());
        vo.setRetentionPolicy(source.getConfig().getRetentionPolicy()); vo.setCreateTime(epoch(task.getCreateTime())); vo.setUpdateTime(epoch(task.getUpdateTime()));
        return vo;
    }

    public ImageCollectionCreateVO toCreateVO(BizTaskDTO task) {
        ImageCollectionCreateVO vo = new ImageCollectionCreateVO();
        if (task != null) { vo.setTaskId(task.getTaskId()); vo.setState(task.getState()); vo.setPlannedCount(task.getPlannedCount()); vo.setNextPlanTime(epoch(task.getNextPlanTime())); }
        return vo;
    }

    private static Long epoch(java.time.LocalDateTime value) { return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(); }
}
