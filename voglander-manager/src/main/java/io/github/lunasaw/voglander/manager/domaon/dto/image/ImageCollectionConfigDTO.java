package io.github.lunasaw.voglander.manager.domaon.dto.image;

import java.time.LocalDateTime;

import com.alibaba.fastjson2.JSONObject;

import lombok.Data;

@Data
public class ImageCollectionConfigDTO {
    private Long id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String taskId;
    private String deviceId;
    private String channelId;
    private String deviceNameSnapshot;
    private String channelNameSnapshot;
    private String retentionPolicy;
    private JSONObject captureOptions;
    private Integer version;
}
