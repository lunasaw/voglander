package io.github.lunasaw.voglander.manager.manager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.manager.assembler.StreamProxyAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.manager.service.StreamProxyService;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
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