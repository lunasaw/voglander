package io.github.lunasaw.voglander.manager.manager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.manager.assembler.StreamProxyAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.manager.service.StreamProxyService;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
import io.github.lunasaw.zlm.api.ZlmRestService;
import io.github.lunasaw.zlm.entity.StreamProxyItem;
import io.github.lunasaw.zlm.entity.ServerResponse;
import io.github.lunasaw.zlm.entity.StreamKey;
import lombok.extern.slf4j.Slf4j;

/**
 * 拉流代理管理器
 * 负责处理拉流代理相关的复杂业务逻辑
 *
 * <p>
 * 架构设计：
 * </p>
 * <ul>
 * <li>统一修改入口：{@link #updateStreamProxyInternal(StreamProxyDO, String)} - 所有修改操作的核心方法</li>
 * <li>统一删除入口：{@link #deleteStreamProxyInternal(Long, String)} - 所有删除操作的核心方法</li>
 * <li>统一缓存管理：{@link #clearProxyCache(Long, String, String)} - 统一的缓存清理逻辑</li>
 * </ul>
 *
 * <p>
 * 优势：
 * </p>
 * <ul>
 * <li>缓存一致性：所有修改和删除操作都通过统一入口，确保缓存清理的一致性</li>
 * <li>日志规范：统一的日志记录格式和内容</li>
 * <li>异常处理：统一的数据校验和异常处理逻辑</li>
 * <li>维护便捷：业务逻辑变更只需修改核心方法</li>
 * </ul>
 *
 * @author luna
 * @since 2025-01-23
 */
@Slf4j
@Component
public class StreamProxyManager {

    @Autowired
    private StreamProxyService   streamProxyService;

    @Autowired
    private StreamProxyAssembler streamProxyAssembler;

    @Autowired
    private CacheManager         cacheManager;

    // TODO: 这些配置应该从配置文件读取，暂时硬编码用于演示
    private static final String  DEFAULT_ZLM_HOST   = "127.0.0.1:80";
    private static final String  DEFAULT_ZLM_SECRET = "035c73f7-bb6b-4889-a715-d9eb2d1925cc";

    /**
     * 统一缓存清理方法
     *
     * @param proxyId 代理数据库ID
     * @param oldProxyKey 原代理key（可能为空）
     * @param newProxyKey 新代理key（可能为空）
     */
    private void clearProxyCache(Long proxyId, String oldProxyKey, String newProxyKey) {
        if (proxyId != null) {
            Optional.ofNullable(cacheManager.getCache("streamProxy")).ifPresent(e -> e.evict(proxyId));
        }
        if (oldProxyKey != null) {
            Optional.ofNullable(cacheManager.getCache("streamProxy")).ifPresent(e -> e.evict("key:" + oldProxyKey));
        }
        if (newProxyKey != null && !newProxyKey.equals(oldProxyKey)) {
            Optional.ofNullable(cacheManager.getCache("streamProxy")).ifPresent(e -> e.evict("key:" + newProxyKey));
        }
    }

    /**
     * 创建拉流代理
     *
     * @param streamProxyDTO 代理信息
     * @return 代理ID
     */
    public Long createStreamProxy(StreamProxyDTO streamProxyDTO) {
        Assert.notNull(streamProxyDTO, "代理信息不能为空");
        Assert.hasText(streamProxyDTO.getApp(), "应用名称不能为空");
        Assert.hasText(streamProxyDTO.getStream(), "流ID不能为空");
        Assert.hasText(streamProxyDTO.getUrl(), "拉流地址不能为空");

        // 设置默认值
        if (streamProxyDTO.getEnabled() == null) {
            streamProxyDTO.setEnabled(true);
        }
        if (streamProxyDTO.getStatus() == null) {
            streamProxyDTO.setStatus(1); // 默认启用
        }
        if (streamProxyDTO.getOnlineStatus() == null) {
            streamProxyDTO.setOnlineStatus(0); // 默认离线
        }

        StreamProxyDO streamProxyDO = streamProxyAssembler.dtoToDo(streamProxyDTO);
        streamProxyDO.setCreateTime(LocalDateTime.now());
        streamProxyDO.setUpdateTime(LocalDateTime.now());

        return updateStreamProxyInternal(streamProxyDO, "创建拉流代理");
    }

