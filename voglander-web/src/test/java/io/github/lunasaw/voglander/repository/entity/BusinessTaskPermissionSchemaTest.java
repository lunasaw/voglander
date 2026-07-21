package io.github.lunasaw.voglander.repository.entity;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Business-task menu and permission schema")
class BusinessTaskPermissionSchemaTest {

    private static final List<String> SCHEMAS = Arrays.asList(
        "sql/voglander.sql", "sql/voglander-sqlite.sql", "sql/voglander-postgresql.sql",
        "sql/migration/mysql/1.0.9-durable-business-task-engine.sql",
        "sql/migration/sqlite/1.0.9-durable-business-task-engine.sql",
        "sql/migration/postgresql/1.0.9-durable-business-task-engine.sql");

    @Test
    void allSchemas_shouldDefineTaskQueryControlMenusAndAdminGrant() throws IOException {
        for (String file : SCHEMAS) {
            String sql = new String(Files.readAllBytes(projectRoot().resolve(file)), StandardCharsets.UTF_8);
            assertTrue(sql.contains("Task:Query"), file + " 缺少 Task:Query");
            assertTrue(sql.contains("Task:Control"), file + " 缺少 Task:Control");
            assertTrue(sql.contains("60101"), file + " 缺少 TaskQuery 菜单");
            assertTrue(sql.contains("60102"), file + " 缺少 TaskControl 菜单");
            assertTrue(sql.contains("tb_role_menu"), file + " 缺少管理员角色授权");
        }
    }

    private Path projectRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("sql/voglander-sqlite.sql"))) {
            current = current.getParent();
        }
        return current;
    }
}
