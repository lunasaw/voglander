package io.github.lunasaw.voglander.web.api.medianode.vo;

import io.github.lunasaw.voglander.manager.domaon.dto.MediaNodeDTO;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流媒体节点 VO 模型
 * @author luna
 * @date 2025/01/23
 */
@Data
public class MediaNodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据库主键ID
     */
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
     * API密钥（脱敏显示）
     */
    private String secret;

    /**
     * 是否启用 true启用 false禁用
     */
    private Boolean enabled;

    /**
     * 启用状态名称
     */
    private String enabledName;

    /**
     * 是否启用Hook true启用 false禁用
     */
    private Boolean hookEnabled;

    /**
     * Hook启用状态名称
     */
    private String hookEnabledName;

    /**
     * 节点权重
     */
    private Integer weight;

    /**
     * 心跳时间戳
     */
    private Long keepalive;

    /**
     * 心跳时间
     */
    private Date keepaliveTime;

    /**
     * 节点状态 1在线 0离线
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 节点描述
     */
    private String description;

    /**
     * 扩展字段
     */
    private String extend;

    /**
     * 将 MediaNodeDTO 转换为 MediaNodeVO
     *
     * @param dto 业务DTO
     * @return MediaNodeVO
     */
    public static MediaNodeVO convertVO(MediaNodeDTO dto) {
        if (dto == null) {
            return null;
        }
        MediaNodeVO vo = new MediaNodeVO();
        vo.setId(dto.getId());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        vo.setServerId(dto.getServerId());
        vo.setName(dto.getName());
        vo.setHost(dto.getHost());
        vo.setSecret(maskSecret(dto.getSecret())); // 脱敏处理
        vo.setEnabled(dto.getEnabled());
        vo.setEnabledName(getEnabledName(dto.getEnabled()));
        vo.setHookEnabled(dto.getHookEnabled());
        vo.setHookEnabledName(getEnabledName(dto.getHookEnabled()));
        vo.setWeight(dto.getWeight());
        vo.setKeepalive(dto.getKeepalive());
        vo.setKeepaliveTime(dto.getKeepalive() != null ? new Date(dto.getKeepalive() * 1000) : null);
        vo.setStatus(dto.getStatus());
        vo.setStatusName(getStatusName(dto.getStatus()));
        vo.setDescription(dto.getDescription());
        vo.setExtend(dto.getExtend());
        return vo;
    }

    /**
     * 脱敏处理密钥
     */
    private static String maskSecret(String secret) {
        if (secret == null || secret.length() <= 6) {
            return secret;
        }
        return secret.substring(0, 3) + "***" + secret.substring(secret.length() - 3);
    }

    /**
     * 获取启用状态显示名称
     */
    private static String getEnabledName(Boolean enabled) {
        if (enabled == null) {
            return "未知";
        }
        return enabled ? "启用" : "禁用";
    }

    /**
     * 获取节点状态显示名称
     */
    private static String getStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 1:
                return "在线";
            case 0:
                return "离线";
            default:
                return "异常";
        }
    }
}