    /**
     * 创建拉流代理（支持ZLM集成）
     * <p>
     * 实现数据库优先 + ZLM集成的创建流程：
     * 1. 先在数据库中创建代理记录
     * 2. 从扩展字段解析ZLM参数
     * 3. 调用ZLM API创建代理拉流
     * 4. 根据返回的key更新数据库记录
     * </p>
     *
     * @param streamProxyDTO 代理信息（包含ZLM参数的扩展字段）
     * @return 代理ID
     */
    public Long createWithZlmIntegration(StreamProxyDTO streamProxyDTO) {
        Assert.notNull(streamProxyDTO, "代理信息不能为空");
        Assert.hasText(streamProxyDTO.getApp(), "应用名称不能为空");
        Assert.hasText(streamProxyDTO.getStream(), "流ID不能为空");
        Assert.hasText(streamProxyDTO.getUrl(), "拉流地址不能为空");

        log.info("开始创建代理拉流（ZLM集成）- app: {}, stream: {}, url: {}",
            streamProxyDTO.getApp(), streamProxyDTO.getStream(), streamProxyDTO.getUrl());

        // 设置默认值
        if (streamProxyDTO.getEnabled() == null) {
            streamProxyDTO.setEnabled(true);
        }
        if (streamProxyDTO.getStatus() == null) {
            streamProxyDTO.setStatus(1); // 默认启用
        }
        if (streamProxyDTO.getOnlineStatus() == null) {
            streamProxyDTO.setOnlineStatus(0); // 默认离线，等待ZLM回调更新
        }

        // 第一步：先在数据库中创建记录
        StreamProxyDO streamProxyDO = streamProxyAssembler.dtoToDo(streamProxyDTO);
        streamProxyDO.setCreateTime(LocalDateTime.now());
        streamProxyDO.setUpdateTime(LocalDateTime.now());

        Long proxyId = updateStreamProxyInternal(streamProxyDO, "数据库创建代理记录");

        try {
            // 第二步：构建StreamProxyItem并调用ZLM API
            log.info("调用ZLM API创建代理拉流 - host: {}, app: {}, stream: {}",
                DEFAULT_ZLM_HOST, streamProxyDTO.getApp(), streamProxyDTO.getStream());

            StreamProxyItem streamProxyItem = buildStreamProxyItemFromExtension(streamProxyDTO);

            ServerResponse<StreamKey> response = ZlmRestService.addStreamProxy(
                DEFAULT_ZLM_HOST,
                DEFAULT_ZLM_SECRET,
                streamProxyItem);

            if (response != null && response.getCode() != null && response.getCode() == 0 && response.getData() != null) {
                // 第三步：根据返回的key更新数据库记录
                String proxyKey = response.getData().getKey();
                log.info("ZLM代理创建成功，更新数据库proxyKey - proxyId: {}, proxyKey: {}", proxyId, proxyKey);

                StreamProxyDO updatedProxy = streamProxyService.getById(proxyId);
                if (updatedProxy != null) {
                    updatedProxy.setProxyKey(proxyKey);
                    updatedProxy.setUpdateTime(LocalDateTime.now());
                    updateStreamProxyInternal(updatedProxy, "更新ZLM代理key");
                }

                log.info("代理拉流创建完成（ZLM集成）- proxyId: {}, proxyKey: {}", proxyId, proxyKey);
            } else {
                // ZLM API调用失败，记录错误但不删除数据库记录（允许后续手动处理）
                String errorMsg = response != null ? response.getMsg() : "ZLM API响应为空";
                log.error("ZLM代理创建失败 - proxyId: {}, 错误: {}", proxyId, errorMsg);

                // 更新数据库状态为异常，但保留记录
                StreamProxyDO updatedProxy = streamProxyService.getById(proxyId);
                if (updatedProxy != null) {
                    updatedProxy.setStatus(0); // 设置为异常状态
                    updatedProxy.setUpdateTime(LocalDateTime.now());
                    updateStreamProxyInternal(updatedProxy, "ZLM创建失败，更新状态为异常");
                }

                // 抛出业务异常，但数据库记录已保存，可以后续处理
                throw new RuntimeException("ZLM代理创建失败: " + errorMsg);
            }

        } catch (Exception e) {
            log.error("ZLM集成异常 - proxyId: {}, 错误: {}", proxyId, e.getMessage(), e);

            // 更新数据库状态但不删除记录，允许后续手动处理
            try {
                StreamProxyDO updatedProxy = streamProxyService.getById(proxyId);
                if (updatedProxy != null) {
                    updatedProxy.setStatus(0); // 设置为异常状态
                    updatedProxy.setUpdateTime(LocalDateTime.now());
                    updateStreamProxyInternal(updatedProxy, "ZLM集成异常，更新状态");
                }
            } catch (Exception updateException) {
                log.error("更新异常状态失败 - proxyId: {}, 错误: {}", proxyId, updateException.getMessage());
            }

            throw new RuntimeException("代理创建失败: " + e.getMessage(), e);
        }

        return proxyId;
    }

