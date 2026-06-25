package io.github.lunasaw.voglander.web.api.cascade.req;

import java.io.Serializable;

import lombok.Data;

/**
 * 级联上级平台新增请求（Web 入参）。
 *
 * @author luna
 * @date 2026/06/16
 */
@Data
public class CascadePlatformCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 上级平台国标 ID（20 位，必填）
     */
    private String  platformId;

    /**
     * 上级平台 IP（必填）
     */
    private String  platformIp;

    /**
     * 上级平台端口（必填，默认 5060）
     */
    private Integer platformPort;

    /**
     * 上级平台域（SIP domain）
     */
    private String  platformDomain;

    /**
     * 认证用户名
     */
    private String  username;

    /**
     * 认证密码
     */
    private String  password;

    /**
     * 本端客户端国标 ID（20 位，必填）
     */
    private String  localClientId;

    /**
     * 本端 IP
     */
    private String  localIp;

    /**
     * 本端端口
     */
    private Integer localPort;

    /**
     * 保活心跳间隔（秒，默认 60）
     */
    private Integer keepaliveInterval;

    /**
     * 注册有效期（秒，默认 3600）
     */
    private Integer registerExpires;

    /**
     * 编码（GB2312/UTF-8）
     */
    private String  charset;

    /**
     * 传输协议 UDP / TCP（默认 UDP）
     */
    private String  transport;
}
