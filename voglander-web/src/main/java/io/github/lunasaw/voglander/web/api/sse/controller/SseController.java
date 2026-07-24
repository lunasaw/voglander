package io.github.lunasaw.voglander.web.api.sse.controller;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.service.sse.SseEventBus;
import io.github.lunasaw.voglander.service.sse.SseSubscriptionContext;
import io.github.lunasaw.voglander.web.api.auth.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/stream")
@Tag(name = "SSE 实时事件")
public class SseController {

    private final SseEventBus sseEventBus;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Autowired
    public SseController(SseEventBus sseEventBus, AuthenticatedUserResolver authenticatedUserResolver) {
        this.sseEventBus = sseEventBus;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅实时事件流")
    public SseEmitter subscribe(
            @Parameter(description = "逗号分隔的事件主题或主题前缀；登录用户可订阅任意非空主题")
            @RequestParam(defaultValue = "device,live,alarm") String topics,
            @RequestParam(required = false) String token) {
        UserDTO actor = authenticatedUserResolver.resolveToken(token);
        Set<String> topicSet = new HashSet<>(Arrays.asList(topics.split(",")));
        SseSubscriptionContext context = SseSubscriptionContext.authorized(actor.getId().toString(), topicSet);
        return sseEventBus.register(context);
    }
}
