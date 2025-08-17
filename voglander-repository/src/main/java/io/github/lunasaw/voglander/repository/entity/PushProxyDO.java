package io.github.lunasaw.voglander.repository.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 推流代理管理表实体类
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_push_proxy")
public class PushProxyDO implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long              id;

    /**
     * 创建时间
     */
    private LocalDateTime     createTime;

    /**
     * 修改时间
     */
    private LocalDateTime     updateTime;

    /**
     * 应用名称
     */
    private String            app;

    /**
     * 流ID
     */
    private String            stream;

    /**
     * 推流目标地址
     */
    private String            dstUrl;

    /**
     * 推流协议 rtmp/rtsp
     */
    private String            schema;

    /**
     * 代理状态 1启用 0禁用
     */
    private Integer           status;

    /**
     * 流在线状态 1在线 0离线
     */
    private Integer           onlineStatus;

    /**
     * ZLM返回的代理key
     */
    private String            proxyKey;

    /**
     * 节点ID，保存当前添加推流代理的节点
     */
    private String            serverId;

    /**
     * 是否启用 1启用 0禁用
     */
    private Integer           enabled;

    /**
     * 代理描述
     */
    private String            description;

    /**
     * 扩展字段
     */
    private String            extend;
}