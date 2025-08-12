package io.github.lunasaw.voglander.service.stream;

import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.zlm.entity.req.MediaReq;

/**
 * Stream Proxy 业务服务接口
 * <p>
 * 提供流代理的高级业务逻辑，包括节点选择、流管理、状态同步等功能
 * </p>
 * 
 * @author luna
 * @since 2025-01-23
 */
public interface StreamProxyBizService {
    /**
     * 创建流代理（指定节点）
     * <p>
     * 在指定的ZLM节点上创建流代理，节点ID从streamProxyDTO中获取
     * </p>
     * 
     * @param streamProxyDTO 流代理配置（包含serverId字段）
     * @return 创建的流代理ID
     */
    Long createStreamProxyWithSpecificNode(StreamProxyDTO streamProxyDTO);

    /**
     * 更新流代理
     * <p>
     * 如果流地址、应用名或流名发生变化，则停止旧的代理并重新创建
     * </p>
     * 
     * @param id 流代理ID
     * @param streamProxyDTO 更新的流代理配置
     * @return 更新结果
     */
    boolean updateStreamProxyWithRecreation(Long id, StreamProxyDTO streamProxyDTO);

    /**
     * 删除流代理
     * <p>
     * 删除数据库记录的同时停止ZLM上的流代理
     * </p>
     * 
     * @param id 流代理ID
     * @return 删除结果
     */
    boolean deleteStreamProxyWithTermination(Long id);

    /**
     * 根据代理key删除流代理
     * <p>
     * 删除数据库记录的同时停止ZLM上的流代理
     * </p>
     * 
     * @param proxyKey 代理密钥
     * @return 删除结果
     */
    boolean deleteStreamProxyByKeyWithTermination(String proxyKey);

    /**
     * 更新流代理状态
     * <p>
     * 启用时检查流是否在线，不在线则触发拉流代理
     * 停用时停止流代理
     * </p>
     * 
     * @param id 流代理ID
     * @param status 状态（1启用 0禁用）
     * @return 更新结果
     */
    boolean updateStreamProxyStatus(Long id, Integer status);

    /**
     * 同步流代理在线状态
     * <p>
     * 查询ZLM服务器上的实际状态并更新到数据库
     * </p>
     * 
     * @param id 流代理ID
     * @return 同步结果
     */
    boolean syncStreamProxyOnlineStatus(Long id);

    /**
     * 批量同步所有启用的流代理在线状态
     * <p>
     * 定时任务调用，同步所有启用的流代理状态
     * </p>
     * 
     * @return 同步的记录数
     */
    int syncAllEnabledStreamProxyStatus();

    /**
     * 启动流代理
     * <p>
     * 检查流是否在线，不在线则创建拉流代理
     * </p>
     * 
     * @param id 流代理ID
     * @return 启动结果
     */
    boolean startStreamProxy(Long id);

    /**
     * 停止流代理
     * <p>
     * 停止ZLM上的流代理但保留数据库记录
     * </p>
     * 
     * @param id 流代理ID
     * @return 停止结果
     */
    boolean stopStreamProxy(Long id);

    /**
     * 检查流是否在线
     * <p>
     * 查询指定应用和流的在线状态
     * </p>
     * 
     * @param serverId 节点ID
     * @param app 应用名称
     * @param stream 流名称
     * @return 是否在线
     */
    boolean checkStreamOnline(String serverId, String app, String stream);

}