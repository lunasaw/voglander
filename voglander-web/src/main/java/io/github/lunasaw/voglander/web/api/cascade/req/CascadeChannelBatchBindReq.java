package io.github.lunasaw.voglander.web.api.cascade.req;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * 级联通道批量绑定请求（Web 入参）。
 *
 * <p>
 * 从本地设备批量生成级联通道映射：同一上级平台下绑定多个本地通道，
 * cascadeChannelId 缺省时取 localChannelId（透传编码）。
 * </p>
 *
 * @author luna
 * @date 2026/06/16
 */
@Data
public class CascadeChannelBatchBindReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 所属上级平台国标 ID（必填）
     */
    private String          platformId;

    /**
     * 待绑定通道列表
     */
    private List<BindItem>  channels;

    /**
     * 单条绑定项
     */
    @Data
    public static class BindItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 本地设备国标 ID
         */
        private String localDeviceId;

        /**
         * 本地通道国标 ID（必填）
         */
        private String localChannelId;

        /**
         * 对上级暴露的级联编码（缺省=localChannelId）
         */
        private String cascadeChannelId;

        /**
         * 对上级暴露的通道名称
         */
        private String cascadeName;
    }
}
