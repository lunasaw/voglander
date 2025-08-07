package io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto;

import io.github.lunasaw.zlm.entity.StreamProxyItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流代理请求DTO
 * <p>
 * 封装ZLM流代理操作的请求参数，提供Builder模式构建
 * </p>
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamProxyRequest {

    /**
     * ZLM节点地址
     */
    private String          host;

    /**
     * ZLM节点密钥
     */
    private String          secret;

    /**
     * 流代理配置
     */
    private StreamProxyItem streamProxyItem;

    /**
     * 静态Builder方法
     */
    public static StreamProxyRequestBuilder builder() {
        return new StreamProxyRequestBuilder();
    }

    /**
     * 便捷创建方法
     */
    public static StreamProxyRequest of(String host, String secret, StreamProxyItem streamProxyItem) {
        return StreamProxyRequest.builder()
            .host(host)
            .secret(secret)
            .streamProxyItem(streamProxyItem)
            .build();
    }
}