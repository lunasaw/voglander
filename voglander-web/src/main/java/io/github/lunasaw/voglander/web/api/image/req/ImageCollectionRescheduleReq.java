package io.github.lunasaw.voglander.web.api.image.req;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import lombok.Data;

/** Timing-only reschedule command; camera identity is immutable in the task config. */
@Data
public class ImageCollectionRescheduleReq {
    private Integer expectedVersion;
    private Long scheduleStartTime;
    private Long scheduleEndTime;
    private Long intervalSeconds;
    private String reason;

    public LocalDateTime scheduleStart() { return toTime(scheduleStartTime); }
    public LocalDateTime scheduleEnd() { return toTime(scheduleEndTime); }
    private static LocalDateTime toTime(Long value) {
        return value == null ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.systemDefault());
    }
}
