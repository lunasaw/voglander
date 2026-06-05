package io.github.lunasaw.voglander.web.api.lab.domain;

import lombok.Data;

/** 设备注册请求（Lab 用） */
@Data
public class LabRegisterReq {
    private int expires = 3600;
}
