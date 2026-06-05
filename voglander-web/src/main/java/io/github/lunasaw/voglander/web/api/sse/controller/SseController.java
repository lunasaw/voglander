package io.github.lunasaw.voglander.web.api.sse.controller;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.common.util.JwtUtils;
import io.github.lunasaw.voglander.manager.service.AuthService;
import io.github.lunasaw.voglander.service.sse.SseEventBus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/stream")
@Tag(name = "SSE 实时事件")
public class SseController {

    @Autowired private SseEventBus sseEventBus;
    @Autowired private AuthService  authService;

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅实时事件流")
    public SseEmitter subscribe(
            @RequestParam(defaultValue = "device,live,alarm") String topics,
            @RequestParam(required = false) String token) {
        if (token == null || authService.getUserByToken(token) == null) {
            throw new ServiceException(ServiceExceptionEnum.LOGIN_REQUIRED);
        }
        Long userId = JwtUtils.getUserId(token);
        String userIdStr = userId != null ? userId.toString() : token;
        Set<String> topicSet = new HashSet<>(Arrays.asList(topics.split(",")));
        return sseEventBus.register(userIdStr, topicSet);
    }
}
