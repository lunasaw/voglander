package io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto;

import lombok.Data;

/**
 * 推流代理删除请求参数
 * <p>
 * 封装ZLM推流代理删除API调用所需的参数
 * </p>
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
public class PushProxyDeleteRequest {

    /**
     * ZLM服务器地址
     */
    private String host;

    /**
     * ZLM服务器密钥
     */
    private String secret;

    /**
     * 推流代理密钥
     */
    private String proxyKey;

    /**
     * 构造函数
     *
     * @param host ZLM服务器地址
     * @param secret ZLM服务器密钥
     * @param proxyKey 推流代理密钥
     */
    public PushProxyDeleteRequest(String host, String secret, String proxyKey) {
        this.host = host;
        this.secret = secret;
        this.proxyKey = proxyKey;
    }

    /**
     * 默认构造函数
     */
    public PushProxyDeleteRequest() {}

    /**
     * Builder模式 - 创建删除请求对象
     *
     * @return Builder实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder内部类
     */
    public static class Builder {
        private String host;
        private String secret;
        private String proxyKey;

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder secret(String secret) {
            this.secret = secret;
            return this;
        }

        public Builder proxyKey(String proxyKey) {
            this.proxyKey = proxyKey;
            return this;
        }

        public PushProxyDeleteRequest build() {
            return new PushProxyDeleteRequest(host, secret, proxyKey);
        }
    }
}