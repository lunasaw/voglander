package io.github.lunasaw.voglander.service.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.common.event.BusinessTaskAuditRecord;

class BusinessTaskAuditServiceTest {

    @Test
    void structuredAuditRecordContainsControlFactsButNoSensitiveFields() {
        BusinessTaskAuditService service = new BusinessTaskAuditService();
        service.record(new BusinessTaskAuditRecord(true, "trace-1", "USER", "42", "btask_1", null,
            "CANCEL", "RUNNING", "CANCELLING", "OK", 1L));

        BusinessTaskAuditRecord record = service.recentRecords().get(0);
        assertTrue(record.toStructuredData().containsKey("resultCode"));
        assertFalse(record.toStructuredData().containsKey("payload"));
        assertFalse(record.toStructuredData().containsKey("secret"));
        assertFalse(record.toStructuredData().containsKey("stack"));
        assertFalse(record.toStructuredData().containsKey("reason"));
    }
}
