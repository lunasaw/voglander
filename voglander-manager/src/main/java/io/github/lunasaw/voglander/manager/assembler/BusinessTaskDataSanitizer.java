package io.github.lunasaw.voglander.manager.assembler;

import java.util.Locale;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

/** Removes sensitive fields from small JSON summaries before they cross a query boundary. */
public final class BusinessTaskDataSanitizer {
    private static final int MAX_JSON_LENGTH = 8192;
    private static final int MAX_MESSAGE_LENGTH = 512;

    private BusinessTaskDataSanitizer() {
    }

    public static String sanitizeJson(String source) {
        if (source == null || source.trim().isEmpty()) {
            return source;
        }
        try {
            Object parsed = JSON.parse(source);
            Object sanitized = sanitizeValue(parsed);
            String result = JSON.toJSONString(sanitized);
            return result.length() <= MAX_JSON_LENGTH ? result : "{\"truncated\":true}";
        } catch (RuntimeException exception) {
            return "{}";
        }
    }

    /** Keeps failure/progress text bounded and strips accidental line-oriented diagnostics. */
    public static String sanitizeMessage(String source) {
        if (source == null) {
            return null;
        }
        String normalized = source.replace('\r', ' ').replace('\n', ' ').trim();
        return normalized.length() <= MAX_MESSAGE_LENGTH ? normalized
            : normalized.substring(0, MAX_MESSAGE_LENGTH);
    }

    private static Object sanitizeValue(Object value) {
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject)value;
            JSONObject sanitized = new JSONObject();
            for (Map.Entry<String, Object> entry : object.entrySet()) {
                if (!isSensitiveKey(entry.getKey())) {
                    sanitized.put(entry.getKey(), sanitizeValue(entry.getValue()));
                }
            }
            return sanitized;
        }
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray)value;
            JSONArray sanitized = new JSONArray();
            for (Object item : array) {
                sanitized.add(sanitizeValue(item));
            }
            return sanitized;
        }
        return value;
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        return normalized.contains("password") || normalized.contains("secret") || normalized.contains("token")
            || normalized.contains("authorization") || normalized.contains("credential")
            || normalized.contains("claim") || normalized.contains("storagekey")
            || normalized.contains("storagebucket") || normalized.contains("storagenode")
            || normalized.contains("path") || normalized.contains("stack") || normalized.contains("base64")
            || normalized.contains("jwt");
    }
}
