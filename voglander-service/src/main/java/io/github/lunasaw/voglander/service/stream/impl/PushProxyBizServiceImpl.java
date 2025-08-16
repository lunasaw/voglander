package io.github.lunasaw.voglander.service.stream.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.PushProxyZlmWrapperService;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto.MediaOnlineRequest;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto.PushProxyDeleteRequest;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto.PushProxyRequest;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaNodeDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.PushProxyDTO;
import io.github.lunasaw.voglander.manager.manager.MediaNodeManager;
import io.github.lunasaw.voglander.manager.manager.PushProxyManager;
import io.github.lunasaw.voglander.service.stream.PushProxyBizService;
import io.github.lunasaw.zlm.entity.MediaOnlineStatus;
import io.github.lunasaw.zlm.entity.StreamKey;
import io.github.lunasaw.zlm.entity.StreamPusherItem;
import io.github.lunasaw.zlm.entity.req.MediaReq;
import lombok.extern.slf4j.Slf4j;

/**
 * Push Proxy 业务服务实现
 * <p>
 * 提供推流代理的高级业务逻辑，包括节点选择、推流管理、状态同步等功能
 * </p>
 *
 * @author luna
 * @since 2025-01-23
 */
@Slf4j
@Service
public class PushProxyBizServiceImpl implements PushProxyBizService {

    @Autowired
    private PushProxyManager           pushProxyManager;

    @Autowired
    private MediaNodeManager           mediaNodeManager;

    @Autowired
    private PushProxyZlmWrapperService zlmWrapperService;

    @Override
    public Long createPushProxyWithSpecificNode(PushProxyDTO pushProxyDTO) {
        Assert.notNull(pushProxyDTO, "推流代理配置不能为空");
        Assert.hasText(pushProxyDTO.getServerId(), "节点ID不能为空");

        String serverId = pushProxyDTO.getServerId();
        log.info("开始创建推流代理（指定节点） - 应用: {}, 流: {}, 目标地址: {}, 节点: {}",
            pushProxyDTO.getApp(), pushProxyDTO.getStream(), pushProxyDTO.getDstUrl(), serverId);

        // 获取节点信息
        MediaNodeDTO node = mediaNodeManager.getDTOByServerId(serverId);
        if (node == null || !node.getEnabled()) {
            log.error("创建推流代理失败：节点不存在或未启用 - 节点ID: {}", serverId);
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, "节点不存在或未启用");
        }

