package io.github.lunasaw.voglander.web.api.cascade.req;

import java.io.Serializable;

import lombok.Data;

/**
 * 级联上级平台分页条件查询请求（Web 入参）。
 *
 * @author luna
 * @date 2026/06/16
 */
@Data
public class CascadePlatformPageReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 上级平台国标 ID（精确匹配）
     */
    private String  platformId;

    /**
     * 上级平台 IP（模糊匹配）
     */
    private String  platformIp;

    /**
     * 启用状态 1启用 / 0停用
     */
    private Integer enabled;

    /**
     * 注册状态 0离线 / 1在线 / 2注册中 / 3失败
     */
    private Integer registerStatus;
}
