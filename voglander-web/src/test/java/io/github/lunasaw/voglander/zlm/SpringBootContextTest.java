package io.github.lunasaw.voglander.zlm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import lombok.extern.slf4j.Slf4j;

/**
 * Spring Boot 上下文加载测试
 * 验证测试基类是否正确配置了Spring Boot应用上下文
 *
 * @author luna
 * @date 2025-01-08
 */
@Slf4j
@DisplayName("Spring上下文加载测试")
@SpringBootTest(classes = {
    io.github.lunasaw.voglander.web.ApplicationWeb.class,
}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
public class SpringBootContextTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("应用上下文加载测试")
    public void testApplicationContextLoading() {
        log.info("=== Spring Boot 应用上下文加载测试 ===");

        assertNotNull(applicationContext, "应用上下文不能为空");
        log.info("应用上下文加载成功，定义的Bean数量: {}", applicationContext.getBeanDefinitionCount());

        // 打印一些关键的Bean名称
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        log.info("前20个Bean名称:");
        for (int i = 0; i < Math.min(20, beanNames.length); i++) {
            log.info("  {}: {}", i + 1, beanNames[i]);
        }

        // 检查关键Bean是否存在
        boolean hasStreamProxyManager = applicationContext.containsBean("streamProxyManager");
        boolean hasHookService = applicationContext.containsBean("voglanderZlmHookServiceImpl");

        log.info("StreamProxyManager Bean 存在: {}", hasStreamProxyManager);
        log.info("VoglanderZlmHookServiceImpl Bean 存在: {}", hasHookService);

        // 如果Bean不存在，尝试按类型查找
        try {
            Object streamProxyManagerBean = applicationContext.getBean("streamProxyManager");
            log.info("StreamProxyManager Bean 类型: {}", streamProxyManagerBean.getClass().getName());
        } catch (Exception e) {
            log.warn("StreamProxyManager Bean 获取失败: {}", e.getMessage());

            // 尝试查找所有Manager相关的Bean
            String[] managerBeans = applicationContext.getBeanNamesForType(Object.class);
            log.info("查找包含'manager'的Bean:");
            for (String beanName : managerBeans) {
                if (beanName.toLowerCase().contains("manager")) {
                    log.info("  找到Manager Bean: {}", beanName);
                }
            }
        }

        try {
            Object hookServiceBean = applicationContext.getBean("voglanderZlmHookServiceImpl");
            log.info("VoglanderZlmHookServiceImpl Bean 类型: {}", hookServiceBean.getClass().getName());
        } catch (Exception e) {
            log.warn("VoglanderZlmHookServiceImpl Bean 获取失败: {}", e.getMessage());

            // 尝试查找所有Hook相关的Bean
            String[] hookBeans = applicationContext.getBeanNamesForType(Object.class);
            log.info("查找包含'hook'的Bean:");
            for (String beanName : hookBeans) {
                if (beanName.toLowerCase().contains("hook")) {
                    log.info("  找到Hook Bean: {}", beanName);
                }
            }
        }

        log.info("=== Spring Boot 应用上下文加载测试完成 ===");
    }
}