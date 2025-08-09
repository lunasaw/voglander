package io.github.lunasaw.voglander.zlm.mock;

import lombok.Builder;
import lombok.Data;

/**
 * 流状态变化Hook请求参数
 *
 * @author luna
 * @date 2025-01-23
 */
@Data
@Builder
public class OnStreamChangedHookRequest {

    /**
     * 媒体服务器ID
     */
    private String  mediaServerId;

    /**
     * 应用名称
     */
    private String  app;

    /**
     * 流ID
     */
    private String  stream;

    /**
     * 注册状态
     */
    private Boolean regist;

    /**
     * 协议类型
     */
    private String  schema;

    /**
     * 虚拟主机
     */
    private String  vhost;
}