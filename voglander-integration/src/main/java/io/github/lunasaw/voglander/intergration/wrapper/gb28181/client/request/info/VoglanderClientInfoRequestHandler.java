package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.request.info;

import io.github.lunasaw.gbproxy.client.transmit.request.info.InfoRequestHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Voglander GB28181客户端INFO请求处理器
 * 
 * @author luna
 * @date 2025/7/31
 */
@Slf4j
@Component
public class VoglanderClientInfoRequestHandler implements InfoRequestHandler {

    @Override
    public void receiveInfo(String userId, String content) {
        log.info("接收INFO消息 - userId: {}", userId);
        log.debug("INFO消息内容: {}", content);

        try {
            if (userId == null || userId.isEmpty()) {
                log.warn("userId为空，无法处理INFO消息");
                return;
            }

            if (content == null || content.isEmpty()) {
                log.warn("INFO消息内容为空 - userId: {}", userId);
                return;
            }

            // TODO: 根据INFO消息内容进行相应处理
            processInfoContent(userId, content);

            log.info("INFO消息处理完成 - userId: {}", userId);
        } catch (Exception e) {
            log.error("处理INFO消息失败 - userId: {}, error: {}", userId, e.getMessage(), e);
            throw new RuntimeException("INFO消息处理失败", e);
        }
    }

    /**
     * 处理INFO消息内容
     */
    private void processInfoContent(String userId, String content) {
        // TODO: 实现INFO消息内容处理逻辑
        // INFO消息通常用于传递实时信息，如：
        // 1. RTCP报告
        // 2. 媒体流统计信息
        // 3. 设备状态更新
        // 4. 其他实时通知信息

        try {
            // 解析消息内容
            if (content.contains("RTCP")) {
                handleRtcpInfo(userId, content);
            } else if (content.contains("MediaStatus")) {
                handleMediaStatusInfo(userId, content);
            } else if (content.contains("DeviceStatus")) {
                handleDeviceStatusInfo(userId, content);
            } else {
                handleGenericInfo(userId, content);
            }
        } catch (Exception e) {
            log.error("处理INFO消息内容失败 - userId: {}, content: {}, error: {}",
                userId, content, e.getMessage(), e);
        }
    }

    /**
     * 处理RTCP信息
     */
    private void handleRtcpInfo(String userId, String content) {
        log.debug("处理RTCP信息 - userId: {}", userId);
        // TODO: 实现RTCP报告处理
        // 解析RTCP统计信息，更新媒体流质量数据
    }

    /**
     * 处理媒体状态信息
     */
    private void handleMediaStatusInfo(String userId, String content) {
        log.debug("处理媒体状态信息 - userId: {}", userId);
        // TODO: 实现媒体状态处理
        // 更新媒体流状态、码率、帧率等信息
    }

    /**
     * 处理设备状态信息
     */
    private void handleDeviceStatusInfo(String userId, String content) {
        log.debug("处理设备状态信息 - userId: {}", userId);
        // TODO: 实现设备状态处理
        // 更新设备在线状态、健康状态等信息
    }

    /**
     * 处理通用信息
     */
    private void handleGenericInfo(String userId, String content) {
        log.debug("处理通用INFO信息 - userId: {}", userId);
        // TODO: 实现通用信息处理
        // 记录或转发其他类型的INFO消息
    }
}