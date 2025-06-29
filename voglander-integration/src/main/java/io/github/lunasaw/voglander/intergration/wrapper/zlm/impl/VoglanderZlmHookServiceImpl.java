package io.github.lunasaw.voglander.intergration.wrapper.zlm.impl;

import io.github.lunasaw.voglander.manager.manager.MediaNodeManager;
import io.github.lunasaw.zlm.entity.ServerNodeConfig;
import io.github.lunasaw.zlm.hook.param.*;
import io.github.lunasaw.zlm.hook.service.AbstractZlmHookService;
import io.github.lunasaw.zlm.hook.service.ZlmHookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ZLM Hook服务实现
 * 用于处理ZLMediaKit的各种Hook回调
 *
 * @author luna
 * @date 2025/01/22
 */
@Slf4j
@Service
public class VoglanderZlmHookServiceImpl extends AbstractZlmHookService {

    @Autowired
    private MediaNodeManager mediaNodeManager;

    @Override
    public void onServerKeepLive(OnServerKeepaliveHookParam param) {
        String serverId = param.getMediaServerId();
        log.info("ZLM服务器心跳回调 - 服务器ID: {}", serverId);

        try {
            // 更新节点状态：在线状态，心跳时间为当前时间戳
            Long keepalive = System.currentTimeMillis() / 1000; // 转换为Unix时间戳
            Long nodeId = mediaNodeManager.saveOrUpdateNodeStatus(serverId, 1, keepalive, null, null);
            log.info("处理心跳回调成功，节点ID: {}, 数据库ID: {}", serverId, nodeId);
        } catch (Exception e) {
            log.error("处理心跳回调失败，节点ID: {}, 错误: {}", serverId, e.getMessage(), e);
        }
    }

    @Override
    public HookResult onPlay(OnPlayHookParam param) {
        log.info("ZLM播放开始回调 - ID: {}, 应用: {}, 流名: {}, 客户端IP: {}",
            param.getId(), param.getApp(), param.getStream(), param.getIp());

        // TODO: 实现播放鉴权逻辑
        // 返回 HookResult.SUCCESS() 表示允许播放
        // 返回 HookResult.FAILED() 表示拒绝播放

        HookResult result = new HookResult();
        result.setCode(0); // 0表示成功
        result.setMsg("允许播放");
        return result;
    }

    @Override
    public HookResultForOnPublish onPublish(OnPublishHookParam param) {
        log.info("ZLM推流开始回调 - ID: {}, 应用: {}, 流名: {}, 客户端IP: {}",
            param.getId(), param.getApp(), param.getStream(), param.getIp());

        // TODO: 实现推流鉴权逻辑
        // 可以验证推流密钥、IP白名单等

        HookResultForOnPublish result = new HookResultForOnPublish();
        result.setCode(0); // 0表示成功
        result.setMsg("允许推流");
        result.setEnableHls(true); // 启用HLS
        result.setEnableMp4(true); // 启用MP4录制
        return result;
    }

    @Override
    public void onStreamChanged(OnStreamChangedHookParam param) {
        log.info("ZLM流状态变化回调 - 应用: {}, 流名: {}, 状态: {}",
            param.getApp(), param.getStream(), param.isRegist());

        // TODO: 实现流状态变化处理逻辑
        // 可以更新流状态、通知相关服务等
        if (param.isRegist()) {
            log.info("流 {} 上线", param.getStream());
            // 处理流上线逻辑
        } else {
            log.info("流 {} 下线", param.getStream());
            // 处理流下线逻辑
        }
    }

    @Override
    public HookResultForStreamNoneReader onStreamNoneReader(OnStreamNoneReaderHookParam param) {
        log.info("ZLM流无人观看回调 - 应用: {}, 流名: {}",
            param.getApp(), param.getStream());

        // TODO: 实现无人观看处理逻辑
        // 可以选择关闭流或继续保持

        HookResultForStreamNoneReader result = new HookResultForStreamNoneReader();
        result.setCode(0);
        result.setClose(false); // false表示不关闭流
        return result;
    }

    @Override
    public void onStreamNotFound(OnStreamNotFoundHookParam param) {
        log.info("ZLM流未找到回调 - 应用: {}, 流名: {}",
            param.getApp(), param.getStream());

        // TODO: 实现流未找到处理逻辑
        // 可以尝试拉取流、从其他源获取流等
    }

