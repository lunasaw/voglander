package io.github.lunasaw.voglander.intergration.wrapper.zlm.service.impl;

import com.luna.common.dto.ResultDTO;
import io.github.lunasaw.voglander.intergration.wrapper.common.WrapperExceptionHandler;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.common.ZlmWrapperValidator;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.StreamProxyZlmWrapperService;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto.MediaOnlineRequest;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto.StreamProxyDeleteRequest;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto.StreamProxyRequest;
import io.github.lunasaw.zlm.api.ZlmRestService;
import io.github.lunasaw.zlm.entity.MediaOnlineStatus;
import io.github.lunasaw.zlm.entity.ServerResponse;
import io.github.lunasaw.zlm.entity.StreamKey;
import io.github.lunasaw.zlm.entity.StreamProxyItem;
import io.github.lunasaw.zlm.entity.req.MediaReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * todo 这里确定已经包装了ResultDTO返回，所以业务使用的时候不需要在判断外部错误了，只需要获取模型，判断是否为null
 * Stream Proxy ZLM 包装服务实现
 * <p>
 * 简单的 ZLM API 包装层，仅负责参数校验和异常处理。
 * 遵循 Wrapper 层设计原则：参数验证 + 异常捕获，业务逻辑委托给底层 API。
 * </p>
 * 
 * @author luna
 * @since 2025-01-23
 */
@Slf4j
@Service
public class StreamProxyZlmWrapperServiceImpl implements StreamProxyZlmWrapperService {

    @Override
    public ResultDTO<StreamKey> addStreamProxy(StreamProxyRequest request) {
        return WrapperExceptionHandler.executeWithExceptionHandling(() -> {
            // 参数验证
            WrapperExceptionHandler.validateRequest(request, "流代理请求参数");
            ZlmWrapperValidator.validateZlmConnection(request.getHost(), request.getSecret());
            WrapperExceptionHandler.validateRequest(request.getStreamProxyItem(), "流代理配置");

            StreamProxyItem streamProxyItem = request.getStreamProxyItem();
            ZlmWrapperValidator.validateAppAndStream(streamProxyItem.getApp(), streamProxyItem.getStream());
            ZlmWrapperValidator.validateStreamUrl(streamProxyItem.getUrl());

            // 调用底层 API
            ServerResponse<StreamKey> response = ZlmRestService.addStreamProxy(
                request.getHost(), request.getSecret(), streamProxyItem);

            // 简单的响应验证
            if (response == null || response.getCode() != 0 || response.getData() == null) {
                log.error("ZLM API调用失败 - 添加流代理: {}", response != null ? response.getMsg() : "响应为空");
                return null;
            }

            return response.getData();

        }, "添加流代理");
    }

    @Override
    public ResultDTO<StreamKey.StringDelFlag> deleteStreamProxy(StreamProxyDeleteRequest request) {
        return WrapperExceptionHandler.executeWithExceptionHandling(() -> {
            // 参数验证
            WrapperExceptionHandler.validateRequest(request, "流代理删除请求参数");
            ZlmWrapperValidator.validateZlmConnection(request.getHost(), request.getSecret());
            ZlmWrapperValidator.validateProxyKey(request.getProxyKey());

            // 调用底层 API
            ServerResponse<StreamKey.StringDelFlag> response = ZlmRestService.delStreamProxy(
                request.getHost(), request.getSecret(), request.getProxyKey());

            // 简单的响应验证
            if (response == null || response.getCode() != 0) {
                log.error("ZLM API调用失败 - 删除流代理: {}", response != null ? response.getMsg() : "响应为空");
                return null;
            }

            return response.getData();

        }, "删除流代理");
    }

    @Override
    public ResultDTO<MediaOnlineStatus> isMediaOnline(MediaOnlineRequest request) {
        return WrapperExceptionHandler.executeWithExceptionHandling(() -> {
            // 参数验证
            WrapperExceptionHandler.validateRequest(request, "媒体在线状态查询请求参数");
            ZlmWrapperValidator.validateZlmConnection(request.getHost(), request.getSecret());
            WrapperExceptionHandler.validateRequest(request.getMediaReq(), "媒体查询请求");

            MediaReq mediaReq = request.getMediaReq();
            ZlmWrapperValidator.validateMedia(mediaReq.getApp(), mediaReq.getStream(), mediaReq.getSchema(), mediaReq.getVhost());

            // 调用底层 API
            MediaOnlineStatus status = ZlmRestService.isMediaOnline(
                request.getHost(), request.getSecret(), mediaReq);

            // 简单的响应验证
            if (status == null) {
                log.error("ZLM API调用失败 - 查询媒体在线状态: 响应为空");
                return null;
            }

            return status;

        }, "查询媒体在线状态");
    }
}