    /**
     * 从DTO扩展字段构建StreamProxyItem
     * <p>
     * 解析JSON格式的扩展字段，映射到ZLM API所需的StreamProxyItem对象
     * </p>
     *
     * @param streamProxyDTO DTO对象，包含扩展字段
     * @return StreamProxyItem对象
     */
    private StreamProxyItem buildStreamProxyItemFromExtension(StreamProxyDTO streamProxyDTO) {
        StreamProxyItem item = new StreamProxyItem();

        // 设置基础字段
        item.setApp(streamProxyDTO.getApp());
        item.setStream(streamProxyDTO.getStream());
        item.setUrl(streamProxyDTO.getUrl());

        // 设置默认虚拟主机
        item.setVHost("__defaultVhost__");

        // 解析扩展字段中的ZLM参数
        if (streamProxyDTO.getExtend() != null && !streamProxyDTO.getExtend().trim().isEmpty()) {
            try {
                Map<String, Object> zlmParams = JSON.parseObject(streamProxyDTO.getExtend(), Map.class);

                // 映射ZLM参数到StreamProxyItem
                setIfExists(zlmParams, "vhost", String.class, item::setVHost);
                setIfExists(zlmParams, "retry_count", Integer.class, item::setRetryCount);
                setIfExists(zlmParams, "rtp_type", Integer.class, item::setRtpType);
                setIfExists(zlmParams, "timeout_sec", Integer.class, item::setTimeoutSec);

                // 协议启用参数
                setIfExists(zlmParams, "enable_hls", Boolean.class, item::setEnableHls);
                setIfExists(zlmParams, "enable_hls_fmp4", Boolean.class, item::setEnableHlsFmp4);
                setIfExists(zlmParams, "enable_mp4", Boolean.class, item::setEnableMp4);
                setIfExists(zlmParams, "enable_rtsp", Boolean.class, item::setEnableRtsp);
                setIfExists(zlmParams, "enable_rtmp", Boolean.class, item::setEnableRtmp);
                setIfExists(zlmParams, "enable_ts", Boolean.class, item::setEnableTs);
                setIfExists(zlmParams, "enable_fmp4", Boolean.class, item::setEnableFmp4);

                // 按需生成参数
                setIfExists(zlmParams, "hls_demand", Boolean.class, item::setHlsDemand);
                setIfExists(zlmParams, "rtsp_demand", Boolean.class, item::setRtspDemand);
                setIfExists(zlmParams, "rtmp_demand", Boolean.class, item::setRtmpDemand);
                setIfExists(zlmParams, "ts_demand", Boolean.class, item::setTsDemand);
                setIfExists(zlmParams, "fmp4_demand", Boolean.class, item::setFmp4Demand);

                // 音频参数
                setIfExists(zlmParams, "enable_audio", Boolean.class, item::setEnableAudio);
                setIfExists(zlmParams, "add_mute_audio", Boolean.class, item::setAddMuteAudio);

                // MP4参数
                setIfExists(zlmParams, "mp4_save_path", String.class, item::setMp4SavePath);
                setIfExists(zlmParams, "mp4_max_second", Integer.class, item::setMp4MaxSecond);
                setIfExists(zlmParams, "mp4_as_player", Boolean.class, item::setMp4AsPlayer);

                // HLS参数
                setIfExists(zlmParams, "hls_save_path", String.class, item::setHlsSavePath);

                // 高级参数
                setIfExists(zlmParams, "modify_stamp", Integer.class, item::setModifyStamp);
                setIfExists(zlmParams, "auto_close", Boolean.class, item::setAutoClose);

                log.debug("ZLM参数映射完成 - 扩展参数数量: {}", zlmParams.size());

            } catch (Exception e) {
                log.warn("解析ZLM扩展参数失败，使用默认值 - app: {}, stream: {}, 错误: {}",
                    streamProxyDTO.getApp(), streamProxyDTO.getStream(), e.getMessage());
            }
        }

        return item;
    }

