package io.github.lunasaw.voglander.intergration.wrapper.zlm.service.impl;

import com.luna.common.dto.ResultDTO;
import com.luna.common.dto.ResultDTOUtils;
import com.luna.common.dto.constant.ResultCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.lunasaw.voglander.intergration.wrapper.common.WrapperExceptionHandler;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.common.ZlmWrapperValidator;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.PushProxyZlmWrapperService;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto.MediaOnlineRequest;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto.PushProxyDeleteRequest;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto.PushProxyRequest;
import io.github.lunasaw.zlm.api.ZlmRestService;
import io.github.lunasaw.zlm.entity.MediaOnlineStatus;
import io.github.lunasaw.zlm.entity.ServerResponse;
import io.github.lunasaw.zlm.entity.StreamKey;
import io.github.lunasaw.zlm.entity.StreamPusherItem;
import io.github.lunasaw.zlm.entity.req.MediaReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PushProxyZlmWrapperServiceImpl implements PushProxyZlmWrapperService {

    @CircuitBreaker(name = "zlm", fallbackMethod = "addPushProxyFallback")
    @Override
    public ResultDTO<StreamKey> addPushProxy(PushProxyRequest request) {
        return WrapperExceptionHandler.executeWithExceptionHandling(() -> {
            WrapperExceptionHandler.validateRequest(request, "推流代理请求参数");
            ZlmWrapperValidator.validateZlmConnection(request.getHost(), request.getSecret());
            WrapperExceptionHandler.validateRequest(request.getStreamPusherItem(), "推流代理配置");
            StreamPusherItem streamPusherItem = request.getStreamPusherItem();
            ZlmWrapperValidator.validateAppAndStream(streamPusherItem.getApp(), streamPusherItem.getStream());
            WrapperExceptionHandler.validateNotEmpty(streamPusherItem.getDstUrl(), "推流目标地址");
            ServerResponse<StreamKey> response = ZlmRestService.addStreamPusherProxy(request.getHost(), request.getSecret(), streamPusherItem);
            if (response == null || response.getCode() != 0 || response.getData() == null) {
                log.error("ZLM API调用失败 - 添加推流代理: {}", response != null ? response.getMsg() : "响应为空");
                return null;
            }
            return response.getData();
        }, "添加推流代理");
    }

    public ResultDTO<StreamKey> addPushProxyFallback(PushProxyRequest request, Throwable t) {
        log.warn("添加推流代理熔断降级: {}", t.getMessage());
        @SuppressWarnings("unchecked")
        final ResultDTO result = ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "ZLM服务不可用");
        return result;
    }

    @CircuitBreaker(name = "zlm", fallbackMethod = "deletePushProxyFallback")
    @Override
    public ResultDTO<StreamKey.StringDelFlag> deletePushProxy(PushProxyDeleteRequest request) {
        return WrapperExceptionHandler.executeWithExceptionHandling(() -> {
            WrapperExceptionHandler.validateRequest(request, "推流代理删除请求参数");
            ZlmWrapperValidator.validateZlmConnection(request.getHost(), request.getSecret());
            ZlmWrapperValidator.validateProxyKey(request.getProxyKey());
            ServerResponse<StreamKey.StringDelFlag> response = ZlmRestService.delStreamPusherProxy(request.getHost(), request.getSecret(), request.getProxyKey());
            if (response == null || response.getCode() != 0) {
                log.error("ZLM API调用失败 - 删除推流代理: {}", response != null ? response.getMsg() : "响应为空");
                return null;
            }
            return response.getData();
        }, "删除推流代理");
    }

    public ResultDTO<StreamKey.StringDelFlag> deletePushProxyFallback(PushProxyDeleteRequest request, Throwable t) {
        log.warn("删除推流代理熔断降级: {}", t.getMessage());
        @SuppressWarnings("unchecked")
        final ResultDTO result = ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "ZLM服务不可用");
        return result;
    }

    @CircuitBreaker(name = "zlm", fallbackMethod = "isSourceStreamOnlineFallback")
    @Override
    public ResultDTO<MediaOnlineStatus> isSourceStreamOnline(MediaOnlineRequest request) {
        return WrapperExceptionHandler.executeWithExceptionHandling(() -> {
            WrapperExceptionHandler.validateRequest(request, "媒体在线状态查询请求参数");
            ZlmWrapperValidator.validateZlmConnection(request.getHost(), request.getSecret());
            WrapperExceptionHandler.validateRequest(request.getMediaReq(), "媒体查询请求");
            MediaReq mediaReq = request.getMediaReq();
            ZlmWrapperValidator.validateMedia(mediaReq.getApp(), mediaReq.getStream(), mediaReq.getSchema(), mediaReq.getVhost());
            MediaOnlineStatus status = ZlmRestService.isMediaOnline(request.getHost(), request.getSecret(), mediaReq);
            if (status == null) {
                log.error("ZLM API调用失败 - 检查源流在线状态: 响应为空");
                return null;
            }
            return status;
        }, "检查源流在线状态");
    }

    public ResultDTO<MediaOnlineStatus> isSourceStreamOnlineFallback(MediaOnlineRequest request, Throwable t) {
        log.warn("检查源流在线状态熔断降级: {}", t.getMessage());
        @SuppressWarnings("unchecked")
        final ResultDTO result = ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "ZLM服务不可用");
        return result;
    }
}
