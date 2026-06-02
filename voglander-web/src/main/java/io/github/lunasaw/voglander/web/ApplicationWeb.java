package io.github.lunasaw.voglander.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.github.lunasaw.gbproxy.server.config.EnableSipServer;

/**
 * @author luna
 * @version 1.0
 * @date 2023/12/15
 * @description:
 */
@SpringBootApplication
@ComponentScan("io.github.lunasaw.voglander")
// 启用 GB28181 平台服务端能力：导入 Gb28181CommonAutoConfig（提供 CommandStrategyFactory）、
// SipProxyServerAutoConfig 及 SipProxyAutoConfig。这些 @Import 配置无 .imports 注册，
// 不会被 classpath 自动激活，因此该注解为必需（缺失则 ServerCommandSender/ClientCommandSender 无法实例化）。
@EnableSipServer
// 启用 Spring 调度能力��B1 硬阻塞前置）：高并发优化方案的 flush / detectOffline / sweeper /
// 延迟双删扫描 / 节点存活续期等周期任务全部基于 @Scheduled，缺失该注解则全部静默失败。
// 注意：启用后既有的 StreamProxyBizServiceImpl（30s）与 SpringDynamicTask（5min）两个 @Scheduled
// 任务开始运行，其中流代理同步任务受 voglander.stream-proxy.scheduled-sync.enabled 开关控制。
@EnableScheduling
public class ApplicationWeb {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationWeb.class, args);
    }
}