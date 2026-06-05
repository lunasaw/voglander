package io.github.lunasaw.voglander.web.api.lab.domain;

import lombok.Data;

/** 周期心跳开关请求（Lab 用） */
@Data
public class LabKeepaliveAutoReq {

    /** true 启用周期心跳，false 关闭 */
    private boolean enabled;

    /** 心跳间隔（秒）；≤0 时后端回落默认 30s */
    private int     intervalSec = 30;
}
