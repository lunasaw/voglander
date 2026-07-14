package io.github.lunasaw.voglander;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import io.github.lunasaw.voglander.config.CacheTestConfig;
import io.github.lunasaw.voglander.config.TestRedisConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring集成测试基类
 * 用于需要完整Spring容器和真实Bean注入的集成测试
 *
 * 特点：
 * - 完整的Spring Boot应用上下文
 * - 真实的Bean注入和依赖关系
 * - 数据库事务支持（测试结束后自动回滚）
 * - 使用随机端口避免并发测试端口冲突
 * - 使用 Mock Redis beans 避免真实 Redis 连接
 * - 适用于跨层级的集成测试
 *
 * 注意：
 * - 仅适用于同步操作测试
 * - 异步操作测试请使用 BaseAsyncTest（避免 @Transactional 跨线程问题）
 *
 * @author luna
 * @date 2025/6/22
 */
@Slf4j
@SpringBootTest(classes = {
    io.github.lunasaw.voglander.web.ApplicationWeb.class,
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({CacheTestConfig.class, TestRedisConfig.class})
@Transactional
@TestPropertySource(properties = {
    "local.sip.server.enabled=true",
    "local.sip.server.ip=127.0.0.1",
    "local.sip.server.port=5060",
    "local.sip.server.domain=34020000002000000001",
    "local.sip.server.serverId=34020000002000000001",
    "local.sip.server.serverName=GB28181-Server",
    "local.sip.server.enableUdp=true",
    "local.sip.server.enableTcp=false",
    "local.sip.client.enabled=true",
    "local.sip.client.clientId=34020000001320000001",
    "local.sip.client.clientName=GB28181-Client",
    "local.sip.client.username=admin",
    "local.sip.client.password=123456",
    "local.sip.client.ip=127.0.0.1",
    "local.sip.client.port=5061",
    "local.sip.client.realm=34020000",
    "sip.enable=false",
    "sip.enable-log=true",
    "sip.server.enabled=true",
    "sip.server.ip=127.0.0.1",
    "sip.server.port=5060",
    "sip.server.domain=34020000002000000001",
    "sip.server.serverId=34020000002000000001",
    "sip.server.serverName=GB28181-Server",
    "sip.client.enabled=true",
    "logging.level.io.github.lunasaw.sip=INFO",
    "logging.level.io.github.lunasaw.gbproxy=INFO",
    "sse.type=local"
})
public abstract class BaseTest {

    /**
     * 随机分配的服务器端口，避免并发测试时端口冲突
     * 子类可以使用此字段构建测试 URL
     */
    @LocalServerPort
    protected int port;

    @BeforeEach
    public void baseSetUp() {
        log.debug("BaseTest setUp - Spring integration test initialized on port {}", port);
    }
}
