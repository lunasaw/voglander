package io.github.lunasaw.voglander.intergration.wrapper.zlm.common;

import org.springframework.util.Assert;

/**
 * ZLM Wrapper层专用验证工具类
 * <p>
 * 专门处理ZLM相关的参数验证，避免在通用工具类中放置特定业务验证逻辑
 * </p>
 * 
 * @author luna
 * @since 2025-01-23
 */
public class ZlmWrapperValidator {

    /**
     * 验证ZLM连接参数
     * 
     * @param host ZLM节点地址
     * @param secret ZLM节点密钥
     * @throws IllegalArgumentException 当参数为空时
     */
    public static void validateZlmConnection(String host, String secret) {
        Assert.hasText(host, "ZLM节点地址不能为空");
        Assert.hasText(secret, "ZLM节点密钥不能为空");
    }

    /**
     * 验证应用和流参数
     *
     * @param app 应用名称
     * @param stream 流名称
     * @throws IllegalArgumentException 当参数为空时
     */
    public static void validateAppAndStream(String app, String stream) {
        Assert.hasText(app, "应用名称不能为空");
        Assert.hasText(stream, "流名称不能为空");
    }

    public static void validateMedia(String app, String stream, String schema, String vhost) {
        Assert.hasText(schema, "协议不能为空");
        Assert.hasText(vhost, "虚拟主机名称不能为空");
        Assert.hasText(app, "应用名称不能为空");
        Assert.hasText(stream, "流名称不能为空");
    }

    /**
     * 验证代理key
     * 
     * @param proxyKey 代理key
     * @throws IllegalArgumentException 当代理key为空时
     */
    public static void validateProxyKey(String proxyKey) {
        Assert.hasText(proxyKey, "代理key不能为空");
    }

    /**
     * 验证拉流地址
     * 
     * @param url 拉流地址
     * @throws IllegalArgumentException 当拉流地址为空时
     */
    public static void validateStreamUrl(String url) {
        Assert.hasText(url, "拉流地址不能为空");
    }
}