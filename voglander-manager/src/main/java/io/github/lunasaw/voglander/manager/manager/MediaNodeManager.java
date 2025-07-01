package io.github.lunasaw.voglander.manager.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.manager.assembler.MediaNodeAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaNodeDTO;
import io.github.lunasaw.voglander.manager.service.MediaNodeService;
import io.github.lunasaw.voglander.repository.entity.MediaNodeDO;

import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * 流媒体节点管理器
 * 负责处理流媒体节点相关的复杂业务逻辑
 *
 * @author luna
 * @since 2025-01-23
 */
@Slf4j
@Component
public class MediaNodeManager {

    @Autowired
    private MediaNodeService mediaNodeService;

    @Autowired
    private MediaNodeAssembler mediaNodeAssembler;

    @Autowired
    private CacheManager cacheManager;

    /**
     * 创建流媒体节点
     *
     * @param mediaNodeDTO 节点信息
     * @return 节点ID
     */
    public Long createMediaNode(MediaNodeDTO mediaNodeDTO) {
        Assert.notNull(mediaNodeDTO, "节点信息不能为空");
        Assert.hasText(mediaNodeDTO.getServerId(), "节点ID不能为空");
        Assert.hasText(mediaNodeDTO.getHost(), "节点地址不能为空");

        // 检查节点ID是否已存在
        MediaNodeDO existingNode = getByServerId(mediaNodeDTO.getServerId());
        Assert.isNull(existingNode, "节点ID已存在: " + mediaNodeDTO.getServerId());

        // 设置默认值
        if (mediaNodeDTO.getEnabled() == null) {
            mediaNodeDTO.setEnabled(true);
        }
        if (mediaNodeDTO.getHookEnabled() == null) {
            mediaNodeDTO.setHookEnabled(true);
        }
        if (mediaNodeDTO.getWeight() == null) {
            mediaNodeDTO.setWeight(0);
        }
        if (mediaNodeDTO.getStatus() == null) {
            mediaNodeDTO.setStatus(0); // 默认离线
        }

        MediaNodeDO mediaNodeDO = mediaNodeAssembler.toMediaNodeDO(mediaNodeDTO);
        mediaNodeDO.setCreateTime(new Date());
        mediaNodeDO.setUpdateTime(new Date());

        boolean saved = mediaNodeService.save(mediaNodeDO);
        Assert.isTrue(saved, "节点创建失败");

        log.info("成功创建流媒体节点，节点ID: {}, 数据库ID: {}", mediaNodeDTO.getServerId(), mediaNodeDO.getId());
        return mediaNodeDO.getId();
    }

