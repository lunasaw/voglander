package io.github.lunasaw.voglander.web.api.lab.domain;

import lombok.Data;

/** 目录上报请求（兼做被查询时通道配置）。默认前缀对齐 LabChannelHolder.DEFAULT_NAME_PREFIX。 */
@Data
public class LabCatalogPushReq {
    private int channelCount = 4;
    private String catalogName = "Lab-ch";
}
