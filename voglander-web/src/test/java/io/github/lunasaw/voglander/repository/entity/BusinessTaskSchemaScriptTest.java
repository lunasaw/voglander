package io.github.lunasaw.voglander.repository.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Durable business-task schema contracts shared by all supported dialects. */
@DisplayName("Durable business-task schema scripts")
class BusinessTaskSchemaScriptTest {

    private static final List<String> TASK_FIELDS = Arrays.asList(
        "id", "create_time", "update_time", "task_id", "task_type", "task_name", "description",
        "task_mode", "schedule_start_time", "schedule_end_time", "interval_seconds", "next_plan_time",
        "schedule_version", "state", "priority", "last_execution_id", "last_execute_time", "completed_time",
        "planned_count", "success_count", "failed_count", "missed_count", "cancelled_count", "progress_current",
        "progress_total", "progress_message", "progress_revision", "biz_key", "subject_type", "subject_id",
        "payload", "payload_version", "result_ref_type", "result_ref_id", "result_summary", "last_failure_code",
        "last_failure_message", "origin_task_id", "origin_execution_id", "owner_type", "owner_id",
        "organization_id", "idempotency_key", "version");

    private static final List<String> EXECUTION_FIELDS = Arrays.asList(
        "id", "create_time", "update_time", "execution_id", "task_id", "schedule_version", "planned_at",
        "deadline_at", "state", "attempt_count", "max_attempts", "next_attempt_time", "started_at", "heartbeat_at",
        "finished_at", "claim_token", "worker_node", "lease_until", "progress_current", "progress_total",
        "progress_message", "progress_revision", "result_ref_type", "result_ref_id", "result_summary", "failure_code",
        "failure_message", "retryable", "retry_origin_execution_id", "version");

    private static final List<String> EVENT_FIELDS = Arrays.asList(
        "id", "create_time", "event_id", "task_id", "execution_id", "event_type", "from_state", "to_state",
        "attempt_no", "progress_current", "progress_total", "progress_message", "failure_code", "failure_message",
        "actor_type", "actor_id", "worker_node", "trace_id", "dedupe_key", "event_data", "occurred_at");

    private static final List<String> REQUIRED_INDEXES = Arrays.asList(
        "uk_biz_task_task_id", "uk_biz_task_idempotency", "idx_biz_task_due", "idx_biz_task_type_state",
        "idx_biz_task_owner", "idx_biz_task_biz_key", "idx_biz_task_subject", "uk_biz_task_execution_id",
        "uk_biz_task_execution_plan", "idx_biz_task_execution_task", "idx_biz_task_execution_pending",
        "idx_biz_task_execution_lease", "idx_biz_task_execution_retry_origin", "uk_biz_task_event_id",
        "uk_biz_task_event_dedupe", "idx_biz_task_event_task", "idx_biz_task_event_execution",
        "idx_biz_task_event_type");

    static Stream<Arguments> schemaScripts() {
        return Stream.of(
            Arguments.of("mysql-full", "sql/voglander.sql"),
            Arguments.of("sqlite-full", "sql/voglander-sqlite.sql"),
            Arguments.of("postgresql-full", "sql/voglander-postgresql.sql"),
            Arguments.of("mysql-incremental", "sql/migration/mysql/1.0.9-durable-business-task-engine.sql"),
            Arguments.of("sqlite-incremental", "sql/migration/sqlite/1.0.9-durable-business-task-engine.sql"),
            Arguments.of("postgresql-incremental", "sql/migration/postgresql/1.0.9-durable-business-task-engine.sql"));
    }

    static Stream<Arguments> fullSchemas() {
        return Stream.of(
            Arguments.of("mysql", "sql/voglander.sql", "bigint unsigned", "datetime"),
            Arguments.of("sqlite", "sql/voglander-sqlite.sql", "integer primary key autoincrement", "datetime"),
            Arguments.of("postgresql", "sql/voglander-postgresql.sql", "bigserial", "timestamp"));
    }

    static Stream<Arguments> incrementalScripts() {
        return Stream.of(
            Arguments.of("mysql", "sql/migration/mysql/1.0.9-durable-business-task-engine.sql"),
            Arguments.of("sqlite", "sql/migration/sqlite/1.0.9-durable-business-task-engine.sql"),
            Arguments.of("postgresql", "sql/migration/postgresql/1.0.9-durable-business-task-engine.sql"));
    }