    /**
     * 辅助方法：如果Map中存在指定键且类型匹配，则设置到目标对象
     */
    private <T> void setIfExists(Map<String, Object> map, String key, Class<T> type, java.util.function.Consumer<T> setter) {
        Object value = map.get(key);
        if (value != null && type.isInstance(value)) {
            setter.accept(type.cast(value));
        }
    }

    /**
     * 更新拉流代理状态
     *
     * @param proxyKey 代理key
     * @param onlineStatus 在线状态
     * @param operationDesc 操作描述
     * @return 代理ID
     */
    public Long updateProxyOnlineStatus(String proxyKey, Integer onlineStatus, String operationDesc) {
        Assert.hasText(proxyKey, "代理key不能为空");
        Assert.notNull(onlineStatus, "在线状态不能为空");

        StreamProxyDO existingProxy = getByProxyKey(proxyKey);
        if (existingProxy == null) {
            log.warn("代理不存在，创建新的代理记录: proxyKey={}", proxyKey);
            return null;
        }

        existingProxy.setOnlineStatus(onlineStatus);
        existingProxy.setUpdateTime(LocalDateTime.now());

        return updateStreamProxyInternal(existingProxy, operationDesc);
    }

    /**
     * 更新拉流代理在线状态（根据应用名称和流ID）
     *
     * @param app 应用名称
     * @param stream 流ID
     * @param onlineStatus 在线状态
     * @param extend 扩展信息
     * @return 代理ID，如果代理不存在则返回null
     */
    public Long updateStreamProxyOnlineStatus(String app, String stream, Integer onlineStatus, String extend) {
        Assert.hasText(app, "应用名称不能为空");
        Assert.hasText(stream, "流ID不能为空");
        Assert.notNull(onlineStatus, "在线状态不能为空");

        StreamProxyDO existingProxy = getByAppAndStream(app, stream);
        if (existingProxy == null) {
            log.warn("流代理不存在，无法更新在线状态 - app: {}, stream: {}", app, stream);
            return null;
        }

        existingProxy.setOnlineStatus(onlineStatus);
        existingProxy.setUpdateTime(LocalDateTime.now());

        // 更新扩展信息（如果提供）
        if (extend != null) {
            existingProxy.setExtend(extend);
        }

        String operationDesc = String.format("更新流在线状态为%s", onlineStatus == 1 ? "在线" : "离线");
        return updateStreamProxyInternal(existingProxy, operationDesc);
    }

