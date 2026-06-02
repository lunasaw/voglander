package io.github.lunasaw.voglander.support;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 跨线程测试数据清理工具（不加 @Transactional 的测试使用）
 *
 * @author luna
 */
@Slf4j
@Component
public class TestDataCleaner {

    private static final List<String> TABLES = List.of(
        "tb_media_node",
        "tb_media_session",
        "tb_stream_proxy",
        "tb_push_proxy",
        "tb_device",
        "tb_device_channel",
        "tb_device_config",
        "tb_export_task"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 清理指定表中包含特定标识符的测试数据（通过 server_id/device_id/call_id 精确删除） */
    public void deleteMediaNodeByServerId(String serverId) {
        jdbcTemplate.update("DELETE FROM tb_media_node WHERE server_id = ?", serverId);
    }

    public void deleteDeviceByDeviceId(String deviceId) {
        jdbcTemplate.update("DELETE FROM tb_device WHERE device_id = ?", deviceId);
    }

    public void deleteMediaSessionByCallId(String callId) {
        jdbcTemplate.update("DELETE FROM tb_media_session WHERE call_id = ?", callId);
    }

    public void deleteStreamProxyByAppStream(String app, String stream) {
        jdbcTemplate.update("DELETE FROM tb_stream_proxy WHERE app = ? AND stream = ?", app, stream);
    }

    /** 清空所有白名单测试表（慎用，仅限无 @Transactional 的测试 @AfterEach） */
    public void cleanAll() {
        for (String table : TABLES) {
            try {
                jdbcTemplate.update("DELETE FROM " + table);
            } catch (Exception e) {
                log.warn("清理表 {} 失败: {}", table, e.getMessage());
            }
        }
    }
}
