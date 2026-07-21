package io.github.lunasaw.voglander.web.api.image.vo;

import lombok.Data;

@Data
public class ImageAssetConstraintsVO {
    private long maxUploadBytes;
    private long maxPixels;
    private String[] formats;
    private int maxPlannedCount;
    private int minIntervalSeconds;
}
