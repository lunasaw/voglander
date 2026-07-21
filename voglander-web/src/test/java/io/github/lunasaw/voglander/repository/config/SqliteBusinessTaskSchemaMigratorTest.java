package io.github.lunasaw.voglander.repository.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("SQLite 1.0.9 通用业务任务增量迁移")
class SqliteBusinessTaskSchemaMigratorTest {

    private Path dbFile;
    private DataSource dataSource;

    @BeforeEach
    void setUp() throws IOException {
        dbFile = Files.createTempFile("voglander-task-migration-", ".db");
        Files.deleteIfExists(dbFile);
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setDriverClassName("org.sqlite.JDBC");
        source.setUrl("jdbc:sqlite:" + dbFile.toAbsolutePath());
        dataSource = source;
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(dbFile);
        Files.deleteIfExists(Path.of(dbFile.toAbsolutePath() + "-wal"));
        Files.deleteIfExists(Path.of(dbFile.toAbsolutePath() + "-shm"));
    }

    @Test
    @DisplayName("旧 21 表数据库迁移为 23 表并移除空 export 表")
    void legacyDatabase_shouldUpgradeToTaskSchema() throws Exception {
        prepareLegacyDatabase(false);
        assertEquals(21, countBusinessTables());

        migrator().migrate();

        assertEquals(23, countBusinessTables());
        assertFalse(tableExists("tb_export_task"));
        assertTrue(tableExists("tb_biz_task"));
        assertTrue(tableExists("tb_biz_task_execution"));
        assertTrue(tableExists("tb_biz_task_event"));
        assertTrue(indexExists("idx_biz_task_execution_lease"));
        assertEquals(1, countMigrationHistory());
    }

    @Test
    @DisplayName("重复启动只登记一次 migration 且不改变 schema")
    void repeatedStartup_shouldBeIdempotent() throws Exception {
        prepareLegacyDatabase(false);
        SqliteBusinessTaskSchemaMigrator migrator = migrator();

        migrator.migrate();
        migrator.migrate();

        assertEquals(23, countBusinessTables());
        assertEquals(1, countMigrationHistory());
    }

    @Test
    @DisplayName("迁移中途失败时 DDL、旧表删除和 history 一并回滚")
    void partialFailure_shouldRollback() throws Exception {
        prepareLegacyDatabase(false);
        SqliteBusinessTaskSchemaMigrator migrator = migrator();
        ReflectionTestUtils.setField(migrator, "migrationScript", "db/migration/sqlite/broken-task-migration.sql");

        assertThrows(IllegalStateException.class, migrator::migrate);

        assertTrue(tableExists("tb_export_task"));
        assertFalse(tableExists("tb_biz_task"));
        assertEquals(0, countMigrationHistory());
    }