    /**
     * 批量创建流媒体节点
     *
     * @param mediaNodeDTOList 节点信息列表
     * @return 成功创建的数量
     */
    public int batchCreateMediaNode(List<MediaNodeDTO> mediaNodeDTOList) {
        if (mediaNodeDTOList == null || mediaNodeDTOList.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (MediaNodeDTO dto : mediaNodeDTOList) {
            try {
                createMediaNode(dto);
                successCount++;
            } catch (Exception e) {
                log.error("批量创建节点失败，节点ID: {}, 错误: {}", dto.getServerId(), e.getMessage());
            }
        }

        log.info("批量创建流媒体节点完成，成功: {}, 总数: {}", successCount, mediaNodeDTOList.size());
        return successCount;
    }

    /**
     * 更新流媒体节点
     *
     * @param mediaNodeDTO 节点信息
     * @return 节点ID
     */
    @CacheEvict(value = "mediaNode", key = "#mediaNodeDTO.id")
    public Long updateMediaNode(MediaNodeDTO mediaNodeDTO) {
        Assert.notNull(mediaNodeDTO, "节点信息不能为空");
        Assert.notNull(mediaNodeDTO.getId(), "节点数据库ID不能为空");

        MediaNodeDO existingNode = mediaNodeService.getById(mediaNodeDTO.getId());
        Assert.notNull(existingNode, "节点不存在，ID: " + mediaNodeDTO.getId());

        MediaNodeDO mediaNodeDO = mediaNodeAssembler.toMediaNodeDO(mediaNodeDTO);
        mediaNodeDO.setUpdateTime(new Date());

        boolean updated = mediaNodeService.updateById(mediaNodeDO);
        Assert.isTrue(updated, "节点更新失败");

        log.info("成功更新流媒体节点，节点ID: {}, 数据库ID: {}", mediaNodeDTO.getId(), mediaNodeDTO.getId());
        return mediaNodeDTO.getId();
    }

    /**
     * 批量更新流媒体节点
     *
     * @param mediaNodeDTOList 节点信息列表
     * @return 成功更新的数量
     */
    public int batchUpdateMediaNode(List<MediaNodeDTO> mediaNodeDTOList) {
        if (mediaNodeDTOList == null || mediaNodeDTOList.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (MediaNodeDTO dto : mediaNodeDTOList) {
            try {
                updateMediaNode(dto);
                successCount++;
            } catch (Exception e) {
                log.error("批量更新节点失败，节点ID: {}, 错误: {}", dto.getServerId(), e.getMessage());
            }
        }

        log.info("批量更新流媒体节点完成，成功: {}, 总数: {}", successCount, mediaNodeDTOList.size());
        return successCount;
    }

    /**
     * 根据节点ID获取节点
     *
     * @param serverId 节点ID
     * @return MediaNodeDO
     */
    public MediaNodeDO getByServerId(String serverId) {
        if (serverId == null || serverId.trim().isEmpty()) {
            return null;
        }
        QueryWrapper<MediaNodeDO> query = new QueryWrapper<>();
        query.eq("server_id", serverId);
        return mediaNodeService.getOne(query);
    }

    /**
     * 分页查询节点
     *
     * @param page 页码
     * @param size 每页大小
     * @param query 查询条件
     * @return 分页结果
     */
    public Page<MediaNodeDTO> pageQuery(int page, int size, QueryWrapper<MediaNodeDO> query) {
        Page<MediaNodeDO> pageParam = new Page<>(page, size);
        Page<MediaNodeDO> pageResult = mediaNodeService.page(pageParam, query);

        Page<MediaNodeDTO> dtoPage = new Page<>(page, size);
        dtoPage.setTotal(pageResult.getTotal());
        dtoPage.setCurrent(pageResult.getCurrent());
        dtoPage.setSize(pageResult.getSize());
        dtoPage.setPages(pageResult.getPages());

        List<MediaNodeDTO> dtoList = mediaNodeAssembler.toMediaNodeDTOList(pageResult.getRecords());
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }

    /**
     * 简单分页查询
     *
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    public Page<MediaNodeDTO> pageQuerySimple(int page, int size) {
        return pageQuery(page, size, new QueryWrapper<>());
    }

    /**
     * 根据节点ID获取节点DTO（带缓存）
     *
     * @param serverId 节点ID
     * @return MediaNodeDTO
     */
    @Cacheable(value = "mediaNode", key = "#serverId", unless = "#result == null")
    public MediaNodeDTO getDTOByServerId(String serverId) {
        MediaNodeDO byServerId = getByServerId(serverId);
        return mediaNodeAssembler.toMediaNodeDTO(byServerId);
    }

    /**
     * 根据数据库ID获取节点DTO
     *
     * @param id 数据库主键ID
     * @return MediaNodeDTO
     */
    public MediaNodeDTO getMediaNodeDTOById(Long id) {
        MediaNodeDO mediaNodeDO = mediaNodeService.getById(id);
        return mediaNodeAssembler.toMediaNodeDTO(mediaNodeDO);
    }

    /**
     * 根据实体条件获取单个节点DTO
     *
     * @param mediaNode 查询条件
     * @return MediaNodeDTO
     */
    public MediaNodeDTO getMediaNodeDTOByEntity(MediaNodeDO mediaNode) {
        QueryWrapper<MediaNodeDO> query = new QueryWrapper<>();
        if (mediaNode.getServerId() != null) {
            query.eq("server_id", mediaNode.getServerId());
        }
        if (mediaNode.getName() != null) {
            query.eq("name", mediaNode.getName());
        }
        if (mediaNode.getHost() != null) {
            query.eq("host", mediaNode.getHost());
        }
        if (mediaNode.getEnabled() != null) {
            query.eq("enabled", mediaNode.getEnabled());
        }
        if (mediaNode.getStatus() != null) {
            query.eq("status", mediaNode.getStatus());
        }

        MediaNodeDO mediaNodeDO = mediaNodeService.getOne(query);
        return mediaNodeAssembler.toMediaNodeDTO(mediaNodeDO);
    }

    /**
     * 根据条件查询节点DTO列表
     *
     * @param mediaNode 查询条件
     * @return MediaNodeDTO列表
     */
    public List<MediaNodeDTO> listMediaNodeDTO(MediaNodeDO mediaNode) {
        QueryWrapper<MediaNodeDO> query = new QueryWrapper<>();
        if (mediaNode.getServerId() != null) {
            query.eq("server_id", mediaNode.getServerId());
        }
        if (mediaNode.getName() != null) {
            query.like("name", mediaNode.getName());
        }
        if (mediaNode.getHost() != null) {
            query.like("host", mediaNode.getHost());
        }
        if (mediaNode.getEnabled() != null) {
            query.eq("enabled", mediaNode.getEnabled());
        }
        if (mediaNode.getStatus() != null) {
            query.eq("status", mediaNode.getStatus());
        }

        List<MediaNodeDO> mediaNodeDOList = mediaNodeService.list(query);
        return mediaNodeAssembler.toMediaNodeDTOList(mediaNodeDOList);
    }

    /**
     * 更新节点在线状态
     *
     * @param serverId 节点ID
     * @param status 状态 1在线 0离线
     * @param keepalive 心跳时间戳
     */
    @CacheEvict(value = "mediaNode", key = "#serverId")
    public void updateNodeStatus(String serverId, Integer status, Long keepalive) {
        MediaNodeDO node = getByServerId(serverId);
        if (node != null) {
            node.setStatus(status);
            node.setKeepalive(keepalive);
            node.setUpdateTime(new Date());
            mediaNodeService.updateById(node);

            log.info("更新节点状态，节点ID: {}, 状态: {}", serverId, status);
        }
    }

    /**
     * 获取所有启用的节点
     *
     * @return 启用的节点列表
     */
    public List<MediaNodeDTO> getEnabledNodes() {
        QueryWrapper<MediaNodeDO> query = new QueryWrapper<>();
        query.eq("enabled", true);
        query.orderByDesc("weight").orderByAsc("create_time");

        List<MediaNodeDO> mediaNodeDOList = mediaNodeService.list(query);
        return mediaNodeAssembler.toMediaNodeDTOList(mediaNodeDOList);
    }

    /**
     * 获取所有在线的节点
     *
     * @return 在线的节点列表
     */
    public List<MediaNodeDTO> getOnlineNodes() {
        QueryWrapper<MediaNodeDO> query = new QueryWrapper<>();
        query.eq("enabled", true);
        query.eq("status", 1);
        query.orderByDesc("weight").orderByAsc("create_time");

        List<MediaNodeDO> mediaNodeDOList = mediaNodeService.list(query);
        return mediaNodeAssembler.toMediaNodeDTOList(mediaNodeDOList);
    }

    /**
     * 根据数据库ID删除节点
     *
     * @param id 数据库主键ID
     * @return 是否删除成功
     */
    @CacheEvict(value = "mediaNode", allEntries = true)
    public boolean deleteMediaNodeById(Long id) {
        Assert.notNull(id, "节点ID不能为空");

        MediaNodeDO existingNode = mediaNodeService.getById(id);
        Assert.notNull(existingNode, "节点不存在，ID: " + id);

        // 清除对应的缓存
        if (existingNode.getServerId() != null) {
            cacheManager.getCache("mediaNode").evict(existingNode.getServerId());
        }

        boolean removed = mediaNodeService.removeById(id);
        Assert.isTrue(removed, "节点删除失败");

        log.info("成功删除流媒体节点，节点ID: {}, 数据库ID: {}", existingNode.getServerId(), id);
        return true;
    }

    /**
     * 根据节点服务ID删除节点
     *
     * @param serverId 节点服务ID
     * @return 是否删除成功
     */
    @CacheEvict(value = "mediaNode", key = "#serverId")
    public boolean deleteMediaNodeByServerId(String serverId) {
        Assert.hasText(serverId, "节点服务ID不能为空");

        MediaNodeDO existingNode = getByServerId(serverId);
        Assert.notNull(existingNode, "节点不存在，服务ID: " + serverId);

        boolean removed = mediaNodeService.removeById(existingNode.getId());
        Assert.isTrue(removed, "节点删除失败");

        log.info("成功删除流媒体节点，节点ID: {}, 数据库ID: {}", serverId, existingNode.getId());
        return true;
    }

    /**
     * 批量删除节点
     *
     * @param ids 数据库主键ID列表
     * @return 成功删除的数量
     */
    @CacheEvict(value = "mediaNode", allEntries = true)
    public int batchDeleteMediaNode(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        // 获取要删除的节点信息，用于日志记录
        List<MediaNodeDO> nodesToDelete = mediaNodeService.listByIds(ids);

        boolean removed = mediaNodeService.removeBatchByIds(ids);
        Assert.isTrue(removed, "批量删除节点失败");

        int successCount = nodesToDelete.size();
        log.info("批量删除流媒体节点完成，成功删除: {} 个节点", successCount);

        for (MediaNodeDO node : nodesToDelete) {
            log.info("删除节点: 服务ID={}, 数据库ID={}", node.getServerId(), node.getId());
        }

        return successCount;
    }

    /**
     * 根据条件删除节点
     *
     * @param mediaNode 删除条件
     * @return 成功删除的数量
     */
    @CacheEvict(value = "mediaNode", allEntries = true)
    public int deleteMediaNodeByCondition(MediaNodeDO mediaNode) {
        QueryWrapper<MediaNodeDO> query = new QueryWrapper<>();
        if (mediaNode.getServerId() != null) {
            query.eq("server_id", mediaNode.getServerId());
        }
        if (mediaNode.getName() != null) {
            query.eq("name", mediaNode.getName());
        }
        if (mediaNode.getHost() != null) {
            query.eq("host", mediaNode.getHost());
        }
        if (mediaNode.getEnabled() != null) {
            query.eq("enabled", mediaNode.getEnabled());
        }
        if (mediaNode.getStatus() != null) {
            query.eq("status", mediaNode.getStatus());
        }

        // 先查询要删除的节点，用于日志记录
        List<MediaNodeDO> nodesToDelete = mediaNodeService.list(query);

        boolean removed = mediaNodeService.remove(query);
        Assert.isTrue(removed, "按条件删除节点失败");

        int successCount = nodesToDelete.size();
        log.info("按条件删除流媒体节点完成，成功删除: {} 个节点", successCount);

        return successCount;
    }

    /**
     * 保存或更新节点状态（用于Hook回调）
     * 如果节点不存在则创建，存在则更新状态和心跳时间
     *
     * @param serverId 节点服务ID
     * @param apiSecret 密钥
     * @param keepalive 心跳时间戳
     * @param host 节点地址（可选，仅在创建时使用）
     * @param name 节点名称（可选，仅在创建时使用）
     * @return 节点数据库ID
     */
    @CacheEvict(value = "mediaNode", key = "#serverId")
    public Long saveOrUpdateNodeStatus(String serverId, String apiSecret, Long keepalive, String host, String name) {
        Assert.hasText(serverId, "节点服务ID不能为空");
        Assert.notNull(apiSecret, "密钥不能为空");

        MediaNodeDO existingNode = getByServerId(serverId);
        Date now = new Date();

        if (existingNode != null) {
            // 节点已存在，更新状态和心跳时间
            existingNode.setStatus(1);
            existingNode.setKeepalive(keepalive != null ? keepalive : System.currentTimeMillis());
            existingNode.setUpdateTime(now);

            boolean updated = mediaNodeService.updateById(existingNode);
            Assert.isTrue(updated, "更新节点状态失败");

            log.info("更新现有节点状态，节点ID: {}, host: {}, 心跳: {}", serverId, host, existingNode.getKeepalive());
            return existingNode.getId();
        } else {
            // 节点不存在，创建新节点
            MediaNodeDO newNode = new MediaNodeDO();
            newNode.setServerId(serverId);
            newNode.setName(name != null ? name : serverId);
            newNode.setHost(host);
            newNode.setSecret(apiSecret); // 默认密钥，后续可通过管理界面修改
            newNode.setEnabled(true); // 默认启用
            newNode.setHookEnabled(true); // 默认启用Hook
            newNode.setWeight(0); // 默认权重
            newNode.setStatus(1);
            newNode.setKeepalive(keepalive != null ? keepalive : System.currentTimeMillis() / 1000);
            newNode.setDescription("通过ZLM Hook自动创建");
            newNode.setCreateTime(now);
            newNode.setUpdateTime(now);

            boolean saved = mediaNodeService.save(newNode);
            Assert.isTrue(saved, "创建节点失败");

            log.info("创建新节点，节点ID: {}, host: {}, 心跳: {}", serverId, host, newNode.getKeepalive());
            return newNode.getId();
        }
    }

    /**
     * 更新节点离线状态
     *
     * @param serverId 节点服务ID
     */
    @CacheEvict(value = "mediaNode", key = "#serverId")
    public void updateNodeOffline(String serverId) {
        MediaNodeDO node = getByServerId(serverId);
        if (node != null) {
            node.setStatus(0); // 离线
            node.setUpdateTime(new Date());
            mediaNodeService.updateById(node);

            log.info("更新节点离线状态，节点ID: {}", serverId);
        }
    }

}