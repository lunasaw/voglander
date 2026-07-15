package io.github.lunasaw.voglander.web.api.image.vo;

import lombok.Data;

@Data
public class ImageAssetStatisticsVO {
    private long total;
    private long available;
    private long today;
    private long deleteFailed;
}
