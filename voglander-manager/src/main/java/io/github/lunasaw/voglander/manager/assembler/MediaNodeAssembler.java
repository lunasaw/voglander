package io.github.lunasaw.voglander.manager.assembler;

import io.github.lunasaw.voglander.manager.domaon.dto.MediaNodeDTO;
import io.github.lunasaw.voglander.repository.entity.MediaNodeDO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流媒体节点数据转换器
 * 负责处理 DO、DTO、VO 之间的数据转换
 *
 * @author luna
 * @since 2025-01-23
 */
@Component
public class MediaNodeAssembler {

    /**
     * 将 MediaNodeDO 转换为 MediaNodeDTO
     * 复用原有的转换逻辑，包含扩展字段解析和默认值设置
     *
     * @param mediaNodeDO 数据库实体
     * @return MediaNodeDTO
     */
    public MediaNodeDTO toMediaNodeDTO(MediaNodeDO mediaNodeDO) {
        return MediaNodeDTO.convertDTO(mediaNodeDO);
    }

    /**
     * 批量将 MediaNodeDO 转换为 MediaNodeDTO
     *
     * @param mediaNodeDOList 数据库实体列表
     * @return MediaNodeDTO 列表
     */
    public List<MediaNodeDTO> toMediaNodeDTOList(List<MediaNodeDO> mediaNodeDOList) {
        if (mediaNodeDOList == null || mediaNodeDOList.isEmpty()) {
            return Collections.emptyList();
        }
        return mediaNodeDOList.stream()
            .map(this::toMediaNodeDTO)
            .collect(Collectors.toList());
    }

    /**
     * 将 MediaNodeDTO 转换为 MediaNodeDO
     *
     * @param mediaNodeDTO 业务DTO
     * @return MediaNodeDO
     */
    public MediaNodeDO toMediaNodeDO(MediaNodeDTO mediaNodeDTO) {
        return MediaNodeDTO.convertDO(mediaNodeDTO);
    }

    /**
     * 获取状态显示名称
     *
     * @param status 状态码
     * @return 状态名称
     */
    public String getStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "离线";
            case 1:
                return "在线";
            default:
                return "未知";
        }
    }

    /**
     * 获取启用状态显示名称
     *
     * @param enabled 启用状态
     * @return 状态名称
     */
    public String getEnabledName(Boolean enabled) {
        if (enabled == null) {
            return "未知";
        }
        return enabled ? "启用" : "禁用";
    }
}