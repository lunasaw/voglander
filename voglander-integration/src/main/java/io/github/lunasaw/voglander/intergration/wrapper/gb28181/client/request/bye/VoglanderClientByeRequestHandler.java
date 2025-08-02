package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.request.bye;

import io.github.lunasaw.gbproxy.client.transmit.request.bye.ByeProcessorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Voglander GB28181客户端BYE请求处理器
 * 
 * @author luna
 * @date 2025/7/31
 */
@Slf4j
@Component
public class VoglanderClientByeRequestHandler implements ByeProcessorHandler {

    @Override
    public void closeStream(String callId) {
        log.info("处理BYE请求，关闭流 - callId: {}", callId);

        try {
            // TODO: 实现流关闭逻辑
            // 1. 根据callId查找对应的媒体流会话
            // 2. 停止RTP流传输
            // 3. 释放媒体资源
            // 4. 清理会话状态

            if (callId == null || callId.isEmpty()) {
                log.warn("callId为空，无法关闭流");
                return;
            }

            // 查找并关闭对应的媒体流
            closeMediaStream(callId);

            // 清理会话相关资源
            cleanupSession(callId);

            log.info("BYE请求处理完成，流已关闭 - callId: {}", callId);
        } catch (Exception e) {
            log.error("处理BYE请求失败 - callId: {}, error: {}", callId, e.getMessage(), e);
            throw new RuntimeException("BYE请求处理失败", e);
        }
    }

    /**
     * 关闭媒体流
     */
    private void closeMediaStream(String callId) {
        // TODO: 实现媒体流关闭逻辑
        // 1. 停止RTP接收/发送
        // 2. 关闭媒体端口
        // 3. 通知媒体服务器停止推流
        log.debug("关闭媒体流 - callId: {}", callId);
    }

    /**
     * 清理会话资源
     */
    private void cleanupSession(String callId) {
        // TODO: 实现会话资源清理逻辑
        // 1. 从会话管理器中移除会话信息
        // 2. 释放分配的资源（端口、内存等）
        // 3. 更新设备状态
        log.debug("清理会话资源 - callId: {}", callId);
    }
}