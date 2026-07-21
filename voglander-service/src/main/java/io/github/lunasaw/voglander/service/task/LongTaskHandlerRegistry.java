package io.github.lunasaw.voglander.service.task;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import io.github.lunasaw.voglander.client.service.task.LongTaskHandler;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;

/** Validated immutable registry of durable task handlers. */
@Service
public class LongTaskHandlerRegistry {

    private final Map<String, LongTaskHandler> handlers;

    public LongTaskHandlerRegistry(List<LongTaskHandler> handlerList) {
        Map<String, LongTaskHandler> registered = new LinkedHashMap<>();
        for (LongTaskHandler handler : handlerList) {
            if (handler == null || isBlank(handler.taskType())) {
                throw new IllegalStateException("LongTaskHandler taskType must not be blank");
            }
            if (handler.payloadVersion() <= 0) {
                throw new IllegalStateException("LongTaskHandler payloadVersion must be positive: " + handler.taskType());
            }
            if (handler.capabilities() == null) {
                throw new IllegalStateException("LongTaskHandler capabilities must not be null: " + handler.taskType());
            }
            String type = handler.taskType().trim();
            if (registered.put(type, handler) != null) {
                throw new IllegalStateException("Duplicate LongTaskHandler taskType: " + type);
            }
        }
        handlers = Collections.unmodifiableMap(registered);
    }

    public LongTaskHandler require(String taskType) {
        LongTaskHandler handler = handlers.get(taskType);
        if (handler == null) {
            throw new ServiceException(ServiceExceptionEnum.TASK_TYPE_UNREGISTERED);
        }
        return handler;
    }

    public LongTaskHandler require(String taskType, int payloadVersion) {
        LongTaskHandler handler = require(taskType);
        if (payloadVersion <= 0 || handler.payloadVersion() != payloadVersion) {
            throw new ServiceException(ServiceExceptionEnum.TASK_PAYLOAD_INVALID)
                .setDetailMessage("Unsupported payload version for task type " + taskType);
        }
        return handler;
    }

    public boolean contains(String taskType) {
        return handlers.containsKey(taskType);
    }

    public Map<String, LongTaskHandler> all() {
        return handlers;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
