package io.github.lunasaw.voglander.repository.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 流媒体节点管理表实体类
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_media_node")
public class MediaNodeDO implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
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
}