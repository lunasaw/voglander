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
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.annotation.DbType;

import lombok.extern.slf4j.Slf4j;

/**
 * Applies the non-destructive SQLite migration for the 1.0.9 image domain.
 *
 * <p>The migration and its history insert share one JDBC transaction. SQLite DDL is transactional, therefore a
 * failed statement leaves neither partial image tables nor a completed history row.</p>
 */
@Slf4j
@Component
public class SqliteImageSchemaMigrator {

    public static final String MIGRATION_KEY = "1.0.9-image-asset-collection";

    private static final String DEFAULT_MIGRATION_SCRIPT =
        "db/migration/sqlite/1.0.9-image-asset-collection.sql";

    private static final List<String> REQUIRED_TABLES = Arrays.asList(
        "tb_image_asset",
        "tb_image_asset_source",
        "tb_image_collection_config");

    private static final List<String> REQUIRED_INDEXES = Arrays.asList(
        "uk_image_asset_asset_id",
        "uk_image_asset_source_execution",
        "uk_image_collection_config_task",
        "idx_image_collection_config_camera");

    @Autowired(required = false)
    private DataSource dataSource;

    private String migrationScript = DEFAULT_MIGRATION_SCRIPT;

    /** Apply the migration once and verify the resulting schema on every startup. */
    public void migrate() {
        if (dataSource == null || DatabaseTypeDetector.detectDbType(dataSource) != DbType.SQLITE) {
            return;
        }

        ensureHistoryTable();
        requireCoreMigration();
        if (isApplied()) {
            // The change was redesigned before release from image-specific task/execution tables to
            // the generic task engine plus tb_image_collection_config. Developer databases may
            // therefore carry the final migration key with the obsolete pre-release footprint.
            // Replaying this additive, IF-NOT-EXISTS script repairs missing final objects without
            // deleting or rewriting any business data.
            applyMigration(false);
            return;
        }

        applyMigration(true);
    }

    private void applyMigration(boolean recordHistory) {

        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(new ClassPathResource(migrationScript));
                populator.setContinueOnError(false);
                populator.populate(connection);
                verifySchema(connection);
                if (recordHistory) {
                    try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO schema_migration_history(migration_key, applied_at) VALUES(?, CURRENT_TIMESTAMP)")) {
                        statement.setString(1, MIGRATION_KEY);
                        statement.executeUpdate();
                    }
                }
                connection.commit();
                if (recordHistory) {
                    log.info("SQLite migration {} applied", MIGRATION_KEY);
                } else {
                    log.debug("SQLite migration {} verified by idempotent replay", MIGRATION_KEY);
                }
            } catch (Exception e) {
                rollback(connection, e);
                throw new IllegalStateException("SQLite migration " + MIGRATION_KEY + " failed", e);
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("SQLite migration " + MIGRATION_KEY + " failed", e);
        }
    }

    void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void ensureHistoryTable() {
        String sql = "CREATE TABLE IF NOT EXISTS schema_migration_history ("
            + "migration_key VARCHAR(128) PRIMARY KEY, applied_at DATETIME NOT NULL)";
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot initialize SQLite migration history", e);
        }
    }

    private boolean isApplied() {
        String sql = "SELECT 1 FROM schema_migration_history WHERE migration_key=?";
        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, MIGRATION_KEY);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read SQLite migration history", e);
        }
    }

    private void requireCoreMigration() {
        String sql = "SELECT 1 FROM schema_migration_history WHERE migration_key=?";
        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, SqliteBusinessTaskSchemaMigrator.MIGRATION_KEY);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Image schema requires durable business-task migration first");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot verify durable business-task migration", e);
        }
    }

    private void verifySchema() {
        try (Connection connection = dataSource.getConnection()) {
            verifySchema(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot verify SQLite image schema", e);
        }
    }

    private void verifySchema(Connection connection) throws SQLException {
        for (String table : REQUIRED_TABLES) {
            verifyObjectExists(connection, "table", table);
        }
        for (String index : REQUIRED_INDEXES) {
            verifyObjectExists(connection, "index", index);
        }
    }

    private void verifyObjectExists(Connection connection, String type, String name) throws SQLException {
        String sql = "SELECT 1 FROM sqlite_master WHERE type=? AND name=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type);
            statement.setString(2, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("SQLite image schema missing " + type + " " + name);
                }
            }
        }
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }
}
