package io.github.lunasaw.voglander.service.stream.impl;

import java.util.List;
import java.util.Optional;

import com.alibaba.fastjson2.JSONReader;
import io.github.lunasaw.voglander.manager.service.MediaNodeService;
import io.github.lunasaw.voglander.manager.service.StreamProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.voglander.common.anno.TechnicalScheduler;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.StreamProxyZlmWrapperService;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto.MediaOnlineRequest;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto.StreamProxyDeleteRequest;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto.StreamProxyRequest;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaNodeDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.manager.manager.MediaNodeManager;
import io.github.lunasaw.voglander.manager.manager.StreamProxyManager;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
import io.github.lunasaw.voglander.service.stream.StreamProxyBizService;
import io.github.lunasaw.zlm.entity.MediaOnlineStatus;
import io.github.lunasaw.zlm.entity.StreamKey;
import io.github.lunasaw.zlm.entity.StreamProxyItem;
import io.github.lunasaw.zlm.entity.req.MediaReq;
import lombok.extern.slf4j.Slf4j;

/**
 * Stream Proxy 业务服务实现
 * <p>
 * 提供流代理的高级业务逻辑，包括节点选择、流管理、状态同步等功能
 * </p>
 *
 * @author luna
 * @since 2025-01-23
 */
@Slf4j
@Service
@TechnicalScheduler(category = TechnicalScheduler.Category.MAINTENANCE)
public class StreamProxyBizServiceImpl implements StreamProxyBizService {

    @Autowired
    private StreamProxyManager           streamProxyManager;

    @Autowired
    private MediaNodeManager             mediaNodeManager;

    @Autowired
    private StreamProxyService           streamProxyService;

    @Autowired
    private StreamProxyZlmWrapperService zlmWrapperService;

    /**
     * 定时同步开关：仅控制 {@link #scheduledSyncTask()} 定时触发器是否执行实际同步。
     * <p>
     * 启用 {@code @EnableScheduling}（Phase 0）后，本类的流代理状态同步任务（轮询 ZLM + 写库）
     * 开始按 30s 周期运行。该任务此前从未运行，为避免在 ZLM 不可达等场景下产生非预期的批量写库/离线翻转，
     * 提供本开关作为运行时逃生通道。默认 {@code true}（保留任务作者本意）。
     * </p>
     * <p>
     * 仅 gate 定时触发器；接口方法 {@link #syncAllEnabledStreamProxyStatus()} 仍可被手动直接调用。
     * </p>
     */
    @Value("${voglander.stream-proxy.scheduled-sync.enabled:true}")
    private boolean                      scheduledSyncEnabled;

    @Override
    public Long createStreamProxyWithSpecificNode(StreamProxyDTO streamProxyDTO) {
        Assert.notNull(streamProxyDTO, "流代理配置不能为空");
        Assert.hasText(streamProxyDTO.getServerId(), "节点ID不能为空");

        String serverId = streamProxyDTO.getServerId();
        log.info("开始创建流代理（指定节点） - 应用: {}, 流: {}, 地址: {}, 节点: {}",
            streamProxyDTO.getApp(), streamProxyDTO.getStream(), streamProxyDTO.getUrl(), serverId);

        // 获取节点信息
        MediaNodeDTO node = mediaNodeManager.getDTOByServerId(serverId);
        if (node == null || !node.getEnabled()) {
            log.error("创建流代理失败：节点不存在或未启用 - 节点ID: {}", serverId);
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, "节点不存在或未启用");
        }

