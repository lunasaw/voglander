package io.github.lunasaw.voglander.repository.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.support.UniqueKeyFactory;

/**
 * Schema 约束契约测试 — DDL 漂移检测器
 * <p>
 * 任何修改 schema-sqlite.sql 的变更必须同步更新此测试
 *
 * @author luna
 */
@DisplayName("Schema 约束契约测试")
@Transactional
class SchemaConstraintTest extends BaseTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final List<String> FULL_SCHEMA_SCRIPTS = Arrays.asList(
        "sql/voglander.sql",
        "sql/voglander-sqlite.sql",
        "sql/voglander-postgresql.sql");

    private static final List<String> IMAGE_TABLES = Arrays.asList(
        "tb_image_asset",
        "tb_image_asset_source",
        "tb_image_collection_config");

    private static final List<String> TASK_TABLES = Arrays.asList(
        "tb_biz_task",
        "tb_biz_task_execution",
        "tb_biz_task_event");

    @Test
    @DisplayName("三种全量脚本应包含通用任务三表与图像领域三表")
    void fullSchemaScripts_shouldContainImageTablesAndConstraints() throws IOException {
        for (String scriptPath : FULL_SCHEMA_SCRIPTS) {
            String sql = readProjectFile(scriptPath).toLowerCase();
            for (String table : IMAGE_TABLES) {
                assertTrue(sql.contains(table), scriptPath + " 缺少表 " + table);
            }
            for (String table : TASK_TABLES) {
                assertTrue(sql.contains(table), scriptPath + " 缺少表 " + table);
            }
            assertTrue(sql.contains("uk_image_asset_asset_id"), scriptPath + " 缺少资产 ID 唯一键");
            assertTrue(sql.contains("uk_image_asset_source_execution"), scriptPath + " 缺少来源 execution 唯一键");
            assertTrue(sql.contains("uk_image_collection_config_task"), scriptPath + " 缺少图像配置 taskId 唯一键");
            assertTrue(sql.contains("uk_biz_task_task_id"), scriptPath + " 缺少通用任务 ID 唯一键");
            assertTrue(sql.contains("uk_biz_task_execution_plan"), scriptPath + " 缺少通用计划点唯一键");
            assertTrue(sql.contains("idx_biz_task_due"), scriptPath + " 缺少到期任务扫描索引");
            assertTrue(sql.contains("idx_biz_task_execution_pending"), scriptPath + " 缺少待执行扫描索引");
            assertTrue(sql.contains("idx_biz_task_execution_lease"), scriptPath + " 缺少租约扫描索引");
            assertFalse(sql.contains("create table tb_image_collection_task"), scriptPath + " 不得保留专属图像任务表");
            assertFalse(sql.contains("create table tb_image_collection_execution"), scriptPath + " 不得保留专属图像执行表");
        }
    }

    @Test
    @DisplayName("三种全量脚本应包含 700 段菜单、七项按钮权限与管理员授权")
    void fullSchemaScripts_shouldContainImageMenusAndPermissions() throws IOException {
        List<String> permissions = Arrays.asList(
            "Image:Asset:Query",
            "Image:Asset:View",
            "Image:Asset:Upload",
            "Image:Asset:Download",
            "Image:Asset:Delete",
            "Image:Collection:Query",
            "Image:Collection:Create",
            "Image:Collection:Control");
        for (String scriptPath : FULL_SCHEMA_SCRIPTS) {
            String sql = readProjectFile(scriptPath);
            assertTrue(sql.contains("(700,"), scriptPath + " 缺少图像管理目录菜单");
            assertTrue(sql.contains("(701,"), scriptPath + " 缺少图像资产菜单");
            assertTrue(sql.contains("(702,"), scriptPath + " 缺少图像采集菜单");
            assertTrue(sql.contains("/image/assets"), scriptPath + " 缺少图像资产路由");
            assertTrue(sql.contains("/image/collection"), scriptPath + " 缺少图像采集路由");
            for (String permission : permissions) {
                assertTrue(sql.contains(permission), scriptPath + " 缺少权限 " + permission);
            }
            assertTrue(sql.contains("tb_role_menu"), scriptPath + " 缺少管理员菜单授权");
        }
    }

    private Path projectRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("sql/voglander-sqlite.sql"))) {
            current = current.getParent();
        }
        assertNotNull(current, "无法定位 voglander 项目根目录");
        return current;
    }

    private String readProjectFile(String path) throws IOException {
        return new String(Files.readAllBytes(projectRoot().resolve(path)), StandardCharsets.UTF_8);
    }

    // ---- tb_media_session ----

    @Test
    @DisplayName("tb_media_session.status 默认值应为 2 (INVITING)")
    void tb_media_session_status_default_should_be_inviting() {
        String callId = "schema-test-" + UniqueKeyFactory.callId();
        jdbcTemplate.update("INSERT INTO tb_media_session(call_id) VALUES(?)", callId);
        Integer status = jdbcTemplate.queryForObject(
            "SELECT status FROM tb_media_session WHERE call_id=?", Integer.class, callId);
        assertEquals(2, status);
    }

    @Test
    @DisplayName("tb_media_session.call_id 应为 UNIQUE")
    void tb_media_session_call_id_should_be_unique() {
        String callId = "dup-" + UniqueKeyFactory.callId();
        jdbcTemplate.update("INSERT INTO tb_media_session(call_id) VALUES(?)", callId);
        assertThrows(Exception.class,
            () -> jdbcTemplate.update("INSERT INTO tb_media_session(call_id) VALUES(?)", callId));
    }

    @Test
    @DisplayName("tb_media_session.ref_count 默认值应为 0")
    void tb_media_session_ref_count_default_should_be_0() {
        String callId = "refcount-" + UniqueKeyFactory.callId();
        jdbcTemplate.update("INSERT INTO tb_media_session(call_id) VALUES(?)", callId);
        Integer refCount = jdbcTemplate.queryForObject(
            "SELECT ref_count FROM tb_media_session WHERE call_id=?", Integer.class, callId);
        assertEquals(0, refCount);
    }

    @Test
    @DisplayName("tb_media_session.stream_id 应为 UNIQUE")
    void tb_media_session_stream_id_should_be_unique() {
        String streamId = "stream-" + UniqueKeyFactory.callId();
        jdbcTemplate.update("INSERT INTO tb_media_session(call_id, stream_id) VALUES(?,?)",
            "c1-" + streamId, streamId);
        assertThrows(Exception.class,
            () -> jdbcTemplate.update("INSERT INTO tb_media_session(call_id, stream_id) VALUES(?,?)",
                "c2-" + streamId, streamId));
    }

    // ---- tb_alarm ----

    @Test
    @DisplayName("tb_alarm.ack_status 默认值应为 0")
    void tb_alarm_ack_status_default_should_be_0() {
        String deviceId = "alarm-dev-" + UniqueKeyFactory.deviceId();
        jdbcTemplate.update("INSERT INTO tb_alarm(device_id) VALUES(?)", deviceId);
        Integer ackStatus = jdbcTemplate.queryForObject(
            "SELECT ack_status FROM tb_alarm WHERE device_id=?", Integer.class, deviceId);
        assertEquals(0, ackStatus);
    }

    // ---- tb_media_node ----

    @Test
    @DisplayName("tb_media_node.weight 默认值应为 100")
    void tb_media_node_weight_default_should_be_100() {
        String serverId = "schema-node-" + UniqueKeyFactory.serverId();
        jdbcTemplate.update(
            "INSERT INTO tb_media_node(server_id,host,secret) VALUES(?,?,?)",
            serverId, "127.0.0.1", "secret");
        Integer weight = jdbcTemplate.queryForObject(
            "SELECT weight FROM tb_media_node WHERE server_id=?", Integer.class, serverId);
        assertEquals(100, weight);
    }

    @Test
    @DisplayName("tb_media_node.server_id 应为 UNIQUE")
    void tb_media_node_server_id_should_be_unique() {
        String serverId = "dup-node-" + UniqueKeyFactory.serverId();
        jdbcTemplate.update("INSERT INTO tb_media_node(server_id,host,secret) VALUES(?,?,?)",
            serverId, "127.0.0.1", "s");
        assertThrows(Exception.class,
            () -> jdbcTemplate.update("INSERT INTO tb_media_node(server_id,host,secret) VALUES(?,?,?)",
                serverId, "127.0.0.2", "s2"));
    }

    @Test
    @DisplayName("tb_media_node.enabled 默认值应为 1")
    void tb_media_node_enabled_default_should_be_1() {
        String serverId = "schema-enabled-" + UniqueKeyFactory.serverId();
        jdbcTemplate.update("INSERT INTO tb_media_node(server_id,host,secret) VALUES(?,?,?)",
            serverId, "127.0.0.1", "s");
        Integer enabled = jdbcTemplate.queryForObject(
            "SELECT enabled FROM tb_media_node WHERE server_id=?", Integer.class, serverId);
        assertEquals(1, enabled);
    }

    // ---- tb_stream_proxy ----

    @Test
    @DisplayName("tb_stream_proxy (app, stream) 复合 UNIQUE 应生效")
    void tb_stream_proxy_app_stream_should_be_unique() {
        String app = UniqueKeyFactory.app();
        String stream = UniqueKeyFactory.stream();
        jdbcTemplate.update("INSERT INTO tb_stream_proxy(app,stream,url) VALUES(?,?,?)",
            app, stream, "rtsp://x");
        assertThrows(Exception.class,
            () -> jdbcTemplate.update("INSERT INTO tb_stream_proxy(app,stream,url) VALUES(?,?,?)",
                app, stream, "rtsp://y"));
    }

    @Test
    @DisplayName("tb_stream_proxy.status 默认值应为 1")
    void tb_stream_proxy_status_default_should_be_1() {
        String app = UniqueKeyFactory.app();
        String stream = UniqueKeyFactory.stream();
        jdbcTemplate.update("INSERT INTO tb_stream_proxy(app,stream,url) VALUES(?,?,?)",
            app, stream, "rtsp://x");
        Integer status = jdbcTemplate.queryForObject(
            "SELECT status FROM tb_stream_proxy WHERE app=? AND stream=?",
            Integer.class, app, stream);
        assertEquals(1, status);
    }

    @Test
    @DisplayName("tb_stream_proxy.online_status 默认值应为 0")
    void tb_stream_proxy_online_status_default_should_be_0() {
        String app = UniqueKeyFactory.app();
        String stream = UniqueKeyFactory.stream();
        jdbcTemplate.update("INSERT INTO tb_stream_proxy(app,stream,url) VALUES(?,?,?)",
            app, stream, "rtsp://x");
        Integer onlineStatus = jdbcTemplate.queryForObject(
            "SELECT online_status FROM tb_stream_proxy WHERE app=? AND stream=?",
            Integer.class, app, stream);
        assertEquals(0, onlineStatus);
    }

    // ---- tb_device ----

    @Test
    @DisplayName("tb_device.device_id 应为 UNIQUE")
    void tb_device_device_id_should_be_unique() {
        String deviceId = "dup-dev-" + UniqueKeyFactory.deviceId();
        jdbcTemplate.update(
            "INSERT INTO tb_device(device_id,ip,port,server_ip) VALUES(?,?,?,?)",
            deviceId, "127.0.0.1", 5060, "127.0.0.1");
        assertThrows(Exception.class,
            () -> jdbcTemplate.update(
                "INSERT INTO tb_device(device_id,ip,port,server_ip) VALUES(?,?,?,?)",
                deviceId, "127.0.0.2", 5060, "127.0.0.1"));
    }

    // ---- tb_device_channel ----

    @Test
    @DisplayName("tb_device_channel (channel_id, device_id) 复合 UNIQUE 应生效")
    void tb_device_channel_composite_unique_should_work() {
        String channelId = UniqueKeyFactory.channelId();
        String deviceId = UniqueKeyFactory.deviceId();
        jdbcTemplate.update(
            "INSERT INTO tb_device_channel(channel_id,device_id) VALUES(?,?)",
            channelId, deviceId);
        assertThrows(Exception.class,
            () -> jdbcTemplate.update(
                "INSERT INTO tb_device_channel(channel_id,device_id) VALUES(?,?)",
                channelId, deviceId));
    }

    // ---- tb_device_config ----

    @Test
    @DisplayName("tb_device_config (device_id, config_key) 复合 UNIQUE 应生效")
    void tb_device_config_composite_unique_should_work() {
        jdbcTemplate.update(
            "INSERT INTO tb_device_config(device_id,config_key,config_value) VALUES(?,?,?)",
            999001, "key1", "v1");
        assertThrows(Exception.class,
            () -> jdbcTemplate.update(
                "INSERT INTO tb_device_config(device_id,config_key,config_value) VALUES(?,?,?)",
                999001, "key1", "v2"));
    }
}