    /**
     * 保存或更新拉流代理（用于Hook回调）
     *
     * @param app 应用名称
     * @param stream 流ID
     * @param url 拉流地址
     * @param proxyKey 代理key
     * @param onlineStatus 在线状态
     * @param extend 扩展信息
     * @return 代理ID
     */
    public Long saveOrUpdateProxy(String app, String stream, String url, String proxyKey, Integer onlineStatus, String extend) {
        Assert.hasText(app, "应用名称不能为空");
        Assert.hasText(stream, "流ID不能为空");
        Assert.hasText(url, "拉流地址不能为空");
        Assert.hasText(proxyKey, "代理key不能为空");

        // 先根据proxyKey查找现有记录
        StreamProxyDO existingProxy = getByProxyKey(proxyKey);

        if (existingProxy != null) {
            // 存在则更新
            existingProxy.setOnlineStatus(onlineStatus != null ? onlineStatus : 1);
            existingProxy.setUpdateTime(LocalDateTime.now());
            if (extend != null) {
                existingProxy.setExtend(extend);
            }
            log.info("更新拉流代理: proxyKey={}, onlineStatus={}", proxyKey, onlineStatus);
            return updateStreamProxyInternal(existingProxy, "Hook回调更新代理状态");
        } else {
            // 不存在则创建
            StreamProxyDO newProxy = new StreamProxyDO();
            newProxy.setApp(app);
            newProxy.setStream(stream);
            newProxy.setUrl(url);
            newProxy.setProxyKey(proxyKey);
            newProxy.setStatus(1); // 默认启用
            newProxy.setOnlineStatus(onlineStatus != null ? onlineStatus : 1);
            newProxy.setEnabled(true);
            newProxy.setExtend(extend);
            newProxy.setCreateTime(LocalDateTime.now());
            newProxy.setUpdateTime(LocalDateTime.now());

            log.info("创建拉流代理: app={}, stream={}, url={}, proxyKey={}", app, stream, url, proxyKey);
            return updateStreamProxyInternal(newProxy, "Hook回调创建代理");
        }
    }

