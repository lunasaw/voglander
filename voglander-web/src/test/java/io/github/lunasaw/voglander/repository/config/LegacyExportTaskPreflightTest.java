package io.github.lunasaw.voglander.repository.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Legacy export-task destructive migration preflight")
class LegacyExportTaskPreflightTest {

    @Test
    @DisplayName("preflight backs up and counts the legacy table before the guarded drop")
    void preflightScript_shouldBackupCountAndRequireExplicitAuthorization() throws IOException {
        Path root = Paths.get("").toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("sql/voglander-sqlite.sql"))) {
            root = root.getParent();
        }
        Path script = root == null ? Path.of("scripts/preflight-legacy-export-task.sh")
            : root.resolve("scripts/preflight-legacy-export-task.sh");
        assertTrue(Files.exists(script), "release preflight script must be checked in");

        String source = Files.readString(script);
        assertTrue(source.contains("tb_export_task"));
        assertTrue(source.contains(".backup"), "SQLite backup must happen before the count/drop gate");
        assertTrue(source.matches("(?s).*SELECT COUNT\\s*\\(\\s*\\*\\s*\\).*"),
            "preflight must count rows, not only test table existence");
        assertTrue(source.contains("--allow-destructive"));
        assertTrue(source.contains("exit 2"), "non-empty legacy data must stop the release by default");
    }
}
