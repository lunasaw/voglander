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
}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
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
    "sip.enable=true",
    "sip.enable-log=true",
    "sip.server.enabled=true",
    "sip.client.enabled=true",
    "logging.level.io.github.lunasaw.sip=DEBUG",
    "logging.level.io.github.lunasaw.gbproxy=DEBUG"
})
public abstract class BaseTest {

    @BeforeEach
    public void baseSetUp() {
        log.debug("BaseTest setUp - Spring integration test initialized");
    }
}
