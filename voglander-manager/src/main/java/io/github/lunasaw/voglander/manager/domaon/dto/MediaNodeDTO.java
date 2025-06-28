package io.github.lunasaw.voglander.manager.domaon.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import io.github.lunasaw.voglander.repository.entity.MediaNodeDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * 流媒体节点管理DTO
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class MediaNodeDTO implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 节点ID
     */
    private String serverId;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点地址
     */
    private String host;

    /**
     * API密钥
     */
    private String secret;

    /**
     * 是否启用 1启用 0禁用
     */
    private Boolean enabled;

    /**
     * 是否启用Hook 1启用 0禁用
     */
    private Boolean hookEnabled;

    /**
     * 节点权重
     */
    private Integer weight;

    /**
     * 心跳时间戳
     */
    private Long keepalive;

    /**
     * 节点状态 1在线 0离线
     */
    private Integer status;

    /**
     * 节点描述
     */
    private String description;

    /**
     * 扩展字段
     */
    private String extend;

    /**
     * 将 MediaNodeDO 转换为 MediaNodeDTO
     *
     * @param mediaNodeDO 数据库实体
     * @return MediaNodeDTO
     */
    public static MediaNodeDTO convertDTO(MediaNodeDO mediaNodeDO) {
        if (mediaNodeDO == null) {
            return null;
        }
        MediaNodeDTO dto = new MediaNodeDTO();
        dto.setId(mediaNodeDO.getId());
        dto.setCreateTime(mediaNodeDO.getCreateTime());
        dto.setUpdateTime(mediaNodeDO.getUpdateTime());
        dto.setServerId(mediaNodeDO.getServerId());
        dto.setName(mediaNodeDO.getName());
        dto.setHost(mediaNodeDO.getHost());
        dto.setSecret(mediaNodeDO.getSecret());
        dto.setEnabled(mediaNodeDO.getEnabled());
        dto.setHookEnabled(mediaNodeDO.getHookEnabled());
        dto.setWeight(mediaNodeDO.getWeight());
        dto.setKeepalive(mediaNodeDO.getKeepalive());
        dto.setStatus(mediaNodeDO.getStatus());
        dto.setDescription(mediaNodeDO.getDescription());
        dto.setExtend(mediaNodeDO.getExtend());
        return dto;
    }

    /**
     * 将 MediaNodeDTO 转换为 MediaNodeDO
     *
     * @param mediaNodeDTO 业务DTO
     * @return MediaNodeDO
     */
    public static MediaNodeDO convertDO(MediaNodeDTO mediaNodeDTO) {
        if (mediaNodeDTO == null) {
            return null;
        }
        MediaNodeDO mediaNodeDO = new MediaNodeDO();
        mediaNodeDO.setId(mediaNodeDTO.getId());
        mediaNodeDO.setCreateTime(mediaNodeDTO.getCreateTime());
        mediaNodeDO.setUpdateTime(mediaNodeDTO.getUpdateTime());
        mediaNodeDO.setServerId(mediaNodeDTO.getServerId());
        mediaNodeDO.setName(mediaNodeDTO.getName());
        mediaNodeDO.setHost(mediaNodeDTO.getHost());
        mediaNodeDO.setSecret(mediaNodeDTO.getSecret());
        mediaNodeDO.setEnabled(mediaNodeDTO.getEnabled());
        mediaNodeDO.setHookEnabled(mediaNodeDTO.getHookEnabled());
        mediaNodeDO.setWeight(mediaNodeDTO.getWeight());
        mediaNodeDO.setKeepalive(mediaNodeDTO.getKeepalive());
        mediaNodeDO.setStatus(mediaNodeDTO.getStatus());
        mediaNodeDO.setDescription(mediaNodeDTO.getDescription());
        mediaNodeDO.setExtend(mediaNodeDTO.getExtend());
        return mediaNodeDO;
    }
}