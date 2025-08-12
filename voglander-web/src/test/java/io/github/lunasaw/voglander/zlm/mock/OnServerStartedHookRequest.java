package io.github.lunasaw.voglander.zlm.mock;

import lombok.Builder;
import lombok.Data;

/**
 * 服务器启动Hook请求参数
 *
 * @author luna
 * @date 2025-01-23
 */
@Data
@Builder
public class OnServerStartedHookRequest {

    /**
     * 媒体服务器ID
     */
    private String mediaServerId;

    /**
     * API密钥
     */
    private String apiSecret;

    /**
     * HTTP端口
     */
    private String httpPort;
}