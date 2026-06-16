package io.github.lunasaw.voglander.web.api.cascade.req;

import java.io.Serializable;

import lombok.Data;

/**
 * 级联上报通道分页条件查询请求（Web 入参）。
 *
 * @author luna
 * @date 2026/06/16
 */
@Data
public class CascadeChannelPageReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 所属上级平台国标 ID（精确匹配）
     */
    private String platformId;

    /**
     * 本地设备国标 ID（精确匹配）
     */
    private String localDeviceId;

    /**
     * 本地通道国标 ID（精确匹配）
     */
    private String localChannelId;

    /**
     * 级联通道编码（精确匹配）
     */
    private String cascadeChannelId;
}
