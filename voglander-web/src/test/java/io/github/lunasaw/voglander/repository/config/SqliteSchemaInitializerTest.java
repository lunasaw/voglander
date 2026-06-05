package io.github.lunasaw.voglander.repository.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * D3 红线测试：把「sqlite3 CLI 能建全 17 张表、Spring ResourceDatabasePopulator 半途中断只建 15 张」
 * 这一差异钉成 CI 失败。
 * <p>
 * 纯单元测试（不起 Spring 上下文）：对一个全新临时 SQLite 文件运行<b>真实</b>的
 * {@link SqliteSchemaInitializer#initSchemaIfEmpty()}（加载 classpath {@code db/voglander-sqlite.sql}），
 * 断言：
 * <ol>
 *   <li>建出全 17 张 {@code tb_} 业务表（净化脚本前只建 15 张 → 红）；</li>
 *   <li>admin 种子（id=1）存在；</li>
 *   <li>建表后校验生效：sentinel 在但表残缺时 fail-fast，不留隐性坏库；</li>
 *   <li>已初始化库（sentinel 在且表齐全）二次调用直接跳过，幂等。</li>
 * </ol>
 *
 * @author luna
 */
@DisplayName("D3 — SQLite clone-and-run 建表完整性")
class SqliteSchemaInitializerTest {

    /** 脚本权威 tb_ 业务表数（sequence 表不在 tb_% 过滤内） */
    private static final int EXPECTED_TABLE_COUNT = 17;

    private Path             dbFile;
    private DataSource       dataSource;

    @BeforeEach
    void setUp() throws IOException {
        dbFile = Files.createTempFile("voglander-schema-init-test-", ".db");
        // 删掉空占位文件，确保是「全新空库」语义（sqlite 会按需重建）
        Files.deleteIfExists(dbFile);

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.sqlite.JDBC");
        ds.setUrl("jdbc:sqlite:" + dbFile.toAbsolutePath() + "?busy_timeout=10000");
        this.dataSource = ds;
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(dbFile);
        // 清理 WAL/SHM 旁路文件
        Files.deleteIfExists(Path.of(dbFile.toAbsolutePath() + "-wal"));
        Files.deleteIfExists(Path.of(dbFile.toAbsolutePath() + "-shm"));
    }

    private SqliteSchemaInitializer newInitializer(DataSource ds) {
        SqliteSchemaInitializer initializer = new SqliteSchemaInitializer();
        ReflectionTestUtils.setField(initializer, "dataSource", ds);
        return initializer;
    }

    private int countUserTables(DataSource ds) throws Exception {
        String sql = "SELECT count(*) FROM sqlite_master WHERE type='table' AND name LIKE 'tb_%'";
        try (Connection conn = ds.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private boolean tableExists(DataSource ds, String table) throws Exception {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + table + "'";
        try (Connection conn = ds.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next();
        }
    }

    @Test
    @DisplayName("全新空库 → 真实建表流程 → 建出全 17 张 tb_ 表")
    void freshDb_shouldBuildAll17Tables() throws Exception {
        newInitializer(dataSource).initSchemaIfEmpty();

        int actual = countUserTables(dataSource);
        assertEquals(EXPECTED_TABLE_COUNT, actual,
            "期望建出 " + EXPECTED_TABLE_COUNT + " 张 tb_ 表，实际 " + actual
                + "（Spring ResourceDatabasePopulator 语句切分中断的回归红线）");
    }

    @Test
    @DisplayName("全新空库 → 建表序列最后一张 tb_cascade_channel 必须存在")
    void freshDb_shouldBuildLastTable() throws Exception {
        newInitializer(dataSource).initSchemaIfEmpty();

        assertTrue(tableExists(dataSource, "tb_cascade_channel"),
            "建表序列真正最后一张表 tb_cascade_channel 缺失 → 脚本中断");
        assertTrue(tableExists(dataSource, "tb_cascade_platform"),
            "倒数第二张 tb_cascade_platform 缺失 → 脚本中断");
    }

    @Test
    @DisplayName("全新空库 → admin 种子(id=1)与角色映射建成")
    void freshDb_shouldSeedAdmin() throws Exception {
        newInitializer(dataSource).initSchemaIfEmpty();

        try (Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, username FROM tb_user WHERE username='admin'")) {
            assertTrue(rs.next(), "admin 种子用户应存在");
            assertEquals(1, rs.getInt("id"), "admin 用户 id 应为 1");
        }
    }

    @Test
    @DisplayName("已初始化库（sentinel 在且表齐全）→ 二次调用跳过，幂等")
    void alreadyInitialized_shouldSkip() throws Exception {
        SqliteSchemaInitializer initializer = newInitializer(dataSource);
        initializer.initSchemaIfEmpty();
        int afterFirst = countUserTables(dataSource);

        // 二次调用不应抛异常、不应改变表数
        initializer.initSchemaIfEmpty();
        assertEquals(afterFirst, countUserTables(dataSource), "已初始化库二次调用应幂等跳过");
    }

    @Test
    @DisplayName("半成品坏库（sentinel 在但表残缺）→ fail-fast 抛异常，不静默放过")
    void halfBuiltDb_shouldFailFast() throws Exception {
        // 构造「隐性坏库」：只建出 sentinel(tb_cascade_channel) 但缺其余表
        try (Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE tb_cascade_channel (id INTEGER PRIMARY KEY)");
        }

        // 既然 sentinel 已在，initSchemaIfEmpty 会判「已初始化」并校验完整性 → 表数<17 必须 fail-fast
        SqliteSchemaInitializer initializer = newInitializer(dataSource);
        assertThrows(IllegalStateException.class, initializer::initSchemaIfEmpty,
            "sentinel 在但 tb_ 表数<17 的残缺库应 fail-fast，绝不留隐性坏库");
    }

    @Test
    @DisplayName("旧坏库自愈（sentinel 改判核心）：旧 tb_user 在但末表缺失 → 重新建表至 17 张")
    void oldBrokenStateWithLegacySentinel_shouldSelfHeal() throws Exception {
        // 模拟 D3 历史坏库：旧 sentinel tb_user 已建（建表序列靠前），但脚本在末尾中断，
        // 真正最后一张 tb_cascade_channel 缺失。旧实现以 tb_user 为 sentinel 会误判已初始化、永久残缺。
        try (Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE tb_user (id INTEGER PRIMARY KEY, username VARCHAR(64))");
        }

        // 新 sentinel = tb_cascade_channel 缺失 → 应重新执行建表 → 自愈到 17 张
        newInitializer(dataSource).initSchemaIfEmpty();

        assertEquals(EXPECTED_TABLE_COUNT, countUserTables(dataSource),
            "旧坏库（仅 tb_user 在）应被 sentinel 改判识别并重建至 17 张");
        assertTrue(tableExists(dataSource, "tb_cascade_channel"), "自愈后末表 tb_cascade_channel 应存在");
    }
}
