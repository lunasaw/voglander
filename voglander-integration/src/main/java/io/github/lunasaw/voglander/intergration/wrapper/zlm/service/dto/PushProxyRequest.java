package io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto;

import io.github.lunasaw.zlm.entity.StreamPusherItem;
import lombok.Data;

/**
 * 推流代理请求参数
 * <p>
 * 封装ZLM推流代理API调用所需的参数，包括连接信息和推流配置
 * </p>
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
public class PushProxyRequest {

    /**
     * ZLM服务器地址
     */
    private String           host;

    /**
     * ZLM服务器密钥
     */
    private String           secret;

    /**
     * 推流代理配置项
     */
    private StreamPusherItem streamPusherItem;

    /**
     * 构造函数
     *
     * @param host ZLM服务器地址
     * @param secret ZLM服务器密钥
     * @param streamPusherItem 推流代理配置项
     */
    public PushProxyRequest(String host, String secret, StreamPusherItem streamPusherItem) {
        this.host = host;
        this.secret = secret;
        this.streamPusherItem = streamPusherItem;
    }

    /**
     * 默认构造函数
     */
    public PushProxyRequest() {}

    /**
     * Builder模式 - 创建请求对象
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
        private String           host;
        private String           secret;
        private StreamPusherItem streamPusherItem;

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder secret(String secret) {
            this.secret = secret;
            return this;
        }

        public Builder streamPusherItem(StreamPusherItem streamPusherItem) {
            this.streamPusherItem = streamPusherItem;
            return this;
        }

        public PushProxyRequest build() {
            return new PushProxyRequest(host, secret, streamPusherItem);
        }
    }
}