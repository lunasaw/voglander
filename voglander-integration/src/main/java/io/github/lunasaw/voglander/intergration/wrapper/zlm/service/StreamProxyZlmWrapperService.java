package io.github.lunasaw.voglander.intergration.wrapper.zlm.service;

import com.luna.common.dto.ResultDTO;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto.MediaOnlineRequest;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto.StreamProxyDeleteRequest;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto.StreamProxyRequest;
import io.github.lunasaw.zlm.entity.MediaOnlineStatus;
import io.github.lunasaw.zlm.entity.StreamKey;
import io.github.lunasaw.zlm.entity.StreamProxyItem;
import io.github.lunasaw.zlm.entity.req.MediaReq;

/**
 * Stream Proxy ZLM 包装服务接口
 * <p>
 * 提供对ZLM媒体服务器拉流代理相关操作的包装，统一异常处理和返回格式。
 * 使用Builder DTO方式调用，提供类型安全和参数验证。
 * </p>
 * 
 * @author luna
 * @since 2025-01-23
 */
public interface StreamProxyZlmWrapperService {

    // ==================== DTO Builder 方式方法 ====================

    /**
     * 添加流代理（DTO方式）
     * <p>
     * 使用Builder模式封装的请求参数在指定的ZLM节点上创建拉流代理
     * </p>
     * 
     * @param request 流代理请求参数
     * @return 代理密钥结果，包含key用于后续操作
     */
    ResultDTO<StreamKey> addStreamProxy(StreamProxyRequest request);

    /**
     * 删除流代理（DTO方式）
     * <p>
     * 使用Builder模式封装的请求参数删除ZLM节点上的拉流代理
     * </p>
     * 
     * @param request 流代理删除请求参数
     * @return 删除结果
     */
    ResultDTO<StreamKey.StringDelFlag> deleteStreamProxy(StreamProxyDeleteRequest request);

    /**
     * 检查媒体流是否在线（DTO方式）
     * <p>
     * 使用Builder模式封装的请求参数查询指定应用和流的在线状态
     * </p>
     * 
     * @param request 媒体在线状态查询请求参数
     * @return 媒体在线状态
     */
    ResultDTO<MediaOnlineStatus> isMediaOnline(MediaOnlineRequest request);

}