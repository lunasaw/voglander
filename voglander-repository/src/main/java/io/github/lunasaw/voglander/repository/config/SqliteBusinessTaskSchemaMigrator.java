package io.github.lunasaw.voglander.repository.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.annotation.DbType;

import lombok.extern.slf4j.Slf4j;

/** Applies and verifies the additive SQLite durable business-task schema migration. */
@Slf4j
@Component
public class SqliteBusinessTaskSchemaMigrator {

    public static final String MIGRATION_KEY = "add-durable-business-task-engine";
    private static final String DEFAULT_SCRIPT = "db/migration/sqlite/1.0.9-durable-business-task-engine.sql";
    private static final List<String> REQUIRED_TABLES = Arrays.asList(
        "tb_biz_task", "tb_biz_task_execution", "tb_biz_task_event");
    private static final List<String> REQUIRED_INDEXES = Arrays.asList(
        "uk_biz_task_task_id", "uk_biz_task_execution_plan", "uk_biz_task_event_id",
        "idx_biz_task_due", "idx_biz_task_execution_pending", "idx_biz_task_execution_lease");

    @Autowired(required = false)
    private DataSource dataSource;
    private String migrationScript = DEFAULT_SCRIPT;
    @Value("${voglander.task.legacy-export-drop-enabled:false}")
    private boolean authorizedLegacyExportDrop;

    public void migrate() {
        if (dataSource == null || DatabaseTypeDetector.detectDbType(dataSource) != DbType.SQLITE) {
            return;
        }
        ensureHistoryTable();
        if (isApplied()) {
            // A database may have been marked by an earlier additive build before
            // the legacy table-removal gate was deployed. Re-run the guarded,
            // idempotent removal on every startup so the breaking change cannot
            // silently leave the old table behind.
            try (Connection connection = dataSource.getConnection()) {
                dropLegacyExportTaskWhenAuthorized(connection);
                verifySchema(connection);
            } catch (SQLException e) {
                throw new IllegalStateException("Cannot verify SQLite business-task schema", e);
            }
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                dropLegacyExportTaskWhenAuthorized(connection);
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(new ClassPathResource(migrationScript));
                populator.setContinueOnError(false);
                populator.populate(connection);
                verifySchema(connection);
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO schema_migration_history(migration_key,applied_at) VALUES(?,CURRENT_TIMESTAMP)")) {
                    statement.setString(1, MIGRATION_KEY);
                    statement.executeUpdate();
                }
                connection.commit();
                log.info("SQLite migration {} applied", MIGRATION_KEY);
            } catch (Exception e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw new IllegalStateException("SQLite migration " + MIGRATION_KEY + " failed", e);
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("SQLite migration " + MIGRATION_KEY + " failed", e);
        }
    }

    void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    void setAuthorizedLegacyExportDrop(boolean authorizedLegacyExportDrop) {
        this.authorizedLegacyExportDrop = authorizedLegacyExportDrop;
    }

    private void dropLegacyExportTaskWhenAuthorized(Connection connection) throws SQLException {
        if (!objectExists(connection, "table", "tb_export_task")) {
            return;
        }
        int count;
        try (Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM tb_export_task")) {
            resultSet.next();
            count = resultSet.getInt(1);
        }
        if (count > 0 && !authorizedLegacyExportDrop) {
            throw new IllegalStateException("tb_export_task contains " + count
                + " rows; run scripts/preflight-legacy-export-task.sh to create a backup and explicitly "
                + "authorize destructive removal");
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE tb_export_task");
        }
    }

    private void ensureHistoryTable() {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS schema_migration_history ("
                + "migration_key VARCHAR(128) PRIMARY KEY, applied_at DATETIME NOT NULL)");
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot initialize SQLite migration history", e);
        }
    }

    private boolean isApplied() {
        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM schema_migration_history WHERE migration_key=?")) {
            statement.setString(1, MIGRATION_KEY);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read SQLite migration history", e);
        }
    }

    private void verifySchema() {
        try (Connection connection = dataSource.getConnection()) {
            verifySchema(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot verify SQLite business-task schema", e);
        }
    }

    private void verifySchema(Connection connection) throws SQLException {
        for (String table : REQUIRED_TABLES) {
            verifyObject(connection, "table", table);
        }
        for (String index : REQUIRED_INDEXES) {
            verifyObject(connection, "index", index);
        }
    }

    private void verifyObject(Connection connection, String type, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT 1 FROM sqlite_master WHERE type=? AND name=?")) {
            statement.setString(1, type);
            statement.setString(2, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("SQLite task schema missing " + type + " " + name);
                }
            }
        }
    }

    private boolean objectExists(Connection connection, String type, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT 1 FROM sqlite_master WHERE type=? AND name=?")) {
            statement.setString(1, type);
            statement.setString(2, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
