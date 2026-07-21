package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class ImageLogSanitizerTest {
    @Test
    void logsAcceptOnlyStableCodesAndOpaqueIdentifiers() {
        assertEquals("IMAGE_STORAGE_WRITE_FAILED", ImageLogSanitizer.code("image_storage_write_failed"));
        assertEquals("UNKNOWN", ImageLogSanitizer.code("Bearer eyJhbGciOiJIUzI1NiJ9"));
        String id = ImageLogSanitizer.identifier("img_abc-123");
        assertEquals("img_abc-123", id);
        assertFalse(ImageLogSanitizer.identifier("https://example.test/a?token=secret").contains("?"));
    }

    @Test
    void logContextBindsStableCorrelationFieldsAndRestoresMdc() {
        MDC.put("traceId", "trace-1");
        try (ImageLogContext ignored = ImageLogContext.open("btask_1", "bexec_1", "img_1", "device_1", "channel_1", "user_1")) {
            assertEquals("btask_1", MDC.get("taskId"));
            assertEquals("bexec_1", MDC.get("executionId"));
            assertFalse(MDC.getCopyOfContextMap().containsKey("secret"));
        }
        assertEquals("trace-1", MDC.get("traceId"));
        MDC.clear();
    }
}