        // 首先保存到数据库
        Long proxyId = streamProxyManager.createStreamProxy(streamProxyDTO);
        try {
            // 创建ZLM拉流代理
            boolean started = startStreamProxy(proxyId);
            if (!started) {
                log.warn("流代理创建成功但启动失败 - ID: {}, 应用: {}, 流: {}",
                    proxyId, streamProxyDTO.getApp(), streamProxyDTO.getStream());
            }

            log.info("创建流代理成功 - ID: {}, 应用: {}, 流: {}, 节点: {}, 启动状态: {}",
                proxyId, streamProxyDTO.getApp(), streamProxyDTO.getStream(), serverId, started);

            return proxyId;

        } catch (Exception e) {
            log.error("创建ZLM拉流代理失败但数据库记录已保存 - ID: {}, 应用: {}, 流: {}, 节点: {}, 异常: {}",
                proxyId, streamProxyDTO.getApp(), streamProxyDTO.getStream(), serverId, e.getMessage(), e);

            // 返回ID，允许后续手动重试
            return proxyId;
        }
    }

    @Override
    public boolean updateStreamProxyWithRecreation(StreamProxyDTO updateDTO) {
        Assert.notNull(updateDTO, "更新DTO不能为空");
        Assert.notNull(updateDTO.getId(), "流代理ID不能为空");

        log.info("开始更新流代理 - updateDTO: {}", JSON.toJSONString(updateDTO));

        // 获取现有记录
        StreamProxyDTO existingProxy = streamProxyManager.getById(updateDTO.getId());
        if (existingProxy == null) {
            log.error("更新流代理失败：记录不存在 - ID: {}", updateDTO.getId());
            throw new ServiceException(ServiceExceptionEnum.DATA_NOT_EXISTS, "流代理记录不存在");
        }

        // 检查是否需要重新创建（URL、app、stream有变化）
        boolean needRecreation = needStreamRecreation(existingProxy, updateDTO);

        if (needRecreation) {
            log.info("检测到流信息变化，需要重新创建代理 - ID: {}, 旧应用: {}, 新应用: {}, 旧流: {}, 新流: {}, 旧地址: {}, 新地址: {}",
                updateDTO.getId(), existingProxy.getApp(), updateDTO.getApp(),
                existingProxy.getStream(), updateDTO.getStream(),
                existingProxy.getUrl(), updateDTO.getUrl());

            // 停止旧的代理
            if (existingProxy.getProxyKey() != null) {
                stopStreamProxyByKey(existingProxy.getServerId(), existingProxy.getProxyKey());
            }

            // 清除旧的代理key
            updateDTO.setProxyKey(null);

            // 检查是否需要更换节点
            String targetServerId = updateDTO.getServerId();
            if (targetServerId == null) {
                targetServerId = existingProxy.getServerId();
                updateDTO.setServerId(targetServerId);
            }

            // 更新数据库记录
            Boolean updated = streamProxyManager.updateStreamProxy(updateDTO, "更新流代理-需要重新创建");

            if (updated) {
                try {
                    // 启动新的代理
                    StreamProxyDTO startDTO = new StreamProxyDTO();
                    startDTO.setId(updateDTO.getId());
                    boolean started = startStreamProxy(startDTO);
                    log.info("流代理重新创建完成 - ID: {}, 启动状态: {}", updateDTO.getId(), started);
                } catch (Exception e) {
                    log.error("更新流代理后重新创建失败 - ID: {}, 异常: {}", updateDTO.getId(), e.getMessage(), e);
                }
            }

            return updated;
        } else {
            // 普通更新，不需要重新创建代理
            log.info("流信息未变化，执行普通更新 - ID: {}", updateDTO.getId());
            return streamProxyManager.updateStreamProxy(updateDTO, "更新流代理-普通更新");
        }
    }

    @Override
    public boolean updateStreamProxyWithRecreation(Long id, StreamProxyDTO updateDTO) {
        Assert.notNull(id, "流代理ID不能为空");
        Assert.notNull(updateDTO, "更新内容不能为空");

        // 设置ID并调用主要方法
        updateDTO.setId(id);
        return updateStreamProxyWithRecreation(updateDTO);
    }

    @Override
    public boolean deleteStreamProxyWithTermination(StreamProxyDTO deleteDTO) {
        log.info("开始删除流代理 - ID: {}", JSON.toJSONString(deleteDTO));

        // 获取现有记录
        StreamProxyDTO existingProxy = streamProxyManager.get(deleteDTO);
        if (existingProxy == null) {
            log.warn("删除流代理：记录不存在 - ID: {}", JSON.toJSONString(deleteDTO));
            return true;
        }

        // 停止ZLM上的代理
        if (existingProxy.getProxyKey() != null && existingProxy.getServerId() != null) {
            stopStreamProxyByKey(existingProxy.getServerId(), existingProxy.getProxyKey());
        }

        // 删除数据库记录
        boolean deleted = streamProxyManager.deleteStreamProxy(existingProxy, "删除流代理");
        Boolean deletedOne = streamProxyManager.deleteOne(deleteDTO);
        log.info("删除流代理完成 - deleteDTO: {}, 停止拉流结果: {}, DB删除结果: {}", JSON.toJSONString(deleteDTO), deleted, deletedOne);

        return deleted;
    }

    @Override
    public boolean deleteStreamProxyByKeyWithTermination(String proxyKey) {
        Assert.hasText(proxyKey, "代理密钥不能为空");

        log.info("开始根据代理key删除流代理 - key: {}", proxyKey);

        // 根据代理key查找记录
        StreamProxyDTO streamProxyDTO = new StreamProxyDTO();
        streamProxyDTO.setProxyKey(proxyKey);
        StreamProxyDTO existingProxy = streamProxyManager.get(streamProxyDTO);
        if (existingProxy == null) {
            log.warn("根据代理key删除流代理：记录不存在 - key: {}", proxyKey);
            return true;
        }

        return deleteStreamProxyWithTermination(existingProxy);
    }

    @Override
    public boolean updateStreamProxyStatus(StreamProxyDTO statusDTO) {
        Assert.notNull(statusDTO, "状态更新DTO不能为空");
        Assert.notNull(statusDTO.getId(), "流代理ID不能为空");
        Assert.notNull(statusDTO.getStatus(), "状态不能为空");

        Long id = statusDTO.getId();
        Integer status = statusDTO.getStatus();
        log.info("开始更新流代理状态 - ID: {}, 状态: {}", id, status);

        // 获取现有记录
        StreamProxyDTO existingProxy = streamProxyManager.getById(id);
        if (existingProxy == null) {
            log.error("更新流代理状态失败：记录不存在 - ID: {}", id);
            throw new ServiceException(ServiceExceptionEnum.DATA_NOT_EXISTS, "流代理记录不存在");
        }

        if (status == 1) {
            // 启用：检查流是否在线，不在线则触发拉流代理
            log.info("启用流代理 - ID: {}, 应用: {}, 流: {}", id, existingProxy.getApp(), existingProxy.getStream());

            // 更新数据库状态
            boolean updated = streamProxyManager.updateStreamProxy(statusDTO, "启用流代理");

            if (updated) {
                try {
                    // 启动流代理
                    startStreamProxy(existingProxy);
                } catch (Exception e) {
                    log.error("启用流代理后启动失败 - ID: {}, 异常: {}", id, e.getMessage(), e);
                }
            }

            return updated;
        } else {
            // 禁用：停止流代理
            log.info("禁用流代理 - ID: {}, 应用: {}, 流: {}", id, existingProxy.getApp(), existingProxy.getStream());

            // 停止流代理
            if (existingProxy.getProxyKey() != null && existingProxy.getServerId() != null) {
                stopStreamProxyByKey(existingProxy.getServerId(), existingProxy.getProxyKey());
            }

            // 更新数据库状态
            return streamProxyManager.updateStreamProxy(statusDTO, "禁用流代理");
        }
    }

    @Override
    public boolean updateStreamProxyStatus(Long id, Integer status) {
        Assert.notNull(id, "流代理ID不能为空");
        Assert.notNull(status, "状态不能为空");

        // 构建状态更新DTO并调用主要方法
        StreamProxyDTO statusDTO = new StreamProxyDTO();
        statusDTO.setId(id);
        statusDTO.setStatus(status);

        return updateStreamProxyStatus(statusDTO);
    }

    @Override
    public boolean syncStreamProxyOnlineStatus(StreamProxyDTO syncDTO) {
        Assert.notNull(syncDTO, "同步DTO不能为空");
        Assert.notNull(syncDTO.getId(), "流代理ID不能为空");

        Long id = syncDTO.getId();
        StreamProxyDTO proxy = streamProxyManager.getById(id);
        if (proxy == null || proxy.getServerId() == null) {
            log.warn("同步流代理在线状态：记录不存在或节点ID为空 - ID: {}", id);
            return false;
        }

        boolean isOnline = checkStreamOnline(proxy);
        Integer onlineStatus = isOnline ? 1 : 0;

        // 只有状态发生变化才更新
        if (!onlineStatus.equals(proxy.getOnlineStatus())) {
            boolean updated = streamProxyManager.updateStreamProxyOnlineStatus(id, onlineStatus, "同步流代理在线状态");
            log.debug("同步流代理在线状态完成 - ID: {}, 应用: {}, 流: {}, 在线状态: {}, 更新结果: {}",
                id, proxy.getApp(), proxy.getStream(), isOnline, updated);
            return updated;
        }

        return true;
    }

    @Override
    public boolean syncStreamProxyOnlineStatus(Long id) {
        Assert.notNull(id, "流代理ID不能为空");

        // 构建同步DTO并调用主要方法
        StreamProxyDTO syncDTO = new StreamProxyDTO();
        syncDTO.setId(id);

        return syncStreamProxyOnlineStatus(syncDTO);
    }

    /**
     * 定时触发器：受 {@link #scheduledSyncEnabled} 开关控制的流代理状态同步入口。
     * <p>
     * 开关关闭时直接返回，不触碰任何数据访问层，杜绝非预期副作用；
     * 开关开启时委托给 {@link #syncAllEnabledStreamProxyStatus()} 执行实际同步。
     * </p>
     */
    @Scheduled(fixedRate = 30000) // 每30秒执行一次
    public void scheduledSyncTask() {
        if (!scheduledSyncEnabled) {
            log.debug("流代理定时同步开关已关闭，跳过本次执行（voglander.stream-proxy.scheduled-sync.enabled=false）");
            return;
        }
        syncAllEnabledStreamProxyStatus();
    }

    @Override
    public int syncAllEnabledStreamProxyStatus() {
        log.info("开始同步所有启用的流代理在线状态");

        int syncCount = 0;
        int pageSize = 100;
        int pageNum = 1;

        while (true) {
            // 分页查询启用的流代理
            Page<StreamProxyDO> page = new Page<>(pageNum, pageSize);
            QueryWrapper<StreamProxyDO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("enabled", 1);
            queryWrapper.isNotNull("server_id");
            queryWrapper.orderByAsc("id");

            // 分页查询启用的流代理
            page = streamProxyService.page(page, queryWrapper);
            List<StreamProxyDO> records = page.getRecords();

            if (CollectionUtils.isEmpty(records)) {
                // 没有更多记录
                break;
            }

            // 同步当前页的记录
            for (StreamProxyDO proxy : records) {
                try {
                    syncStreamProxyOnlineStatus(proxy.getId());
                    syncCount++;
                } catch (Exception e) {
                    log.error("同步流代理在线状态异常 - ID: {}, 应用: {}, 流: {}, 异常: {}",
                        proxy.getId(), proxy.getApp(), proxy.getStream(), e.getMessage());
                }
            }

            // 检查是否还有下一页
            if (!page.hasNext()) {
                break;
            }
            pageNum++;
        }

        log.info("同步所有启用的流代理在线状态完成 - 同步记录数: {}", syncCount);
        return syncCount;
    }

    @Override
    public boolean startStreamProxy(StreamProxyDTO startDTO) {
        Assert.notNull(startDTO, "启动DTO不能为空");
        Assert.notNull(startDTO.getId(), "流代理ID不能为空");

        Long id = startDTO.getId();
        StreamProxyDTO proxy = streamProxyManager.getById(id);
        if (proxy == null) {
            log.error("启动流代理失败：记录不存在 - ID: {}", id);
            return false;
        }

        if (proxy.getServerId() == null) {
            log.error("启动流代理失败：节点ID为空 - ID: {}", id);
            return false;
        }

        log.info("开始启动流代理 - ID: {}, 应用: {}, 流: {}, 节点: {}",
            id, proxy.getApp(), proxy.getStream(), proxy.getServerId());

        boolean isOnline = checkStreamOnline(proxy);
        if (isOnline) {
            log.info("流已在线，无需启动代理 - ID: {}, 应用: {}, 流: {}", id, proxy.getApp(), proxy.getStream());
            // 更新在线状态
            streamProxyManager.updateStreamProxyOnlineStatus(id, 1, "检测到流已在线");
            return true;
        }

        // 获取节点信息
        MediaNodeDTO node = mediaNodeManager.getDTOByServerId(proxy.getServerId());
        if (node == null || !node.getEnabled()) {
            log.error("启动流代理失败：节点不存在或未启用 - 节点ID: {}", proxy.getServerId());
            return false;
        }

        // 构建StreamProxyItem
        StreamProxyItem streamProxyItem = buildStreamProxyItem(proxy);

        // 调用ZLM API创建代理
        StreamProxyRequest request = StreamProxyRequest.of(node.getHost(), node.getSecret(), streamProxyItem);
        ResultDTO<StreamKey> result = zlmWrapperService.addStreamProxy(request);

        if (result.isSuccess() && result.getData() != null) {
            String proxyKey = result.getData().getKey();

            // 更新代理key
            streamProxyManager.updateStreamProxyKey(id, proxyKey, "启动流代理成功");

            log.info("启动流代理成功 - ID: {}, 应用: {}, 流: {}, 代理key: {}",
                id, proxy.getApp(), proxy.getStream(), proxyKey);
            return true;
        } else {
            log.error("启动流代理失败 - ID: {}, 应用: {}, 流: {}, 错误: {}",
                id, proxy.getApp(), proxy.getStream(), result.getMessage());
            return false;
        }
    }

    @Override
    public boolean startStreamProxy(Long id) {
        Assert.notNull(id, "流代理ID不能为空");

        // 构建启动DTO并调用主要方法
        StreamProxyDTO startDTO = new StreamProxyDTO();
        startDTO.setId(id);

        return startStreamProxy(startDTO);
    }

    @Override
    public boolean stopStreamProxy(StreamProxyDTO stopDTO) {
        Assert.notNull(stopDTO, "停止DTO不能为空");
        Assert.notNull(stopDTO.getId(), "流代理ID不能为空");

        Long id = stopDTO.getId();
        StreamProxyDTO proxy = streamProxyManager.getById(id);
        if (proxy == null) {
            log.warn("停止流代理：记录不存在 - ID: {}", id);
            // 记录不存在也算停止成功
            return true;
        }

        if (proxy.getProxyKey() == null || proxy.getServerId() == null) {
            log.warn("停止流代理：代理key或节点ID为空 - ID: {}, key: {}, serverId: {}",
                id, proxy.getProxyKey(), proxy.getServerId());
            // 没有代理key也算停止成功
            return true;
        }

        return stopStreamProxyByKey(proxy.getServerId(), proxy.getProxyKey());
    }

    @Override
    public boolean stopStreamProxy(Long id) {
        Assert.notNull(id, "流代理ID不能为空");

        // 构建停止DTO并调用主要方法
        StreamProxyDTO stopDTO = new StreamProxyDTO();
        stopDTO.setId(id);

        return stopStreamProxy(stopDTO);
    }

    @Override
    public boolean checkStreamOnline(StreamProxyDTO checkDTO) {
        Assert.notNull(checkDTO, "检查DTO不能为空");
        Assert.hasText(checkDTO.getServerId(), "节点ID不能为空");
        Assert.hasText(checkDTO.getApp(), "应用名称不能为空");
        Assert.hasText(checkDTO.getStream(), "流名称不能为空");

        String serverId = checkDTO.getServerId();
        String app = checkDTO.getApp();
        String stream = checkDTO.getStream();

        // 获取节点信息
        MediaNodeDTO node = mediaNodeManager.getDTOByServerId(serverId);
        if (node == null || !node.getEnabled()) {
            log.warn("检查流在线状态：节点不存在或未启用 - 节点ID: {}", serverId);
            return false;
        }

        MediaReq mediaReq = new MediaReq();
        mediaReq.setApp(app);
        mediaReq.setStream(stream);
        mediaReq.setSchema(checkDTO.getExtendObj().getSchema());
        mediaReq.setVhost(checkDTO.getExtendObj().getVhost());

        // 标准的防腐层查询模式
        try {
            MediaOnlineRequest request = MediaOnlineRequest.of(node.getHost(), node.getSecret(), mediaReq);
            ResultDTO<MediaOnlineStatus> result = zlmWrapperService.isMediaOnline(request);
            if (result == null) {
                log.error("检查流在线状态失败：ZLM服务器响应为空 - 节点: {}, 应用: {}, 流: {}", serverId, app, stream);
                return false;
            }
            if (!result.isSuccess() || result.getData() == null) {
                log.error("检查流在线状态失败 - 节点: {}, 应用: {}, 流: {}, 错误: {}",
                    serverId, app, stream, result.getMessage());
                return false;
            }
            MediaOnlineStatus data = result.getData();
            Boolean online = data.getOnline();
            log.debug("检查流在线状态完成 - 节点: {}, 应用: {}, 流: {}, 在线: {}", serverId, app, stream, online);
            return Boolean.TRUE.equals(online);
        } catch (Exception e) {
            log.error("checkStreamOnline异常 - serverId: {}, app: {}, stream: {}", serverId, app, stream, e);
            return false;
        }
    }

    @Override
    public boolean checkStreamOnline(String serverId, String app, String stream) {
        Assert.hasText(serverId, "节点ID不能为空");
        Assert.hasText(app, "应用名称不能为空");
        Assert.hasText(stream, "流名称不能为空");

        // 构建检查DTO并调用主要方法
        StreamProxyDTO checkDTO = new StreamProxyDTO();
        checkDTO.setServerId(serverId);
        checkDTO.setApp(app);
        checkDTO.setStream(stream);

        return checkStreamOnline(checkDTO);
    }

    /**
     * 判断是否需要重新创建流代理
     */
    private boolean needStreamRecreation(StreamProxyDTO existing, StreamProxyDTO update) {
        // URL变化
        if (!existing.getUrl().equals(update.getUrl())) {
            return true;
        }

        // 应用名称变化
        if (!existing.getApp().equals(update.getApp())) {
            return true;
        }

        // 流名称变化
        if (!existing.getStream().equals(update.getStream())) {
            return true;
        }

        return false;
    }

    /**
     * 根据代理key停止流代理
     */
    private boolean stopStreamProxyByKey(String serverId, String proxyKey) {
        Assert.hasText(serverId, "节点ID不能为空");
        Assert.hasText(proxyKey, "代理key不能为空");

        log.info("开始停止流代理 - 节点: {}, 代理key: {}", serverId, proxyKey);

        // 获取节点信息
        MediaNodeDTO node = mediaNodeManager.getDTOByServerId(serverId);
        if (node == null || !node.getEnabled()) {
            log.warn("停止流代理：节点不存在或未启用 - 节点ID: {}", serverId);
            return true;
        }

        // 调用ZLM API删除代理
        StreamProxyDeleteRequest request = StreamProxyDeleteRequest.of(node.getHost(), node.getSecret(), proxyKey);
        ResultDTO<StreamKey.StringDelFlag> result = zlmWrapperService.deleteStreamProxy(request);

        if (result.isSuccess()) {
            log.info("停止流代理成功 - 节点: {}, 代理key: {}", serverId, proxyKey);
            return true;
        } else {
            log.error("停止流代理失败 - 节点: {}, 代理key: {}, 错误: {}", serverId, proxyKey, result.getMessage());
            return false;
        }
    }

    /**
     * 构建StreamProxyItem对象
     */
    private StreamProxyItem buildStreamProxyItem(StreamProxyDTO proxy) {
        Assert.notNull(proxy, "流代理配置不能为空");
        Assert.hasText(proxy.getApp(), "应用名称不能为空");
        Assert.hasText(proxy.getStream(), "流名称不能为空");
        Assert.hasText(proxy.getUrl(), "拉流地址不能为空");

        // 直接通过JSON序列化方式构建StreamProxyItem
        StreamProxyItem streamProxyItem;

        if (proxy.getExtend() != null) {
            // 如果有扩展对象，直接序列化为StreamProxyItem
            streamProxyItem = JSON.parseObject(proxy.getExtend(), StreamProxyItem.class, JSONReader.Feature.SupportSmartMatch);

            // 设置基本字段
            streamProxyItem.setApp(proxy.getApp());
            streamProxyItem.setStream(proxy.getStream());
            streamProxyItem.setUrl(proxy.getUrl());

            log.debug("扩展字段处理完成 - 扩展JSON: {}", proxy.getExtend());
        } else {
            // 没有扩展对象，直接创建基本对象
            streamProxyItem = new StreamProxyItem();
            streamProxyItem.setVHost("__defaultVhost__");
            streamProxyItem.setApp(proxy.getApp());
            streamProxyItem.setStream(proxy.getStream());
            streamProxyItem.setUrl(proxy.getUrl());
        }

        log.debug("构建StreamProxyItem完成 - 应用: {}, 流: {}, 地址: {}",
            proxy.getApp(), proxy.getStream(), proxy.getUrl());

        return streamProxyItem;
    }
}
