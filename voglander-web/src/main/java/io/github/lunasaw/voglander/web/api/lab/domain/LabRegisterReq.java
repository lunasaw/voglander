package io.github.lunasaw.voglander.web.api.lab.domain;

import lombok.Data;

/** 设备注册请求（Lab 用）。target/identity 全空 = 注册到本进程自环，行为同现状。 */
@Data
public class LabRegisterReq {
    private int     expires = 3600;
    /** 目标平台覆盖。 */
    private String  serverId;
    private String  serverIp;
    private Integer serverPort;
    private String  serverDomain;
    private String  transport;       // UDP / TCP
    /** 设备身份覆盖（空=用 sip.client）。 */
    private String  clientId;
    private String  clientPassword;
}