    static Stream<Arguments> validationScripts() {
        return Stream.of(
            Arguments.of("mysql", "sql/migration/mysql/1.0.9-durable-business-task-engine-validation.sql",
                "information_schema"),
            Arguments.of("sqlite", "sql/migration/sqlite/1.0.9-durable-business-task-engine-validation.sql",
                "sqlite_master"),
            Arguments.of("postgresql", "sql/migration/postgresql/1.0.9-durable-business-task-engine-validation.sql",
                "pg_indexes"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("schemaScripts")
    @DisplayName("全量和增量脚本包含完整三表、字段与索引")
    void schemaScripts_shouldContainCompleteTablesAndIndexes(String dialect, String path) throws IOException {
        String sql = readProjectFile(path);

        assertTableFields(sql, path, "tb_biz_task", TASK_FIELDS);
        assertTableFields(sql, path, "tb_biz_task_execution", EXECUTION_FIELDS);
        assertTableFields(sql, path, "tb_biz_task_event", EVENT_FIELDS);
        for (String index : REQUIRED_INDEXES) {
            assertContainsWord(sql, index, path + " 缺少索引或唯一键 " + index);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fullSchemas")
    @DisplayName("全量脚本使用数据库原生主键和时间类型")
    void fullSchemas_shouldUseNativeTypes(String dialect, String path, String idType, String timeType)
        throws IOException {
        String sql = normalize(readProjectFile(path));

        assertTrue(sql.contains(idType), path + " 缺少原生主键类型 " + idType);
        assertTrue(sql.contains(timeType), path + " 缺少原生时间类型 " + timeType);
        if ("postgresql".equals(dialect)) {
            assertFalse(sql.contains("auto_increment"), path + " PostgreSQL 脚本不得使用 AUTO_INCREMENT");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("incrementalScripts")
    @DisplayName("核心表增量迁移可重复且不删除已有业务任务表")
    void incrementalScripts_shouldBeRepeatableAndNonDestructive(String dialect, String path) throws IOException {
        String sql = normalize(readProjectFile(path));

        for (String table : Arrays.asList("tb_biz_task", "tb_biz_task_execution", "tb_biz_task_event")) {
            assertTrue(sql.contains("create table if not exists " + table),
                path + " 必须用 CREATE TABLE IF NOT EXISTS 创建 " + table);
            assertFalse(Pattern.compile("drop\\s+table(?:\\s+if\\s+exists)?\\s+" + table).matcher(sql).find(),
                path + " 不得删除已有核心表 " + table);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validationScripts")
    @DisplayName("三方言提供可执行的核心表与索引验证 SQL")
    void validationScripts_shouldCoverAllRequiredObjects(String dialect, String path, String catalog)
        throws IOException {
        String sql = normalize(readProjectFile(path));

        assertTrue(sql.contains("select"), path + " 缺少验证查询");
        assertTrue(sql.contains(catalog), path + " 未使用原生系统目录 " + catalog);
        for (String table : Arrays.asList("tb_biz_task", "tb_biz_task_execution", "tb_biz_task_event")) {
            assertContainsWord(sql, table, path + " 缺少表验证 " + table);
        }
        for (String index : REQUIRED_INDEXES) {
            assertContainsWord(sql, index, path + " 缺少索引验证 " + index);
        }
    }

    private void assertTableFields(String sql, String path, String table, List<String> fields) {
        String normalized = normalize(sql);
        Pattern pattern = Pattern.compile("create\\s+table\\s+(?:if\\s+not\\s+exists\\s+)?" + table
            + "\\s*\\((.*?);", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(normalized);
        assertTrue(matcher.find(), path + " 缺少 CREATE TABLE " + table);
        String tableDefinition = matcher.group(1);
        for (String field : fields) {
            assertContainsWord(tableDefinition, field, path + " 的 " + table + " 缺少字段 " + field);
        }
    }

    private void assertContainsWord(String text, String expected, String message) {
        assertTrue(Pattern.compile("\\b" + Pattern.quote(expected) + "\\b").matcher(normalize(text)).find(), message);
    }

    private String normalize(String sql) {
        return sql.toLowerCase(Locale.ROOT).replace("`", "").replace("\"", "");
    }

    private String readProjectFile(String relativePath) throws IOException {
        Path file = projectRoot().resolve(relativePath);
        assertTrue(Files.isRegularFile(file), "缺少 SQL 文件 " + relativePath);
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private Path projectRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("sql/voglander-sqlite.sql"))) {
            current = current.getParent();
        }
        assertTrue(current != null, "无法定位 voglander 项目根目录");
        return current;
    }
}
