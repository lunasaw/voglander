package io.github.lunasaw.voglander.repository.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Legacy ExportTask removal gate")
class LegacyExportTaskRemovalTest {

    static Stream<String> fullSchemas() {
        return Stream.of("sql/voglander.sql", "sql/voglander-sqlite.sql", "sql/voglander-postgresql.sql");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fullSchemas")
    @DisplayName("full schemas no longer create the legacy export table")
    void fullSchema_shouldNotCreateLegacyExportTable(String relativePath) throws IOException {
        String sql = Files.readString(projectRoot().resolve(relativePath), StandardCharsets.UTF_8).toLowerCase();
        assertFalse(sql.contains("tb_export_task"), relativePath + " still contains tb_export_task");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fullSchemas")
    @DisplayName("durable migrations carry the guarded legacy drop marker")
    void durableMigration_shouldContainLegacyDrop(String ignored) throws IOException {
        String dialect = ignored.contains("postgresql") ? "postgresql" : ignored.contains("sqlite") ? "sqlite" : "mysql";
        Path migration = projectRoot().resolve(
            "sql/migration/" + dialect + "/1.0.9-durable-business-task-engine.sql");
        String sql = Files.readString(migration, StandardCharsets.UTF_8).toLowerCase().replace("`", "");
        assertTrue(sql.contains("drop table if exists tb_export_task"), migration + " lacks guarded drop");
    }

    @Test
    @DisplayName("old Java chain and API contract are physically absent")
    void legacyJavaChain_shouldBeDeleted() throws IOException {
        Path root = projectRoot();
        Stream.of(
            "voglander-common/src/main/java/io/github/lunasaw/voglander/common/enums/export/ExportTaskStatusEnum.java",
            "voglander-common/src/main/java/io/github/lunasaw/voglander/common/enums/export/ExportTaskTypeEnums.java",
            "voglander-repository/src/main/java/io/github/lunasaw/voglander/repository/entity/ExportTaskDO.java",
            "voglander-repository/src/main/java/io/github/lunasaw/voglander/repository/mapper/ExportTaskMapper.java",
            "voglander-manager/src/main/java/io/github/lunasaw/voglander/manager/manager/ExportTaskManager.java",
            "voglander-web/src/main/java/io/github/lunasaw/voglander/web/api/export/ExportTaskController.java")
            .forEach(path -> assertFalse(Files.exists(root.resolve(path)), path + " must be deleted"));

        String api = Files.readString(root.resolve("api/voglander-api.md"), StandardCharsets.UTF_8);
        assertFalse(api.contains("/api/v1/exportTask/"), "legacy export routes must leave OpenAPI docs");
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
