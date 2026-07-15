package io.github.lunasaw.voglander.web.api.image.req;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import lombok.Data;

/** Web request for image collection; time values are Unix milliseconds. */
@Data
public class ImageCollectionCreateReq {
    private String taskName;
    private String collectionMode;
    private String deviceId;
    private String channelId;
    private Long scheduleStartTime;
    private Long scheduleEndTime;
    private Long intervalSeconds;
    private String retentionPolicy;

    public LocalDateTime scheduleStart() { return toTime(scheduleStartTime); }
    public LocalDateTime scheduleEnd() { return toTime(scheduleEndTime); }
    private static LocalDateTime toTime(Long value) {
        return value == null ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.systemDefault());
    }
}
