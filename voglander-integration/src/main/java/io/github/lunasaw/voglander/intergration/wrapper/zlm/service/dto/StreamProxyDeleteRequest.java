package io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流代理删除请求DTO
 * <p>
 * 封装ZLM流代理删除操作的请求参数，提供Builder模式构建
 * </p>
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamProxyDeleteRequest {

    /**
     * ZLM节点地址
     */
    private String host;

    /**
     * ZLM节点密钥
     */
    private String secret;

    /**
     * 代理密钥
     */
    private String proxyKey;

    /**
     * 静态Builder方法
     */
    public static StreamProxyDeleteRequestBuilder builder() {
        return new StreamProxyDeleteRequestBuilder();
    }

    /**
     * 便捷创建方法
     */
    public static StreamProxyDeleteRequest of(String host, String secret, String proxyKey) {
        return StreamProxyDeleteRequest.builder()
            .host(host)
            .secret(secret)
            .proxyKey(proxyKey)
            .build();
    }
}