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
import io.github.lunasaw.voglander.service.task.BusinessTaskAuthorizationService;
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
    private final BusinessTaskAuthorizationService authorizationService;

    @Autowired
    public SseController(SseEventBus sseEventBus, AuthenticatedUserResolver authenticatedUserResolver,
        BusinessTaskAuthorizationService authorizationService) {
        this.sseEventBus = sseEventBus;
        this.authenticatedUserResolver = authenticatedUserResolver;
        this.authorizationService = authorizationService;
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅实时事件流")
    public SseEmitter subscribe(
            @Parameter(description = "逗号分隔的主题 allowlist：device、live、alarm、business.task、image.asset；"
                + "business.task 和 image.asset 需相应查询权限，服务端还会逐事件校验权限")
            @RequestParam(defaultValue = "device,live,alarm") String topics,
            @RequestParam(required = false) String token) {
        UserDTO actor = authenticatedUserResolver.resolveToken(token);
        Set<String> topicSet = new HashSet<>(Arrays.asList(topics.split(",")));
        SseSubscriptionContext context = authorizationService.sseContext(actor, topicSet);
        return sseEventBus.register(context);
    }
}
