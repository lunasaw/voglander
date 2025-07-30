package io.github.lunasaw.voglander;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * Spring集成测试基类
 * 用于需要完整Spring容器和真实Bean注入的集成测试
 * 
 * 特点：
 * - 完整的Spring Boot应用上下文
 * - 真实的Bean注入和依赖关系
 * - 数据库事务支持（测试结束后自动回滚）
 * - 适用于跨层级的集成测试
 * 
 * @author luna
 * @date 2025/6/22
 */
@Slf4j
@SpringBootTest(classes = {
    io.github.lunasaw.voglander.web.ApplicationWeb.class,
    io.github.lunasaw.voglander.config.TestConfig.class
}, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cache.type=simple",
    "spring.datasource.url=jdbc:sqlite:test-app.db"
})
@Transactional
public abstract class BaseTest {

    @BeforeEach
    public void baseSetUp() {
        log.debug("BaseTest setUp - Spring integration test initialized");
    }
}
