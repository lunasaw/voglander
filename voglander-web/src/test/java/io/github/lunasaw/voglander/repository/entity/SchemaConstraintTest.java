package io.github.lunasaw.voglander.repository.entity;

import static org.junit.jupiter.api.Assertions.*;

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