        // 首先保存到数据库
        Long proxyId = pushProxyManager.createPushProxy(pushProxyDTO);
        try {
            // 创建ZLM推流代理
            boolean started = startPushProxy(proxyId);
            if (!started) {
                log.warn("推流代理创建成功但启动失败 - ID: {}, 应用: {}, 流: {}",
                    proxyId, pushProxyDTO.getApp(), pushProxyDTO.getStream());
            }

            log.info("创建推流代理成功 - ID: {}, 应用: {}, 流: {}, 节点: {}, 启动状态: {}",
                proxyId, pushProxyDTO.getApp(), pushProxyDTO.getStream(), serverId, started);

            return proxyId;

        } catch (Exception e) {
            log.error("创建ZLM推流代理失败但数据库记录已保存 - ID: {}, 应用: {}, 流: {}, 节点: {}, 异常: {}",
                proxyId, pushProxyDTO.getApp(), pushProxyDTO.getStream(), serverId, e.getMessage(), e);

            // 返回ID，允许后续手动重试
            return proxyId;
        }
    }

    @Override
    public boolean updatePushProxyWithRecreation(PushProxyDTO updateDTO) {
        Assert.notNull(updateDTO, "更新信息不能为空");

        try {
            log.info("开始更新推流代理 - ID: {}, 应用: {}, 流: {}",
                updateDTO.getId(), updateDTO.getApp(), updateDTO.getStream());

            // 使用Manager层的模板方法进行更新
            boolean updated = pushProxyManager.updatePushProxy(updateDTO, "更新推流代理");

            if (updated) {
                log.info("更新推流代理成功 - ID: {}", updateDTO.getId());
            } else {
                log.warn("更新推流代理失败 - ID: {}", updateDTO.getId());
            }

            return updated;

        } catch (Exception e) {
            log.error("更新推流代理异常 - ID: {}, 错误: {}", updateDTO.getId(), e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, "更新推流代理失败: " + e.getMessage());
        }
    }

    @Override
    public boolean updatePushProxyWithRecreation(Long id, PushProxyDTO updateDTO) {
        Assert.notNull(id, "推流代理ID不能为空");
        Assert.notNull(updateDTO, "更新信息不能为空");

        // 构造查询条件DTO
        PushProxyDTO queryDTO = new PushProxyDTO();
        queryDTO.setId(id);

        // 调用主要方法
        return updatePushProxyWithRecreation(updateDTO);
    }

    @Override
    public boolean deletePushProxyWithTermination(PushProxyDTO deleteDTO) {
        Assert.notNull(deleteDTO, "删除条件不能为空");

        try {
            log.info("开始删除推流代理 - ID: {}, 应用: {}, 流: {}, proxyKey: {}",
                deleteDTO.getId(), deleteDTO.getApp(), deleteDTO.getStream(), deleteDTO.getProxyKey());

            // 先停止推流代理
            try {
                stopPushProxy(deleteDTO);
            } catch (Exception e) {
                log.warn("停止推流代理失败但继续删除数据库记录 - 错误: {}", e.getMessage());
            }

            // 删除数据库记录
            boolean deleted = pushProxyManager.deletePushProxy(deleteDTO, "删除推流代理");

            if (deleted) {
                log.info("删除推流代理成功 - ID: {}", deleteDTO.getId());
            } else {
                log.warn("删除推流代理失败 - ID: {}", deleteDTO.getId());
            }

            return deleted;

        } catch (Exception e) {
            log.error("删除推流代理异常 - ID: {}, 错误: {}", deleteDTO.getId(), e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, "删除推流代理失败: " + e.getMessage());
        }
    }

    @Override
    public boolean deletePushProxyByKeyWithTermination(String proxyKey) {
        Assert.hasText(proxyKey, "代理key不能为空");

        // 构造删除条件DTO
        PushProxyDTO deleteDTO = new PushProxyDTO();
        deleteDTO.setProxyKey(proxyKey);

        return deletePushProxyWithTermination(deleteDTO);
    }

    @Override
    public boolean updatePushProxyStatus(PushProxyDTO statusDTO) {
        Assert.notNull(statusDTO, "状态更新信息不能为空");
        Assert.notNull(statusDTO.getStatus(), "状态值不能为空");

        try {
            log.info("开始更新推流代理状态 - ID: {}, 状态: {}",
                statusDTO.getId(), statusDTO.getStatus());

            boolean updated = pushProxyManager.updatePushProxy(statusDTO, "更新推流代理状态");

            if (updated) {
                // 根据状态启动或停止推流代理
                if (statusDTO.getStatus() == 1) {
                    startPushProxy(statusDTO);
                } else {
                    stopPushProxy(statusDTO);
                }
                log.info("更新推流代理状态成功 - ID: {}, 状态: {}", statusDTO.getId(), statusDTO.getStatus());
            }

            return updated;

        } catch (Exception e) {
            log.error("更新推流代理状态异常 - ID: {}, 状态: {}, 错误: {}",
                statusDTO.getId(), statusDTO.getStatus(), e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, "更新推流代理状态失败: " + e.getMessage());
        }
    }

    @Override
    public boolean updatePushProxyStatus(Long id, Integer status) {
        Assert.notNull(id, "推流代理ID不能为空");
        Assert.notNull(status, "状态值不能为空");

        PushProxyDTO statusDTO = new PushProxyDTO();
        statusDTO.setId(id);
        statusDTO.setStatus(status);

        return updatePushProxyStatus(statusDTO);
    }

    @Override
    public boolean syncPushProxyOnlineStatus(PushProxyDTO syncDTO) {
        // TODO: Implement ZLM status sync when PushProxyZlmWrapperService is available
        log.info("同步推流代理在线状态 - ID: {}, 应用: {}, 流: {}",
            syncDTO.getId(), syncDTO.getApp(), syncDTO.getStream());

        // Placeholder implementation
        return true;
    }

    @Override
    public boolean syncPushProxyOnlineStatus(Long id) {
        Assert.notNull(id, "推流代理ID不能为空");

        PushProxyDTO syncDTO = new PushProxyDTO();
        syncDTO.setId(id);

        return syncPushProxyOnlineStatus(syncDTO);
    }

    @Override
    public int syncAllEnabledPushProxyStatus() {
        // TODO: Implement batch sync when PushProxyZlmWrapperService is available
        log.info("批量同步所有启用的推流代理在线状态");

        // Placeholder implementation
        return 0;
    }

    @Override
    public boolean startPushProxy(PushProxyDTO startDTO) {
        Assert.notNull(startDTO, "启动参数不能为空");

        try {
            log.info("启动推流代理 - ID: {}, 应用: {}, 流: {}",
                startDTO.getId(), startDTO.getApp(), startDTO.getStream());

            // 查询完整的推流代理信息
            PushProxyDTO fullProxyInfo = pushProxyManager.get(startDTO);
            if (fullProxyInfo == null) {
                log.error("启动推流代理失败：未找到代理信息 - ID: {}", startDTO.getId());
                return false;
            }

            // 获取节点信息
            MediaNodeDTO node = mediaNodeManager.getDTOByServerId(fullProxyInfo.getServerId());
            if (node == null || !node.getEnabled()) {
                log.error("启动推流代理失败：节点不存在或未启用 - 节点ID: {}", fullProxyInfo.getServerId());
                return false;
            }

            // 构建ZLM推流参数
            StreamPusherItem pusherItem = buildStreamPusherItem(fullProxyInfo);

            // 调用ZLM API创建推流代理
            PushProxyRequest request = PushProxyRequest.builder()
                .host(node.getHost())
                .secret(node.getSecret())
                .streamPusherItem(pusherItem)
                .build();

            ResultDTO<StreamKey> result = zlmWrapperService.addPushProxy(request);
            if (result == null || result.getData() == null) {
                log.error("启动推流代理失败：ZLM API调用失败 - {}", result != null ? result.getMessage() : "响应为空");
                return false;
            }

            // 更新代理状态和密钥
            String proxyKey = result.getData().getKey();
            pushProxyManager.updatePushProxyKey(fullProxyInfo.getId(), proxyKey, "启动推流代理");
            pushProxyManager.updatePushProxyOnlineStatus(fullProxyInfo.getId(), 1, "启动推流代理");

            log.info("启动推流代理成功 - ID: {}, proxyKey: {}", fullProxyInfo.getId(), proxyKey);
            return true;

        } catch (Exception e) {
            log.error("启动推流代理异常 - ID: {}, 错误: {}", startDTO.getId(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean startPushProxy(Long id) {
        Assert.notNull(id, "推流代理ID不能为空");

        PushProxyDTO startDTO = new PushProxyDTO();
        startDTO.setId(id);

        return startPushProxy(startDTO);
    }

    @Override
    public boolean stopPushProxy(PushProxyDTO stopDTO) {
        Assert.notNull(stopDTO, "停止参数不能为空");

        try {
            log.info("停止推流代理 - ID: {}, 应用: {}, 流: {}",
                stopDTO.getId(), stopDTO.getApp(), stopDTO.getStream());

            // 查询完整的推流代理信息
            PushProxyDTO fullProxyInfo = pushProxyManager.get(stopDTO);
            if (fullProxyInfo == null) {
                log.warn("停止推流代理：未找到代理信息 - ID: {}", stopDTO.getId());
                return true; // 认为已经停止
            }

            // 如果没有proxyKey，则认为已经停止
            if (fullProxyInfo.getProxyKey() == null || fullProxyInfo.getProxyKey().trim().isEmpty()) {
                log.info("推流代理已停止（无proxyKey） - ID: {}", fullProxyInfo.getId());
                pushProxyManager.updatePushProxyOnlineStatus(fullProxyInfo.getId(), 0, "停止推流代理");
                return true;
            }

            // 获取节点信息
            MediaNodeDTO node = mediaNodeManager.getDTOByServerId(fullProxyInfo.getServerId());
            if (node == null || !node.getEnabled()) {
                log.warn("停止推流代理：节点不存在或未启用，仅更新本地状态 - 节点ID: {}", fullProxyInfo.getServerId());
                pushProxyManager.updatePushProxyOnlineStatus(fullProxyInfo.getId(), 0, "停止推流代理");
                return true;
            }

            // 调用ZLM API删除推流代理
            PushProxyDeleteRequest request = PushProxyDeleteRequest.builder()
                .host(node.getHost())
                .secret(node.getSecret())
                .proxyKey(fullProxyInfo.getProxyKey())
                .build();

            ResultDTO<StreamKey.StringDelFlag> result = zlmWrapperService.deletePushProxy(request);
            if (result == null || result.getData() == null) {
                log.warn("停止推流代理：ZLM API调用失败，仅更新本地状态 - {}", result != null ? result.getMessage() : "响应为空");
            } else {
                log.info("停止推流代理成功 - ID: {}, proxyKey: {}", fullProxyInfo.getId(), fullProxyInfo.getProxyKey());
            }

            // 更新本地状态
            pushProxyManager.updatePushProxyOnlineStatus(fullProxyInfo.getId(), 0, "停止推流代理");
            return true;

        } catch (Exception e) {
            log.error("停止推流代理异常 - ID: {}, 错误: {}", stopDTO.getId(), e.getMessage(), e);
            // 即使出现异常，也尝试更新本地状态
            try {
                pushProxyManager.updatePushProxyOnlineStatus(stopDTO.getId(), 0, "停止推流代理（异常）");
            } catch (Exception ex) {
                log.error("更新推流代理状态异常: {}", ex.getMessage());
            }
            return false;
        }
    }

    @Override
    public boolean stopPushProxy(Long id) {
        Assert.notNull(id, "推流代理ID不能为空");

        PushProxyDTO stopDTO = new PushProxyDTO();
        stopDTO.setId(id);

        return stopPushProxy(stopDTO);
    }

    @Override
    public boolean checkSourceStreamOnline(PushProxyDTO checkDTO) {
        Assert.notNull(checkDTO, "检查参数不能为空");
        Assert.hasText(checkDTO.getServerId(), "节点ID不能为空");
        Assert.hasText(checkDTO.getApp(), "应用名称不能为空");
        Assert.hasText(checkDTO.getStream(), "流名称不能为空");

        try {
            log.info("检查源流是否在线 - 应用: {}, 流: {}, 节点: {}",
                checkDTO.getApp(), checkDTO.getStream(), checkDTO.getServerId());

            // 获取节点信息
            MediaNodeDTO node = mediaNodeManager.getDTOByServerId(checkDTO.getServerId());
            if (node == null || !node.getEnabled()) {
                log.error("检查源流在线状态失败：节点不存在或未启用 - 节点ID: {}", checkDTO.getServerId());
                return false;
            }

            // 构建查询请求
            MediaReq mediaReq = new MediaReq();
            mediaReq.setApp(checkDTO.getApp());
            mediaReq.setStream(checkDTO.getStream());

            MediaOnlineRequest request = new MediaOnlineRequest();
            request.setHost(node.getHost());
            request.setSecret(node.getSecret());
            request.setMediaReq(mediaReq);

            // 调用ZLM API检查在线状态
            ResultDTO<MediaOnlineStatus> result = zlmWrapperService.isSourceStreamOnline(request);
            if (result == null || result.getData() == null) {
                log.warn("检查源流在线状态失败：ZLM API调用失败 - {}", result != null ? result.getMessage() : "响应为空");
                return false;
            }

            MediaOnlineStatus status = result.getData();
            boolean isOnline = status.getOnline() != null && status.getOnline();

            log.info("源流在线状态 - 应用: {}, 流: {}, 在线: {}",
                checkDTO.getApp(), checkDTO.getStream(), isOnline);

            return isOnline;

        } catch (Exception e) {
            log.error("检查源流在线状态异常 - 应用: {}, 流: {}, 错误: {}",
                checkDTO.getApp(), checkDTO.getStream(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建ZLM推流参数
     *
     * @param pushProxyDTO 推流代理信息
     * @return ZLM推流参数
     */
    private StreamPusherItem buildStreamPusherItem(PushProxyDTO pushProxyDTO) {
        // 获取扩展配置
        PushProxyDTO.ExtendObj extendObj = pushProxyDTO.getExtendObj();
        if (extendObj == null) {
            extendObj = new PushProxyDTO.ExtendObj();
        }

        // 构建推流参数
        StreamPusherItem pusherItem = new StreamPusherItem(
            extendObj.getVhost() != null ? extendObj.getVhost() : "__defaultVhost__",
            pushProxyDTO.getSchema() != null ? pushProxyDTO.getSchema() : "rtmp",
            pushProxyDTO.getApp(),
            pushProxyDTO.getStream(),
            pushProxyDTO.getDstUrl());

        // 设置可选参数
        if (extendObj.getRetryCount() != null) {
            pusherItem.setRetryCount(extendObj.getRetryCount());
        }
        if (extendObj.getRtpType() != null) {
            pusherItem.setRtpType(extendObj.getRtpType());
        }
        if (extendObj.getTimeoutSec() != null) {
            pusherItem.setTimeoutSec(extendObj.getTimeoutSec());
        }

        return pusherItem;
    }

    @Override
    public boolean checkSourceStreamOnline(String serverId, String app, String stream) {
        Assert.hasText(serverId, "节点ID不能为空");
        Assert.hasText(app, "应用名称不能为空");
        Assert.hasText(stream, "流名称不能为空");

        PushProxyDTO checkDTO = new PushProxyDTO();
        checkDTO.setServerId(serverId);
        checkDTO.setApp(app);
        checkDTO.setStream(stream);

        return checkSourceStreamOnline(checkDTO);
    }
}