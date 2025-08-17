package io.github.lunasaw.voglander.intergration.wrapper.zlm.service;

import com.luna.common.dto.ResultDTO;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto.MediaOnlineRequest;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto.PushProxyDeleteRequest;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto.PushProxyRequest;
import io.github.lunasaw.zlm.entity.MediaOnlineStatus;
import io.github.lunasaw.zlm.entity.StreamKey;

/**
 * Push Proxy ZLM 包装服务接口
 * <p>
 * 提供对ZLM媒体服务器推流代理相关操作的包装，统一异常处理和返回格式。
 * 使用Builder DTO方式调用，提供类型安全和参数验证。
 * </p>
 * 
 * @author luna
 * @since 2025-01-23
 */
public interface PushProxyZlmWrapperService {

    // ==================== DTO Builder 方式方法 ====================

    /**
     * 添加推流代理（DTO方式）
     * <p>
     * 使用Builder模式封装的请求参数在指定的ZLM节点上创建推流代理
     * </p>
     * 
     * @param request 推流代理请求参数
     * @return 代理密钥结果，包含key用于后续操作
     */
    ResultDTO<StreamKey> addPushProxy(PushProxyRequest request);

    /**
     * 删除推流代理（DTO方式）
     * <p>
     * 使用Builder模式封装的请求参数删除ZLM节点上的推流代理
     * </p>
     * 
     * @param request 推流代理删除请求参数
     * @return 删除结果
     */
    ResultDTO<StreamKey.StringDelFlag> deletePushProxy(PushProxyDeleteRequest request);

    /**
     * 检查源流是否在线（DTO方式）
     * <p>
     * 使用Builder模式封装的请求参数查询指定应用和流的在线状态
     * </p>
     * 
     * @param request 媒体在线状态查询请求参数
     * @return 媒体在线状态
     */
    ResultDTO<MediaOnlineStatus> isSourceStreamOnline(MediaOnlineRequest request);

}