package io.github.lunasaw.voglander.service.sse;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/** Shared local/Redis delivery decision for an immutable subscription context. */
@Component
public class SseDeliveryAuthorizer {

    public boolean allow(SseSubscriptionContext context, SseEvent event) {
        if (context == null || event == null || !matches(context.getTopics(), event.getTopic())) return false;
        String root = SseSubscriptionContext.rootOf(event.getTopic());
        if ("image.asset".equals(root)) return context.isImageAssetQueryAllowed();
        if (!"business.task".equals(root)) return true;
        if (context.isTaskQueryAllowed()) return true;
        String taskType = taskType(event.getData());
        return taskType != null && context.isImageCollectionQueryAllowed()
            && context.getAllowedTaskTypes() != null && context.getAllowedTaskTypes().contains(taskType);
    }

    private String taskType(Object data) {
        if (data instanceof Map<?, ?>) {
            Object value = ((Map<?, ?>)data).get("taskType");
            return value == null ? null : String.valueOf(value);
        }
        if (data == null) return null;
        JSONObject object = JSON.parseObject(JSON.toJSONString(data));
        return object == null ? null : object.getString("taskType");
    }

    private boolean matches(Set<String> subscribed, String topic) {
        if (subscribed == null || topic == null) return false;
        if (subscribed.contains(topic)) return true;
        for (String candidate : subscribed) {
            if (candidate != null && !candidate.isEmpty() && topic.startsWith(candidate + ".")) return true;
        }
        return false;
    }
}
