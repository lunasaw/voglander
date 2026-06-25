package io.github.lunasaw.voglander.web.api.cascade.req;

import java.io.Serializable;

import lombok.Data;

/**
 * 级联上报通道更新请求（Web 入参，id 必填）。
 *
 * <p>
 * platformId/localDeviceId/localChannelId 为身份字段，编辑时不可改（前端只展示）；
 * 后端按 id 更新 cascadeChannelId/cascadeName/enabled。
 * </p>
 *
 * @author luna
 * @date 2026/06/16
 */
@Data
public class CascadeChannelUpdateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID（必填）
     */
    private Long    id;

    /**
     * 所属上级平台国标 ID（展示，不可改）
     */
    private String  platformId;

    /**
     * 本地设备国标 ID（展示，不可改）
     */
    private String  localDeviceId;

    /**
     * 本地通道国标 ID（展示，不可改）
     */
    private String  localChannelId;

    /**
     * 对上级暴露的级联通道编码
     */
    private String  cascadeChannelId;

    /**
     * 对上级暴露的通道名称
     */
    private String  cascadeName;

    /**
     * 启用状态 1启用 / 0停用
     */
    private Integer enabled;
}