    /**
     * 根据ID获取拉流代理
     *
     * @param id 代理ID
     * @return 代理信息
     */
    public StreamProxyDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return streamProxyService.getById(id);
    }

    /**
     * 更新拉流代理（公开方法）
     *
     * @param streamProxyDO 代理信息
     * @param operationDesc 操作描述
     * @return 代理ID
     */
    public Long updateStreamProxy(StreamProxyDO streamProxyDO, String operationDesc) {
        return updateStreamProxyInternal(streamProxyDO, operationDesc);
    }

    /**
     * DO转DTO
     *
     * @param streamProxyDO DO对象
     * @return DTO对象
     */
    public StreamProxyDTO doToDto(StreamProxyDO streamProxyDO) {
        return streamProxyAssembler.doToDto(streamProxyDO);
    }

    /**
     * 根据代理key获取代理信息
     *
     * @param proxyKey 代理key
     * @return 代理信息
     */
    public StreamProxyDO getByProxyKey(String proxyKey) {
        if (proxyKey == null) {
            return null;
        }
        QueryWrapper<StreamProxyDO> wrapper = new QueryWrapper<>();
        wrapper.eq("proxy_key", proxyKey);
        return streamProxyService.getOne(wrapper);
    }

    /**
     * 根据应用和流名获取代理信息
     *
     * @param app 应用名称
     * @param stream 流ID
     * @return 代理信息
     */
    public StreamProxyDO getByAppAndStream(String app, String stream) {
        if (app == null || stream == null) {
            return null;
        }
        QueryWrapper<StreamProxyDO> wrapper = new QueryWrapper<>();
        wrapper.eq("app", app).eq("stream", stream);
        return streamProxyService.getOne(wrapper);
    }

    /**
     * 根据应用名称获取所有代理
     *
     * @param app 应用名称
     * @return 代理列表
     */
    public List<StreamProxyDO> getProxyByApp(String app) {
        if (app == null) {
            return new ArrayList<>();
        }
        QueryWrapper<StreamProxyDO> wrapper = new QueryWrapper<>();
        wrapper.eq("app", app);
        return streamProxyService.list(wrapper);
    }

    /**
     * 分页查询拉流代理
     *
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    public Page<StreamProxyDO> getProxyPage(int page, int size) {
        Page<StreamProxyDO> pageInfo = new Page<>(page, size);
        return streamProxyService.page(pageInfo);
    }

    /**
     * 删除拉流代理
     *
     * @param id 代理ID
     * @param operationDesc 操作描述
     * @return 是否成功
     */
    public boolean deleteStreamProxy(Long id, String operationDesc) {
        Assert.notNull(id, "代理ID不能为空");
        return deleteStreamProxyInternal(id, operationDesc);
    }

    /**
     * 根据代理key删除拉流代理
     *
     * @param proxyKey 代理key
     * @param operationDesc 操作描述
     * @return 是否成功
     */
    public boolean deleteByProxyKey(String proxyKey, String operationDesc) {
        Assert.hasText(proxyKey, "代理key不能为空");

        StreamProxyDO existingProxy = getByProxyKey(proxyKey);
        if (existingProxy == null) {
            log.warn("代理不存在，无法删除: proxyKey={}", proxyKey);
            return false;
        }

        return deleteStreamProxyInternal(existingProxy.getId(), operationDesc);
    }

    /**
     * 统一修改入口：处理所有拉流代理的修改操作
     * 
     * @param streamProxyDO 拉流代理数据
     * @param operationDesc 操作描述，用于日志记录
     * @return 拉流代理ID
     */
    private Long updateStreamProxyInternal(StreamProxyDO streamProxyDO, String operationDesc) {
        try {
            log.info("{}开始 - 代理信息: app={}, stream={}, proxyKey={}",
                operationDesc, streamProxyDO.getApp(), streamProxyDO.getStream(), streamProxyDO.getProxyKey());

            String oldProxyKey = null;
            StreamProxyDO targetProxy = null;

            // 首先根据ID查询（如果有ID的话）
            if (streamProxyDO.getId() != null) {
                targetProxy = streamProxyService.getById(streamProxyDO.getId());
                if (targetProxy != null) {
                    oldProxyKey = targetProxy.getProxyKey();
                }
            }

            // 如果没有ID或者根据ID查询不到，则根据app+stream查询现有记录
            if (targetProxy == null && streamProxyDO.getApp() != null && streamProxyDO.getStream() != null) {
                targetProxy = getByAppAndStream(streamProxyDO.getApp(), streamProxyDO.getStream());
                if (targetProxy != null) {
                    oldProxyKey = targetProxy.getProxyKey();
                    // 找到现有记录，更新ID以确保正确的更新操作
                    streamProxyDO.setId(targetProxy.getId());
                    log.info("根据app+stream找到现有代理记录，将进行更新 - ID: {}, app={}, stream={}",
                        targetProxy.getId(), streamProxyDO.getApp(), streamProxyDO.getStream());
                }
            }

            boolean success = streamProxyService.saveOrUpdate(streamProxyDO);
            if (!success) {
                throw new RuntimeException("数据库操作失败");
            }

            // 清理缓存
            clearProxyCache(streamProxyDO.getId(), oldProxyKey, streamProxyDO.getProxyKey());

            log.info("{}成功 - 代理ID: {}, app={}, stream={}, proxyKey={}",
                operationDesc, streamProxyDO.getId(), streamProxyDO.getApp(), streamProxyDO.getStream(), streamProxyDO.getProxyKey());

            return streamProxyDO.getId();
        } catch (Exception e) {
            log.error("{}失败 - 代理信息: app={}, stream={}, proxyKey={}, 错误: {}",
                operationDesc, streamProxyDO.getApp(), streamProxyDO.getStream(), streamProxyDO.getProxyKey(), e.getMessage(), e);
            throw new RuntimeException(operationDesc + "失败: " + e.getMessage(), e);
        }
    }

    /**
     * 统一删除入口：处理所有拉流代理的删除操作
     * 
     * @param proxyId 拉流代理ID
     * @param operationDesc 操作描述，用于日志记录
     * @return 是否成功
     */
    private boolean deleteStreamProxyInternal(Long proxyId, String operationDesc) {
        try {
            log.info("{}开始 - 代理ID: {}", operationDesc, proxyId);

            StreamProxyDO existingProxy = streamProxyService.getById(proxyId);
            if (existingProxy == null) {
                log.warn("代理不存在，无法删除 - 代理ID: {}", proxyId);
                return false;
            }

            boolean success = streamProxyService.removeById(proxyId);
            if (!success) {
                throw new RuntimeException("数据库删除失败");
            }

            // 清理缓存
            clearProxyCache(proxyId, existingProxy.getProxyKey(), null);

            log.info("{}成功 - 代理ID: {}, app={}, stream={}, proxyKey={}",
                operationDesc, proxyId, existingProxy.getApp(), existingProxy.getStream(), existingProxy.getProxyKey());

            return true;
        } catch (Exception e) {
            log.error("{}失败 - 代理ID: {}, 错误: {}", operationDesc, proxyId, e.getMessage(), e);
            throw new RuntimeException(operationDesc + "失败: " + e.getMessage(), e);
        }
    }
}