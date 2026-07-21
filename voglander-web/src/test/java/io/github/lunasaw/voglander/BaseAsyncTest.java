package io.github.lunasaw.voglander;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import io.github.lunasaw.voglander.config.CacheTestConfig;
import io.github.lunasaw.voglander.config.TestRedisConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * 异步操作测试基类
 * 专门用于包含异步操作（@Async、CountDownLatch、后台线程）的集成测试
 *
 * <h3>为什么需要单独的异步测试基类？</h3>
 * <p>
 * {@link BaseTest} 使用 {@code @Transactional} 注解实现测试后自动回滚。
 * 但 Spring 事务是线程绑定的（ThreadLocal），异步操作在不同线程中执行，
 * 导致以下问题：
 * </p>
 * <ul>
 *   <li>主测试线程无法看到异步线程写入的数据（事务隔离）</li>
 *   <li>{@code CountDownLatch.await()} 超时，因为异步数据未提交</li>
 *   <li>异步线程的数据库操作可能失败或不可见</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li>测试使用 {@code @Async} 注解的异步方法</li>
 *   <li>测试后台线程处理（如消息队列消费者）</li>
 *   <li>测试需要验证跨线程数据可见性的场景</li>
 *   <li>测试使用 {@code CountDownLatch} 或 {@code CompletableFuture} 的场景</li>
 * </ul>
 *
 * <h3>使用方法</h3>
 * <pre>{@code
 * @SpringBootTest
 * class MyAsyncTest extends BaseAsyncTest {
 *
 *     @Autowired
 *     private MyAsyncService asyncService;
 *
 *     @Autowired
 *     private MyMapper mapper;
 *
 *     private String testDataId;
 *
 *     @Test
 *     void testAsyncOperation() throws InterruptedException {
 *         // 使用唯一标识避免并发测试冲突
 *         testDataId = "test-" + System.currentTimeMillis();
 *
 *         CountDownLatch latch = new CountDownLatch(1);
 *
 *         // 异步操作
 *         asyncService.processAsync(testDataId, () -> latch.countDown());
 *
 *         // 等待异步完成
 *         assertTrue(latch.await(5, TimeUnit.SECONDS));
 *
 *         // 验证异步操作结果（可以查询到数据）
 *         MyEntity result = mapper.selectById(testDataId);
 *         assertNotNull(result);
 *     }
 *
 *     @AfterEach
 *     void cleanup() {
 *         // 手动清理测试数据
 *         if (testDataId != null) {
 *             mapper.deleteById(testDataId);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li><strong>必须手动清理</strong>：没有 @Transactional 自动回滚，必须在 @AfterEach 中清理数据</li>
 *   <li><strong>使用唯一标识</strong>：用 {@code UniqueKeyFactory} 或时间戳生成唯一 ID，避免并发测试冲突</li>
 *   <li><strong>清理要幂等</strong>：清理逻辑应该能安全地多次执行（如检查 null 后再删除）</li>
 *   <li><strong>设置合理超时</strong>：CountDownLatch.await() 应设置足够但不过长的超时（建议 5-10 秒）</li>
 *   <li><strong>使用 Mock Redis</strong>：通过 {@link TestRedisConfig} 避免真实 Redis 连接</li>
 * </ul>
 *
 * <h3>与 BaseTest 的对比</h3>
 * <table border="1">
 *   <tr>
 *     <th>特性</th>
 *     <th>BaseTest</th>
 *     <th>BaseAsyncTest</th>
 *   </tr>
 *   <tr>
 *     <td>事务支持</td>
 *     <td>✅ @Transactional</td>
 *     <td>❌ 无事务</td>
 *   </tr>
 *   <tr>
 *     <td>数据清理</td>
 *     <td>自动回滚</td>
 *     <td>手动 @AfterEach</td>
 *   </tr>
 *   <tr>
 *     <td>适用场景</td>
 *     <td>同步 Manager/Service</td>
 *     <td>异步操作/后台线程</td>
 *   </tr>
 *   <tr>
 *     <td>跨线程可见性</td>
 *     <td>❌ 不支持</td>
 *     <td>✅ 支持</td>
 *   </tr>
 * </table>
 *
 * @author luna
 * @see BaseTest 同步操作测试基类
 * @see io.github.lunasaw.voglander.test.util.UniqueKeyFactory 唯一标识生成工具
 * @date 2026/07/08
 */
@Slf4j
@SpringBootTest(classes = {
    io.github.lunasaw.voglander.web.ApplicationWeb.class,
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({CacheTestConfig.class, TestRedisConfig.class})
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
public abstract class BaseAsyncTest {

    /**
     * 随机分配的服务器端口，避免并发测试时端口冲突
     * 子类可以使用此字段构建测试 URL
     */
    @LocalServerPort
    protected int port;
}
