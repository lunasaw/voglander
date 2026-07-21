package io.github.lunasaw.voglander.repository.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("SQLite 1.0.9 图像域增量迁移")
class SqliteImageSchemaMigratorTest {

    private Path dbFile;
    private DataSource dataSource;

    @BeforeEach
    void setUp() throws IOException {
        dbFile = Files.createTempFile("voglander-image-migration-", ".db");
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
    @DisplayName("旧 21 表 app.db 启动后迁移为通用任务加图像三表")
    void legacyDatabase_shouldUpgradeToImageSchema() throws Exception {
        prepareLegacyDatabase();
        assertEquals(21, countBusinessTables());

        newInitializer().initSchemaIfEmpty();

        assertEquals(26, countBusinessTables());
        assertTrue(tableExists("tb_image_asset"));
        assertTrue(tableExists("tb_biz_task_execution"));
        assertTrue(indexExists("idx_image_collection_config_camera"));
        assertEquals(1, countMigrationHistory());
    }

    @Test
    @DisplayName("重复启动只登记一次 migration 且不改变 schema")
    void repeatedStartup_shouldBeIdempotent() throws Exception {
        prepareLegacyDatabase();
        SqliteSchemaInitializer initializer = newInitializer();

        initializer.initSchemaIfEmpty();
        initializer.initSchemaIfEmpty();

        assertEquals(26, countBusinessTables());
        assertEquals(1, countMigrationHistory());
    }

    @Test
    @DisplayName("已登记旧版 migration key 时幂等补齐最终 config 表")
    void appliedPreReleaseFootprint_shouldRepairFinalSchema() throws Exception {
        prepareLegacyDatabase();
        coreMigrator().migrate();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO schema_migration_history(migration_key,applied_at) VALUES('"
                + SqliteImageSchemaMigrator.MIGRATION_KEY + "',CURRENT_TIMESTAMP)");
            statement.execute("CREATE TABLE tb_image_collection_task(id INTEGER PRIMARY KEY)");
            statement.execute("CREATE TABLE tb_image_collection_execution(id INTEGER PRIMARY KEY)");
        }

        imageMigrator().migrate();

        assertTrue(tableExists("tb_image_collection_config"));
        assertTrue(indexExists("idx_image_collection_config_camera"));
        assertEquals(1, countMigrationHistory());
    }

    @Test
    @DisplayName("缺少 durable-task migration key 时拒绝图像迁移")
    void missingCoreMigration_shouldFailFast() throws Exception {
        SqliteImageSchemaMigrator migrator = imageMigrator();

        assertThrows(IllegalStateException.class, migrator::migrate);

        assertFalse(tableExists("tb_image_asset"));
        assertEquals(0, countMigrationHistory());
    }

    @Test
    @DisplayName("迁移中途失败应回滚已创建的图像表且不登记 history")
    void partialFailure_shouldRollback() throws Exception {
        prepareLegacyDatabase();
        coreMigrator().migrate();
        SqliteImageSchemaMigrator migrator = imageMigrator();
        ReflectionTestUtils.setField(migrator, "migrationScript", "db/migration/sqlite/broken-image-migration.sql");

        assertThrows(IllegalStateException.class, migrator::migrate);

        assertFalse(tableExists("tb_image_asset"));
        assertEquals(0, countMigrationHistory());
    }

    @Test
    @DisplayName("增量升级保留既有设备业务数据")
    void migration_shouldPreserveExistingBusinessData() throws Exception {
        prepareLegacyDatabase();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO tb_device(device_id,ip,port,server_ip) "
                + "VALUES('migration-device','127.0.0.1',5060,'127.0.0.1')");
        }

        newInitializer().initSchemaIfEmpty();

        assertEquals(1, scalar("SELECT count(*) FROM tb_device WHERE device_id='migration-device'"));
    }

    private SqliteSchemaInitializer newInitializer() {
        SqliteSchemaInitializer initializer = new SqliteSchemaInitializer();
        ReflectionTestUtils.setField(initializer, "dataSource", dataSource);
        ReflectionTestUtils.setField(initializer, "businessTaskSchemaMigrator", coreMigrator());
        ReflectionTestUtils.setField(initializer, "imageSchemaMigrator", imageMigrator());
        return initializer;
    }

    private SqliteBusinessTaskSchemaMigrator coreMigrator() {
        SqliteBusinessTaskSchemaMigrator migrator = new SqliteBusinessTaskSchemaMigrator();
        ReflectionTestUtils.setField(migrator, "dataSource", dataSource);
        return migrator;
    }

    private SqliteImageSchemaMigrator imageMigrator() {
        SqliteImageSchemaMigrator migrator = new SqliteImageSchemaMigrator();
        ReflectionTestUtils.setField(migrator, "dataSource", dataSource);
        return migrator;
    }

    private void prepareLegacyDatabase() throws Exception {
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
            + SqliteImageSchemaMigrator.MIGRATION_KEY + "'");
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
}