    @Test
    @DisplayName("增量升级保留既有设备业务数据")
    void migration_shouldPreserveExistingBusinessData() throws Exception {
        prepareLegacyDatabase(false);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO tb_device(device_id,ip,port,server_ip) "
                + "VALUES('task-migration-device','127.0.0.1',5060,'127.0.0.1')");
        }

        migrator().migrate();

        assertEquals(1, scalar("SELECT count(*) FROM tb_device WHERE device_id='task-migration-device'"));
    }

    @Test
    @DisplayName("非空 export 表且未授权时 fail-fast 并保留全部旧数据")
    void nonEmptyExportTable_withoutAuthorization_shouldFailFast() throws Exception {
        prepareLegacyDatabase(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> migrator().migrate());

        assertTrue(exception.getMessage().contains(SqliteBusinessTaskSchemaMigrator.MIGRATION_KEY));
        assertEquals(1, scalar("SELECT count(*) FROM tb_export_task"));
        assertFalse(tableExists("tb_biz_task"));
        assertEquals(0, countMigrationHistory());
    }

    @Test
    @DisplayName("显式授权后允许删除非空 export 表")
    void nonEmptyExportTable_withAuthorization_shouldDropAndMigrate() throws Exception {
        prepareLegacyDatabase(true);
        SqliteBusinessTaskSchemaMigrator migrator = migrator();
        migrator.setAuthorizedLegacyExportDrop(true);

        migrator.migrate();

        assertFalse(tableExists("tb_export_task"));
        assertTrue(tableExists("tb_biz_task"));
        assertEquals(1, countMigrationHistory());
    }

    @Test
    @DisplayName("没有 legacy export 表时仍可正常迁移")
    void missingExportTable_shouldMigrate() throws Exception {
        prepareLegacyDatabase(false);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE tb_export_task");
        }

        migrator().migrate();

        assertTrue(tableExists("tb_biz_task"));
        assertEquals(1, countMigrationHistory());
    }

    @Test
    @DisplayName("可空幂等键允许多行 NULL，但拒绝同 owner/type 的重复非空值")
    void nullableIdempotencyKey_shouldKeepSqliteUniqueSemantics() throws Exception {
        prepareLegacyDatabase(false);
        migrator().migrate();

        insertTask("btask_null_1", null);
        insertTask("btask_null_2", null);
        insertTask("btask_key_1", "same-key");

        assertEquals(2, scalar("SELECT count(*) FROM tb_biz_task WHERE idempotency_key IS NULL"));
        assertThrows(Exception.class, () -> insertTask("btask_key_2", "same-key"));
    }

    @Test
    @DisplayName("SQLite 验证 SQL 应返回全部 21 个核心对象且均存在")
    void validationSql_shouldConfirmAllRequiredObjects() throws Exception {
        prepareLegacyDatabase(false);
        migrator().migrate();
        String validationSql = new ClassPathResource(
            "db/migration/sqlite/1.0.9-durable-business-task-engine-validation.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        try (Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(validationSql)) {
            int count = 0;
            while (resultSet.next()) {
                assertEquals(1, resultSet.getInt("object_exists"),
                    resultSet.getString("object_type") + " " + resultSet.getString("object_name") + " 缺失");
                count++;
            }
            assertEquals(21, count);
        }
    }

    @Test
    @DisplayName("migration key 已存在但核心索引缺失时应 fail-fast")
    void appliedMigrationWithBrokenCoreSchema_shouldFailFast() throws Exception {
        prepareLegacyDatabase(false);
        SqliteBusinessTaskSchemaMigrator migrator = migrator();
        migrator.migrate();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP INDEX idx_biz_task_execution_lease");
        }

        IllegalStateException exception = assertThrows(IllegalStateException.class, migrator::migrate);

        assertTrue(exception.getMessage().contains("idx_biz_task_execution_lease"));
        assertEquals(1, countMigrationHistory());
    }

    private SqliteBusinessTaskSchemaMigrator migrator() {
        SqliteBusinessTaskSchemaMigrator migrator = new SqliteBusinessTaskSchemaMigrator();
        migrator.setDataSource(dataSource);
        return migrator;
    }

    private void prepareLegacyDatabase(boolean withExportRow) throws Exception {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/voglander-sqlite.sql"));
        try (Connection connection = dataSource.getConnection()) {
            populator.populate(connection);
            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP TABLE tb_image_collection_config");
                statement.execute("DROP TABLE tb_image_asset_source");
                statement.execute("DROP TABLE tb_image_asset");
                statement.execute("DROP TABLE tb_biz_task_event");
                statement.execute("DROP TABLE tb_biz_task_execution");
                statement.execute("DROP TABLE tb_biz_task");
                statement.execute("CREATE TABLE tb_export_task (id INTEGER PRIMARY KEY AUTOINCREMENT, biz_id VARCHAR(255))");
                if (withExportRow) {
                    statement.execute("INSERT INTO tb_export_task(biz_id) VALUES('must-back-up')");
                }
            }
        }
    }

    private int countBusinessTables() throws Exception {
        return scalar("SELECT count(*) FROM sqlite_master WHERE type='table' AND name LIKE 'tb_%'");
    }

    private int countMigrationHistory() throws Exception {
        if (!tableExists("schema_migration_history")) {
            return 0;
        }
        return scalar("SELECT count(*) FROM schema_migration_history WHERE migration_key='"
            + SqliteBusinessTaskSchemaMigrator.MIGRATION_KEY + "'");
    }

    private boolean tableExists(String name) throws Exception {
        return scalar("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='" + name + "'") == 1;
    }

    private boolean indexExists(String name) throws Exception {
        return scalar("SELECT count(*) FROM sqlite_master WHERE type='index' AND name='" + name + "'") == 1;
    }

    private int scalar(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private void insertTask(String taskId, String idempotencyKey) throws Exception {
        try (Connection connection = dataSource.getConnection();
            java.sql.PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tb_biz_task(task_id,task_type,task_name,task_mode,state,payload,payload_version,"
                    + "owner_type,owner_id,idempotency_key) VALUES(?,?,?,?,?,?,?,?,?,?)")) {
            statement.setString(1, taskId);
            statement.setString(2, "TEST_TASK");
            statement.setString(3, "test");
            statement.setString(4, "ONCE");
            statement.setString(5, "SCHEDULED");
            statement.setString(6, "{}");
            statement.setInt(7, 1);
            statement.setString(8, "USER");
            statement.setString(9, "owner-1");
            statement.setString(10, idempotencyKey);
            statement.executeUpdate();
        }
    }
}
