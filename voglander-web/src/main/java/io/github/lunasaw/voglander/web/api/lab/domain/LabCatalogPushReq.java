package io.github.lunasaw.voglander.web.api.lab.domain;

import lombok.Data;

/** 目录上报请求 */
@Data
public class LabCatalogPushReq {
    private int channelCount = 4;
    private String catalogName = "Lab-Channel";
}
