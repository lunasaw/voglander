package io.github.lunasaw.voglander.service.task;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.voglander.common.event.BusinessTaskAuditRecord;
import lombok.extern.slf4j.Slf4j;

/** Structured, redacted audit sink for task control commands. */
@Slf4j
@Component
public class BusinessTaskAuditService {

    private static final int MAX_RECENT = 1000;
    private final CopyOnWriteArrayList<BusinessTaskAuditRecord> recent = new CopyOnWriteArrayList<>();

    public void record(BusinessTaskAuditRecord record) {
        if (record == null) {
            return;
        }
        BusinessTaskAuditRecord safe = new BusinessTaskAuditRecord(record.isAccepted(), record.getTraceId(),
            record.getActorType(), record.getActorId(), record.getTaskId(), record.getExecutionId(),
            record.getCommand(), record.getPreviousState(), record.getCurrentState(), record.getResultCode(),
            record.getTimestamp() <= 0 ? Instant.now().toEpochMilli() : record.getTimestamp());
        recent.add(safe);
        while (recent.size() > MAX_RECENT) {
            recent.remove(0);
        }
        log.info("business_task_audit={}", JSON.toJSONString(safe.toStructuredData()));
    }

    public List<BusinessTaskAuditRecord> recentRecords() {
        return Collections.unmodifiableList(new ArrayList<>(recent));
    }
}
