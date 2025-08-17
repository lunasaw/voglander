package io.github.lunasaw.voglander.service.stream;

import io.github.lunasaw.voglander.manager.domaon.dto.PushProxyDTO;

/**
 * Push Proxy 业务服务接口
 * <p>
 * 提供推流代理的高级业务逻辑，包括节点选择、推流管理、状态同步等功能
 * </p>
 * 
 * @author luna
 * @since 2025-01-23
 */
public interface PushProxyBizService {
    /**
     * 创建推流代理（指定节点）
     * <p>
     * 在指定的ZLM节点上创建推流代理，节点ID从pushProxyDTO中获取
     * </p>
     * 
     * @param pushProxyDTO 推流代理配置（包含serverId字段）
     * @return 创建的推流代理ID
     */
    Long createPushProxyWithSpecificNode(PushProxyDTO pushProxyDTO);

    /**
     * 更新推流代理（主要接口）
     * <p>
     * 如果推流地址、应用名或流名发生变化，则停止旧的代理并重新创建
     * 支持ID查询和更新内容分离的灵活更新策略
     * </p>
     * 
     * @param updateDTO 更新DTO，包含查询条件和更新内容
     * @return 更新结果
     */
    boolean updatePushProxyWithRecreation(PushProxyDTO updateDTO);

    /**
     * 根据ID更新推流代理（便利方法）
     * <p>
     * 内部调用主要DTO方法，提供便利的API
     * </p>
     * 
     * @param id 推流代理ID
     * @param updateDTO 更新的推流代理配置
     * @return 更新结果
     */
    boolean updatePushProxyWithRecreation(Long id, PushProxyDTO updateDTO);

    /**
     * 删除推流代理
     * <p>
     * 删除数据库记录的同时停止ZLM上的推流代理
     * </p>
     * 
     * @param deleteDTO 删除条件DTO，包含查询条件
     * @return 删除结果
     */
    boolean deletePushProxyWithTermination(PushProxyDTO deleteDTO);

    /**
     * 根据代理key删除推流代理（便利方法）
     * <p>
     * 删除数据库记录的同时停止ZLM上的推流代理
     * 内部调用主要DTO方法
     * </p>
     * 
     * @param proxyKey 代理密钥
     * @return 删除结果
     */
    boolean deletePushProxyByKeyWithTermination(String proxyKey);

    /**
     * 更新推流代理状态（主要接口）
     * <p>
     * 启用时检查源流是否在线，在线则启动推流代理
     * 停用时停止推流代理
     * </p>
     * 
     * @param statusDTO 状态更新DTO，包含查询条件和状态值
     * @return 更新结果
     */
    boolean updatePushProxyStatus(PushProxyDTO statusDTO);

    /**
     * 根据ID更新推流代理状态（便利方法）
     * <p>
     * 内部调用主要DTO方法，提供便利的API
     * </p>
     * 
     * @param id 推流代理ID
     * @param status 状态（1启用 0禁用）
     * @return 更新结果
     */
    boolean updatePushProxyStatus(Long id, Integer status);

    /**
     * 同步推流代理在线状态（主要接口）
     * <p>
     * 查询ZLM服务器上的实际状态并更新到数据库
     * </p>
     * 
     * @param syncDTO 同步条件DTO，包含查询条件
     * @return 同步结果
     */
    boolean syncPushProxyOnlineStatus(PushProxyDTO syncDTO);

    /**
     * 根据ID同步推流代理在线状态（便利方法）
     * <p>
     * 内部调用主要DTO方法，提供便利的API
     * </p>
     * 
     * @param id 推流代理ID
     * @return 同步结果
     */
    boolean syncPushProxyOnlineStatus(Long id);

    /**
     * 批量同步所有启用的推流代理在线状态
     * <p>
     * 定时任务调用，同步所有启用的推流代理状态
     * </p>
     * 
     * @return 同步的记录数
     */
    int syncAllEnabledPushProxyStatus();

    /**
     * 启动推流代理（主要接口）
     * <p>
     * 检查源流是否在线，在线则创建推流代理
     * </p>
     * 
     * @param startDTO 启动条件DTO，包含查询条件
     * @return 启动结果
     */
    boolean startPushProxy(PushProxyDTO startDTO);

    /**
     * 根据ID启动推流代理（便利方法）
     * <p>
     * 内部调用主要DTO方法，提供便利的API
     * </p>
     * 
     * @param id 推流代理ID
     * @return 启动结果
     */
    boolean startPushProxy(Long id);

    /**
     * 停止推流代理（主要接口）
     * <p>
     * 停止ZLM上的推流代理但保留数据库记录
     * </p>
     * 
     * @param stopDTO 停止条件DTO，包含查询条件
     * @return 停止结果
     */
    boolean stopPushProxy(PushProxyDTO stopDTO);

    /**
     * 根据ID停止推流代理（便利方法）
     * <p>
     * 内部调用主要DTO方法，提供便利的API
     * </p>
     * 
     * @param id 推流代理ID
     * @return 停止结果
     */
    boolean stopPushProxy(Long id);

    /**
     * 检查源流是否在线（主要接口）
     * <p>
     * 查询指定应用和流的在线状态
     * </p>
     * 
     * @param checkDTO 检查条件DTO，包含serverId、app、stream等信息
     * @return 是否在线
     */
    boolean checkSourceStreamOnline(PushProxyDTO checkDTO);

    /**
     * 检查源流是否在线（便利方法）
     * <p>
     * 内部调用主要DTO方法，提供便利的API
     * </p>
     * 
     * @param serverId 节点ID
     * @param app 应用名称
     * @param stream 流名称
     * @return 是否在线
     */
    boolean checkSourceStreamOnline(String serverId, String app, String stream);
}