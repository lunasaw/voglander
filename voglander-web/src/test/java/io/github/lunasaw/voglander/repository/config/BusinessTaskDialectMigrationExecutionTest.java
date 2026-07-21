package io.github.lunasaw.voglander.repository.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** Executes non-SQLite migrations in H2 dialect modes as a fast syntax and constraint gate. */
@DisplayName("Durable business-task non-SQLite migration execution")
class BusinessTaskDialectMigrationExecutionTest {

    private static final List<String> TABLES = Arrays.asList(
        "tb_biz_task", "tb_biz_task_execution", "tb_biz_task_event");

    private static final List<String> INDEXES = Arrays.asList(
        "uk_biz_task_task_id", "uk_biz_task_idempotency", "idx_biz_task_due", "idx_biz_task_type_state",
        "idx_biz_task_owner", "idx_biz_task_biz_key", "idx_biz_task_subject", "uk_biz_task_execution_id",
        "uk_biz_task_execution_plan", "idx_biz_task_execution_task", "idx_biz_task_execution_pending",
        "idx_biz_task_execution_lease", "idx_biz_task_execution_retry_origin", "uk_biz_task_event_id",
        "uk_biz_task_event_dedupe", "idx_biz_task_event_task", "idx_biz_task_event_execution",
        "idx_biz_task_event_type");

    static Stream<Arguments> dialects() {
        return Stream.of(
            Arguments.of("mysql", "MySQL"),
            Arguments.of("postgresql", "PostgreSQL"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dialects")
    @DisplayName("空库执行和重复执行均保留完整三表、索引与可空唯一语义")
    void migration_shouldExecuteFromFreshDatabaseAndRemainRepeatable(String dialect, String h2Mode) throws Exception {
        String url = "jdbc:h2:mem:task_" + dialect + ";MODE=" + h2Mode
            + ";DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Path migration = projectRoot().resolve(
            "sql/migration/" + dialect + "/1.0.9-durable-business-task-engine.sql");

        try (Connection connection = DriverManager.getConnection(url)) {
            createPermissionTables(connection);
            execute(connection, migration);
            execute(connection, migration);

            for (String table : TABLES) {
                assertEquals(1, scalar(connection,
                    "SELECT count(*) FROM information_schema.tables WHERE lower(table_name)='" + table + "'"),
                    dialect + " migration 缺少表 " + table);
            }
            for (String index : INDEXES) {
                assertEquals(1, objectExists(connection, index), dialect + " migration 缺少索引或唯一键 " + index);
            }

            insertTask(connection, dialect + "_null_1", null);
            insertTask(connection, dialect + "_null_2", null);
            insertTask(connection, dialect + "_key_1", "same-key");
            assertThrows(SQLException.class,
                () -> insertTask(connection, dialect + "_key_2", "same-key"));
        }
    }

    /**
     * Incremental migrations run against an already initialized application schema.  The
     * focused dialect test creates only the two base permission tables needed to verify
     * that the migration also installs the task-center menu and administrator grants.
     */
    private void createPermissionTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE tb_menu ("
                + "id BIGINT PRIMARY KEY, parent_id BIGINT NOT NULL, menu_code VARCHAR(64) NOT NULL,"
                + "menu_name VARCHAR(255) NOT NULL, menu_type INTEGER NOT NULL, path VARCHAR(255),"
                + "component VARCHAR(255), icon VARCHAR(255), sort_order INTEGER NOT NULL, status INTEGER NOT NULL,"
                + "permission VARCHAR(255), meta VARCHAR(2048))");
            statement.execute("CREATE TABLE tb_role_menu ("
                + "role_id BIGINT NOT NULL, menu_id BIGINT NOT NULL, PRIMARY KEY (role_id, menu_id))");
        }
    }

    private void execute(Connection connection, Path migration) {
        ScriptUtils.executeSqlScript(connection,
            new EncodedResource(new FileSystemResource(migration), StandardCharsets.UTF_8));
    }

    private int scalar(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private int objectExists(Connection connection, String name) throws SQLException {
        String sql = "SELECT count(*) FROM ("
            + "SELECT lower(index_name) object_name FROM information_schema.indexes "
            + "UNION SELECT lower(constraint_name) object_name FROM information_schema.table_constraints"
            + ") objects WHERE object_name='" + name + "'";
        return scalar(connection, sql) > 0 ? 1 : 0;
    }

    private void insertTask(Connection connection, String taskId, String idempotencyKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
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

    private Path projectRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("sql/voglander-sqlite.sql"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("Cannot locate voglander project root");
        }
        return current;
    }
}
