package io.github.lunasaw.voglander;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import lombok.extern.slf4j.Slf4j;

/**
 * Mock测试基类
 * 用于单元测试，通过Mock外部依赖来隔离测试目标
 * 
 * 特点：
 * - 轻量级Spring容器（排除Redis、WebMvc等外部依赖）
 * - 使用@MockBean模拟外部依赖
 * - 专注于单个组件的逻辑测试
 * - 测试执行速度快，不依赖外部资源
 * 
 * 使用方式：
 * 1. 继承此类
 * 2. 使用@MockBean注解模拟需要的依赖
 * 3. 在测试方法中使用when().thenReturn()设置Mock行为
 * 4. 使用verify()验证Mock对象的调用
 * 
 * @author luna
 * @date 2025/7/19
 */
@Slf4j
@SpringBootTest(classes = {
    io.github.lunasaw.voglander.config.TestConfig.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cache.type=simple"
})
public abstract class BaseMockTest {

    @BeforeEach
    public void baseMockSetUp() {
        log.debug("BaseMockTest setUp - Mock test environment initialized");
    }
}