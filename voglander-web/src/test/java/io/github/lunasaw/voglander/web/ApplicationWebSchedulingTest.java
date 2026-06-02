package io.github.lunasaw.voglander.web;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.github.lunasaw.gbproxy.server.config.EnableSipServer;

/**
 * Phase 0：调度启用（硬阻塞）单元测试
 * <p>
 * 校验 {@link ApplicationWeb} 同时启用了 {@link EnableScheduling}（OPTIMIZATION 兜底任务前置）
 * 与 {@link EnableSipServer}（GB28181 平台服务端能力，原有约束不可丢）。
 * </p>
 * <p>
 * 纯反射断言，不启动 Spring 上下文，验证注解级别的契约。
 * </p>
 *
 * @author luna
 */
public class ApplicationWebSchedulingTest {

    @Test
    public void testEnableSchedulingPresent() {
        EnableScheduling annotation = ApplicationWeb.class.getAnnotation(EnableScheduling.class);
        assertNotNull(annotation,
            "ApplicationWeb 必须标注 @EnableScheduling，否则 OPTIMIZATION 方案的 flush/detectOffline/sweeper 等 @Scheduled 兜底任务全部静默失败（B1 硬阻塞）");
    }

    @Test
    public void testEnableSipServerStillPresent() {
        EnableSipServer annotation = ApplicationWeb.class.getAnnotation(EnableSipServer.class);
        assertNotNull(annotation,
            "ApplicationWeb 必须保留 @EnableSipServer，缺失则 ServerCommandSender/ClientCommandSender 无法实例化");
    }

    @Test
    public void testBothSchedulingAndSipServerCoexist() {
        boolean hasScheduling = ApplicationWeb.class.isAnnotationPresent(EnableScheduling.class);
        boolean hasSipServer = ApplicationWeb.class.isAnnotationPresent(EnableSipServer.class);
        assertTrue(hasScheduling && hasSipServer,
            "@EnableScheduling 与 @EnableSipServer 必须同级共存");
    }
}
