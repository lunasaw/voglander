package io.github.lunasaw.voglander.intergration.wrapper.zlm.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * ZLM集成配置
 * 用于配置ZLMediaKit的相关参数
 *
 * @author luna
 * @date 2025/01/22
 */
@Getter
@Slf4j
@Configuration
@ConditionalOnProperty(value = "zlm.enable", havingValue = "true")
@ConfigurationProperties(prefix = "zlm")
public class ZlmIntegrationConfig {

    /**
     * 是否启用ZLM
     */
    private boolean enable = true;

    /**
     * Hook是否启用
     */
    private boolean               hookEnable = true;

    /**
     * ZLM服务器配置列表
     */
    private List<ZlmServerConfig> servers;

    public void setEnable(boolean enable) {
        this.enable = enable;
        if (enable) {
            log.info("ZLM集成模块已启用");
        } else {
            log.info("ZLM集成模块已禁用");
        }
    }

    public void setHookEnable(boolean hookEnable) {
        this.hookEnable = hookEnable;
        log.info("ZLM Hook功能{}", hookEnable ? "已启用" : "已禁用");
    }

    public void setServers(List<ZlmServerConfig> servers) {
        this.servers = servers;
        if (servers != null && !servers.isEmpty()) {
            log.info("配置了{}台ZLM服务器", servers.size());
        }
    }

    /**
     * 获取默认ZLM服务器配置
     *
     * @return 默认服务器配置
     */
    public ZlmServerConfig getDefaultServer() {
        if (servers == null || servers.isEmpty()) {
            log.warn("没有配置ZLM服务器，将返回null");
            return null;
        }
        return servers.get(0);
    }

    /**
     * ZLM服务器配置
     */
    @Getter
    public static class ZlmServerConfig {
        private String serverId;
        private String host;
        private int    port;
        private String secret;

        public void setServerId(String serverId) {
            this.serverId = serverId;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        /**
         * 获取主机端口组合字符串
         *
         * @return host:port格式字符串
         */
        public String getHostPort() {
            return host + ":" + port;
        }
    }
}