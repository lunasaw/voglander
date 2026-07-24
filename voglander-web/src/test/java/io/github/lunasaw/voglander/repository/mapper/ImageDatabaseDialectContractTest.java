package io.github.lunasaw.voglander.repository.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

class ImageDatabaseDialectContractTest {

    @Test
    void insertIfAbsentMappersDeclareAllThreeDatabaseVendors() throws Exception {
        for (String mapper : List.of("BizTaskMapper.xml", "BizTaskExecutionMapper.xml", "ImageAssetMapper.xml",
            "ImageAssetSourceMapper.xml", "ImageCollectionConfigMapper.xml")) {
            String xml = read("voglander-repository/src/main/resources/mapper/" + mapper);
            assertContains(xml, mapper, "databaseId=\"sqlite\"", "INSERT OR IGNORE");
            assertContains(xml, mapper, "databaseId=\"mysql\"", "INSERT IGNORE");
            assertContains(xml, mapper, "databaseId=\"postgresql\"", "ON CONFLICT DO NOTHING");
        }
    }

    @Test
    void joinedQueriesFilterBeforePaginationAndKeepStableOrdering() throws Exception {
        String assets = read("voglander-repository/src/main/resources/mapper/ImageAssetMapper.xml");
        assertContains(assets, "ImageAssetMapper.xml", "LEFT JOIN tb_image_asset_source",
            "ORDER BY a.captured_at DESC,a.asset_id DESC");
        for (String vendor : List.of("sqlite", "mysql", "postgresql")) {
            assertTrue(assets.contains("databaseId=\"" + vendor + "\""), vendor);
        }

        String collections = read(
            "voglander-repository/src/main/resources/mapper/ImageCollectionTaskReadMapper.xml");
        assertContains(collections, "ImageCollectionTaskReadMapper.xml",
            "INNER JOIN tb_image_collection_config", "AND c.device_id=#{condition.deviceId}",
            "AND c.channel_id=#{condition.channelId}", "ORDER BY t.create_time DESC,t.task_id DESC");
    }

    @Test
    void mysqlPostgresqlAndSqliteSchemasKeepRequiredUniqueAndQueryIndexes() throws Exception {
        for (String script : List.of("sql/voglander.sql", "sql/voglander-postgresql.sql",
            "sql/voglander-sqlite.sql")) {
            String sql = read(script).toLowerCase();
            assertContains(sql, script, "uk_biz_task_idempotency", "uk_image_asset_idempotency",
                "uk_image_collection_config_task", "idx_image_collection_config_camera");
        }
    }

    private void assertContains(String content, String source, String... required) {
        for (String value : required) {
            assertTrue(content.contains(value), source + " 缺少 " + value);
        }
    }

    private String read(String relativePath) throws Exception {
        return Files.readString(projectRoot().resolve(relativePath));
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
