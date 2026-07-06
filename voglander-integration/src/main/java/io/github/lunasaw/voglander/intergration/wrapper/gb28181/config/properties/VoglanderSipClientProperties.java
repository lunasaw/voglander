package io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties;

import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.luna.common.os.SystemInfoUtil;

import lombok.Data;

/**
 * Voglander SIP客户端配置属性
 * 
 * @author luna
 * @since 2025/8/2
 */
@Data
@Component
@ConfigurationProperties(prefix = "sip.client")
public class VoglanderSipClientProperties {

    /**
     * 是否启用客户端
     */
    private boolean enabled           = false;

    /**
     * 客户端ID
     */
    private String  clientId          = "34020000001320000001";

    /**
     * 客户端名称
     */
    private String  clientName        = "voglander-client";

    /**
     * 用户名
     */
    private String  username          = "admin";

    /**
     * 密码
     */
    private String  password          = "123456";

    /**
     * 保活间隔
     */
    private String  keepAliveInterval = "1m";

    /**
     * 最大重试次数
     */
    private int     maxRetries        = 3;

    /**
     * 重试延迟
     */
    private String  retryDelay        = "5s";

    /**
     * 注册过期时间(秒)
     */
    private int     registerExpires   = 3600;

    /**
     * 客户端IP
     */
    private String  domain            = "127.0.0.1";

    /**
     * 客户端监听点 socket 的绑定地址（与对外通告地址 {@link #domain} 解耦）。
     * <p>
     * 默认 {@code 0.0.0.0}：绑定本机所有网卡，避免指定单一 IP 在切换网络后该 IP
     * 从网卡消失导致发包 {@code BindException: Can't assign requested address}，同时
     * 让任意网段（127 / 10.x / 192.x …）的对端都能抵达。{@link #domain} 仍用于
     * Via/Contact/From 等 SIP 信令的对外通告，必须是对端可回包的真实 IP。
     */
    private String  listenIp          = "0.0.0.0";

    /**
     * 客户端端口
     */
    private int     port              = 5061;

    /**
     * 域
     */
    private String  realm             = "34020000";

    /**
     * 客户端 SIP 传输协议（UDP / TCP），默认 UDP。
     * 用于 Lab 自环兜底目标设备的 Via 协商：401 Digest 重发阶段框架会按目标设备的
     * transport 构造 Via，故此值决定鉴权 REGISTER 走 UDP 还是 TCP。
     */
    private String  transport         = "UDP";

    @PostConstruct
    public void init() {
        if (StringUtils.isBlank(domain)) {
            domain = SystemInfoUtil.getIpv4();
        }
    }
}