package io.github.lunasaw.voglander.web.api.cascade.req;

import java.io.Serializable;

import lombok.Data;

/**
 * 级联上级平台更新请求（Web 入参，id 必填）。
 *
 * <p>
 * platformId 为唯一索引，编辑时不可改（前端只展示）；后端按 id 更新其余字段。
 * </p>
 *
 * @author luna
 * @date 2026/06/16
 */
@Data
public class CascadePlatformUpdateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID（必填）
     */
    private Long    id;

    /**
     * 上级平台国标 ID（展示，不可改）
     */
    private String  platformId;

    /**
     * 上级平台 IP
     */
    private String  platformIp;

    /**
     * 上级平台端口
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
     * 本端客户端国标 ID
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
     * 保活心跳间隔（秒）
     */
    private Integer keepaliveInterval;

    /**
     * 注册有效期（秒）
     */
    private Integer registerExpires;

    /**
     * 编码（GB2312/UTF-8）
     */
    private String  charset;

    /**
     * 传输协议 UDP / TCP
     */
    private String  transport;
}
