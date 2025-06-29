package io.github.lunasaw.voglander.intergration.wrapper.zlm.supplier;

import io.github.lunasaw.voglander.manager.domaon.dto.MediaNodeDTO;
import io.github.lunasaw.voglander.manager.manager.MediaNodeManager;
import io.github.lunasaw.zlm.config.ZlmNode;
import io.github.lunasaw.zlm.node.NodeSupplier;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Voglander节点提供器
 * 从数据库动态获取启用的媒体节点列表
 *
 * @author luna
 * @date 2025/01/23
 */
@Slf4j
@Component
@Order(1)
@ConditionalOnProperty(value = "zlm.enable", havingValue = "true")
public class VoglanderNodeSupplier implements NodeSupplier {

    @Autowired
    private MediaNodeManager mediaNodeManager;

    @Override
    public List<ZlmNode> getNodes() {
        try {
            // 从数据库获取所有启用的节点
            List<MediaNodeDTO> enabledNodes = mediaNodeManager.getEnabledNodes();

            if (enabledNodes == null || enabledNodes.isEmpty()) {
                log.debug("没有找到启用的媒体节点");
                return Collections.emptyList();
            }

            // 转换为ZlmNode列表
            List<ZlmNode> zlmNodes = enabledNodes.stream()
                .map(this::convertToZlmNode)
                .filter(node -> node != null) // 过滤转换失败的节点
                .collect(Collectors.toList());

            log.debug("NodeSupplier获取到{}个启用的ZLM节点", zlmNodes.size());
            return zlmNodes;

        } catch (Exception e) {
            log.error("NodeSupplier获取节点列表失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public ZlmNode getNode(String serverId) {
        if (serverId == null || serverId.trim().isEmpty()) {
            log.debug("NodeSupplier获取节点失败: serverId为空");
            return null;
        }

        try {
            // 从数据库获取指定的节点（带缓存）
            MediaNodeDTO nodeDTO = mediaNodeManager.getDTOByServerId(serverId);

            if (nodeDTO == null) {
                log.debug("NodeSupplier未找到指定节点: serverId={}", serverId);
                return null;
            }

            // 转换为ZlmNode并返回
            ZlmNode zlmNode = convertToZlmNode(nodeDTO);
            if (zlmNode != null) {
                log.debug("NodeSupplier成功获取节点: serverId={}, host={}", serverId, zlmNode.getHost());
            }
            return zlmNode;

        } catch (Exception e) {
            log.error("NodeSupplier获取节点失败: serverId={}, 错误: {}", serverId, e.getMessage());
            return null;
        }
    }

    @Override
    public String getName() {
        return "VoglanderNodeSupplier";
    }

    /**
     * 将MediaNodeDTO转换为ZlmNode
     *
     * @param nodeDTO 数据库节点DTO
     * @return ZlmNode对象，转换失败时返回null
     */
    private ZlmNode convertToZlmNode(MediaNodeDTO nodeDTO) {
        if (nodeDTO == null || nodeDTO.getServerId() == null) {
            return null;
        }
        if (!nodeDTO.getEnabled()) {
            return null;
        }

        try {
            ZlmNode node = new ZlmNode();
            node.setServerId(nodeDTO.getServerId());
            node.setHost(nodeDTO.getHost());
            node.setSecret(nodeDTO.getSecret());
            node.setWeight(nodeDTO.getWeight() != null ? nodeDTO.getWeight() : 1);
            node.setEnabled(nodeDTO.getEnabled() != null ? nodeDTO.getEnabled() : false);

            return node;
        } catch (Exception e) {
            log.error("转换MediaNodeDTO到ZlmNode失败: serverId={}, 错误: {}",
                nodeDTO.getServerId(), e.getMessage());
            return null;
        }
    }
}