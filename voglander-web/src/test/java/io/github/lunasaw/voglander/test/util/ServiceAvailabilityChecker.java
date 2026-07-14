package io.github.lunasaw.voglander.test.util;

import java.sql.Connection;
import java.sql.DriverManager;

import lombok.extern.slf4j.Slf4j;

/**
 * 服务可用性检测工具
 * 用于集成测试中检测外部服务（Redis、PostgreSQL 等）是否可用
 *
 * <h3>使用场景</h3>
 * <p>
 * 在集成测试中，配合 JUnit 5 的 {@code Assumptions.assumeTrue()} 实现优雅的测试跳过：
 * </p>
 * <ul>
 *   <li>服务可用 → 测试正常执行</li>
 *   <li>服务不可用 → 测试自动跳过（不算失败）</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @SpringBootTest
 * class RedisIntegrationTest {
 *
 *     @BeforeEach
 *     void checkRedisAvailable() {
 *         // 如果 Redis 不可用，跳过测试
 *         Assumptions.assumeTrue(
 *             ServiceAvailabilityChecker.isRedisAvailable("localhost", 6379),
 *             "Redis not available at localhost:6379"
 *         );
 *     }
 *
 *     @Test
 *     void testRedisCache() {
 *         // 此测试只在 Redis 可用时执行
 *     }
 * }
 * }</pre>
 *
 * @author luna
 * @date 2026/07/08
 */
@Slf4j
public class ServiceAvailabilityChecker {

    /**
     * 检测 Redis 服务是否可用
     * 使用简单的 socket 连接测试
     *
     * @param host Redis 主机地址
     * @param port Redis 端口
     * @return true 如果 Redis 可用，false 否则
     */
    public static boolean isRedisAvailable(String host, int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), 1000);
            log.info("Redis is available at {}:{}", host, port);
            return true;
        } catch (Exception e) {
            log.warn("Redis not available at {}:{} - {}", host, port, e.getMessage());
            return false;
        }
    }

    /**
     * 检测 Redis 服务是否可用（默认 localhost:6379）
     *
     * @return true 如果 Redis 可用，false 否则
     */
    public static boolean isRedisAvailable() {
        return isRedisAvailable("localhost", 6379);
    }

    /**
     * 检测 PostgreSQL 服务是否可用
     *
     * @param host     PostgreSQL 主机地址
     * @param port     PostgreSQL 端口
     * @param database 数据库名称
     * @param username 用户名
     * @param password 密码
     * @return true 如果 PostgreSQL 可用，false 否则
     */
    public static boolean isPostgreSQLAvailable(String host, int port, String database, String username,
        String password) {
        String url = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            log.info("PostgreSQL is available at {}:{}/{}", host, port, database);
            return true;
        } catch (Exception e) {
            log.warn("PostgreSQL not available at {}:{}/{} - {}", host, port, database, e.getMessage());
            return false;
        }
    }

    /**
     * 检测 PostgreSQL 服务是否可用（默认 localhost:5432/voglander）
     *
     * @return true 如果 PostgreSQL 可用，false 否则
     */
    public static boolean isPostgreSQLAvailable() {
        return isPostgreSQLAvailable("localhost", 5432, "voglander", "postgres", "postgres");
    }

    /**
     * 检测 MySQL 服务是否可用
     *
     * @param host     MySQL 主机地址
     * @param port     MySQL 端口
     * @param database 数据库名称
     * @param username 用户名
     * @param password 密码
     * @return true 如果 MySQL 可用，false 否则
     */
    public static boolean isMySQLAvailable(String host, int port, String database, String username, String password) {
        String url = String.format("jdbc:mysql://%s:%d/%s", host, port, database);
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            log.info("MySQL is available at {}:{}/{}", host, port, database);
            return true;
        } catch (Exception e) {
            log.warn("MySQL not available at {}:{}/{} - {}", host, port, database, e.getMessage());
            return false;
        }
    }

    /**
     * 检测 MySQL 服务是否可用（默认 localhost:3306/voglander）
     *
     * @return true 如果 MySQL 可用，false 否则
     */
    public static boolean isMySQLAvailable() {
        return isMySQLAvailable("localhost", 3306, "voglander", "root", "root");
    }

    /**
     * 获取 Redis 不可用的跳过消息
     *
     * @param host Redis 主机地址
     * @param port Redis 端口
     * @return 跳过消息
     */
    public static String getRedisSkipMessage(String host, int port) {
        return String.format(
            "Redis not available at %s:%d. Start Redis with: brew services start redis or docker run -d -p 6379:6379 redis",
            host, port);
    }

    /**
     * 获取 Redis 不可用的跳过消息（默认 localhost:6379）
     *
     * @return 跳过消息
     */
    public static String getRedisSkipMessage() {
        return getRedisSkipMessage("localhost", 6379);
    }

    /**
     * 获取 PostgreSQL 不可用的跳过消息
     *
     * @param host     PostgreSQL 主机地址
     * @param port     PostgreSQL 端口
     * @param database 数据库名称
     * @return 跳过消息
     */
    public static String getPostgreSQLSkipMessage(String host, int port, String database) {
        return String.format(
            "PostgreSQL not available at %s:%d/%s. Start PostgreSQL with: docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=voglander postgres",
            host, port, database);
    }

    /**
     * 获取 PostgreSQL 不可用的跳过消息（默认 localhost:5432/voglander）
     *
     * @return 跳过消息
     */
    public static String getPostgreSQLSkipMessage() {
        return getPostgreSQLSkipMessage("localhost", 5432, "voglander");
    }
}
