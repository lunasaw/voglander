package io.github.lunasaw.voglander.web.api.cascade.req;

import java.io.Serializable;

import lombok.Data;

/**
 * 级联上报通道新增请求（Web 入参）。
 *
 * @author luna
 * @date 2026/06/16
 */
@Data
public class CascadeChannelCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 所属上级平台国标 ID（必填）
     */
    private String platformId;

    /**
     * 本地设备国标 ID（必填）
     */
    private String localDeviceId;

    /**
     * 本地通道国标 ID（必填）
     */
    private String localChannelId;

    /**
     * 对上级暴露的级联通道编码（国标 20 位，必填）
     */
    private String cascadeChannelId;

    /**
     * 对上级暴露的通道名称
     */
    private String cascadeName;
}