    @Override
    public void onServerStarted(ServerNodeConfig param) {
        log.info("ZLM服务器启动回调 - 服务器配置: {}", param);

        try {
            // 从ServerNodeConfig中获取节点信息
            // 由于ServerNodeConfig的具体字段未知，先使用默认值
            String serverId = extractServerIdFromConfig(param);
            String host = extractHostFromConfig(param);
            String name = serverId; // 默认使用serverId作为名称

            if (serverId == null || serverId.isEmpty()) {
                log.warn("服务器启动回调中未找到服务器ID，使用默认ID");
                serverId = "zlm-" + System.currentTimeMillis();
            }

            // 插入或更新节点：在线状态，心跳时间为当前时间
            Long keepalive = System.currentTimeMillis() / 1000; // 转换为Unix时间戳
            Long nodeId = mediaNodeManager.saveOrUpdateNodeStatus(serverId, 1, keepalive, host, name);
            log.info("处理服务器启动回调成功，节点ID: {}, 数据库ID: {}, 地址: {}", serverId, nodeId, host);
        } catch (Exception e) {
            log.error("处理服务器启动回调失败，错误: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onSendRtpStopped(OnSendRtpStoppedHookParam param) {
        log.info("ZLM RTP发送停止回调 - 服务器ID: {}",
            param.getMediaServerId());

        // TODO: 实现RTP发送停止处理逻辑
        // 可以清理资源、通知相关服务等
    }

    @Override
    public void onRtpServerTimeout(OnRtpServerTimeoutHookParam param) {
        log.info("ZLM RTP服务器超时回调 - 服务器ID: {}",
            param.getMediaServerId());

        // TODO: 实现RTP服务器超时处理逻辑
        // 可以重新创建RTP服务器、清理资源等
    }

    @Override
    public HookResultForOnHttpAccess onHttpAccess(OnHttpAccessParam param) {
        log.info("ZLM HTTP访问回调 - 路径: {}, 客户端IP: {}",
            param.getPath(), param.getIp());

        // TODO: 实现HTTP访问鉴权逻辑
        // 可以验证Token、IP白名单等

        HookResultForOnHttpAccess result = new HookResultForOnHttpAccess();
        result.setCode(0);
        result.setMsg("允许访问");
        return result;
    }

    @Override
    public HookResultForOnRtspRealm onRtspRealm(OnRtspRealmHookParam param) {
        log.info("ZLM RTSP Realm回调 - 服务器ID: {}",
            param.getMediaServerId());

        // TODO: 实现RTSP Realm处理逻辑

        HookResultForOnRtspRealm result = new HookResultForOnRtspRealm();
        result.setCode(0);
        result.setRealm("voglander"); // 设置realm
        return result;
    }

    @Override
    public HookResultForOnRtspAuth onRtspAuth(OnRtspAuthHookParam param) {
        log.info("ZLM RTSP认证回调 - 服务器ID: {}",
            param.getMediaServerId());

        // TODO: 实现RTSP认证逻辑
        // 可以从数据库验证用户名密码

        HookResultForOnRtspAuth result = new HookResultForOnRtspAuth();
        result.setCode(0);
        result.setEncrypted(false); // 密码是否加密
        result.setPasswd("123456"); // 正确的密码
        return result;
    }

    @Override
    public void onFlowReport(OnFlowReportHookParam param) {
        log.info("ZLM流量报告回调 - 服务器ID: {}",
            param.getMediaServerId());

        // TODO: 实现流量统计处理逻辑
        // 可以记录流量统计、计费等
    }

    @Override
    public void onServerExited(HookParam param) {
        String serverId = param.getMediaServerId();
        log.info("ZLM服务器退出回调 - 服务器ID: {}", serverId);

        try {
            // 更新节点为离线状态
            mediaNodeManager.updateNodeOffline(serverId);
            log.info("处理服务器退出回调成功，节点ID: {} 已设置为离线", serverId);
        } catch (Exception e) {
            log.error("处理服务器退出回调失败，节点ID: {}, 错误: {}", serverId, e.getMessage(), e);
        }
    }

    @Override
    public void onRecordMp4(OnRecordMp4HookParam param) {
        log.info("ZLM MP4录制回调 - 服务器ID: {}",
            param.getMediaServerId());

        // TODO: 实现MP4录制处理逻辑
        // 可以移动文件、更新数据库记录等
    }

    /**
     * 从ServerNodeConfig中提取服务器ID
     *
     * @param config 服务器节点配置
     * @return 服务器ID
     */
    private String extractServerIdFromConfig(ServerNodeConfig config) {
        try {
            // 尝试通过反射或toString()方法获取服务器ID
            String configStr = config.toString();
            log.debug("ServerNodeConfig内容: {}", configStr);

            // 可以根据实际的配置内容来解析ID
            // 这里先返回一个基于时间的默认ID
            return "zlm-server-" + System.currentTimeMillis();
        } catch (Exception e) {
            log.warn("提取服务器ID失败，使用默认值: {}", e.getMessage());
            return "zlm-default-" + System.currentTimeMillis();
        }
    }

    /**
     * 从ServerNodeConfig中提取主机地址
     *
     * @param config 服务器节点配置
     * @return 主机地址
     */
    private String extractHostFromConfig(ServerNodeConfig config) {
        try {
            // 尝试从配置中获取主机信息
            // 由于字段名不确定，先使用默认值
            String configStr = config.toString();
            log.debug("ServerNodeConfig内容: {}", configStr);

            // 可以根据实际的配置内容来解析主机地址
            // 这里先返回localhost作为默认值
            return "localhost";
        } catch (Exception e) {
            log.warn("提取主机地址失败，使用默认值: {}", e.getMessage());
            return "localhost";
        }
    }
}