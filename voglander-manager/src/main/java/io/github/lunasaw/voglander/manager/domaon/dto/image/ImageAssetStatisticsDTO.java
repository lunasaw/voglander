package io.github.lunasaw.voglander.manager.domaon.dto.image;

import lombok.Data;

@Data
public class ImageAssetStatisticsDTO {
    private long total;
    private long available;
    private long today;
    private long deleteFailed;
}
