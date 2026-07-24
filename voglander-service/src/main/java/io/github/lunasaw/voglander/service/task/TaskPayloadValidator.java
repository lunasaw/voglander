package io.github.lunasaw.voglander.service.task;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.service.idempotency.CanonicalJsonFingerprint;

/** Validates and freezes trusted domain payloads before persistence. */
final class TaskPayloadValidator {

    private static final Pattern WINDOWS_ABSOLUTE_PATH = Pattern.compile("^[A-Za-z]:[\\\\/].*");
    private static final Pattern BASE64_TEXT = Pattern.compile("^[A-Za-z0-9+/]+={0,2}$");
    private static final Pattern EMBEDDED_CREDENTIAL = Pattern.compile(
        "(?i).*(password|passwd|credential|secret|authorization|access[_-]?token|refresh[_-]?token)\\s*[:=].*");

    private TaskPayloadValidator() {
    }

    static String validateAndSerialize(JSONObject payload) {
        String serialized;
        try {
            serialized = JSON.toJSONString(payload == null ? new JSONObject() : payload);
        } catch (RuntimeException exception) {
            throw invalid("Task payload cannot be serialized");
        }
        if (serialized.getBytes(StandardCharsets.UTF_8).length > TaskConstant.DEFAULT_MAX_PAYLOAD_BYTES) {
            throw invalid("Task payload exceeds the UTF-8 byte limit");
        }
        Object snapshot;
        try {
            snapshot = JSON.parse(serialized);
        } catch (RuntimeException exception) {
            throw invalid("Task payload is not valid JSON");
        }
        validateValue(snapshot);
        return CanonicalJsonFingerprint.canonicalJson((JSONObject)snapshot);
    }

    static JSONObject copyOf(String serialized) {
        return JSON.parseObject(serialized);
    }

    private static void validateValue(Object value) {
        if (value instanceof JSONObject) {
            for (Map.Entry<String, Object> entry : ((JSONObject)value).entrySet()) {
                if (isSensitiveKey(entry.getKey())) {
                    throw invalid("Task payload contains a prohibited field");
                }
                validateValue(entry.getValue());
            }
            return;
        }
        if (value instanceof JSONArray) {
            for (Object item : (JSONArray)value) {
                validateValue(item);
            }
            return;
        }
        if (value instanceof String && isSensitiveContent((String)value)) {
            throw invalid("Task payload contains prohibited content");
        }
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        return normalized.contains("password") || normalized.contains("passwd")
            || normalized.contains("credential") || normalized.contains("secret") || normalized.contains("token")
            || normalized.contains("authorization") || normalized.contains("base64") || normalized.contains("jwt")
            || normalized.contains("privatekey") || normalized.contains("apikey")
            || normalized.contains("accesskey");
    }

    private static boolean isSensitiveContent(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (trimmed.startsWith("/") || trimmed.startsWith("\\\\")
            || WINDOWS_ABSOLUTE_PATH.matcher(trimmed).matches() || lower.startsWith("file:/")) {
            return true;
        }
        if ((lower.startsWith("data:") && lower.contains(";base64,")) || lower.startsWith("base64:")
            || lower.startsWith("bearer ") || lower.startsWith("basic ")) {
            return true;
        }
        if (EMBEDDED_CREDENTIAL.matcher(trimmed).matches()) {
            return true;
        }
        return looksLikeBase64Binary(trimmed);
    }

    private static boolean looksLikeBase64Binary(String value) {
        if (value.length() < 32 || value.length() % 4 != 0 || !BASE64_TEXT.matcher(value).matches()) {
            return false;
        }
        boolean hasEncodingMarker = value.endsWith("=") || value.indexOf('+') >= 0 || value.indexOf('/') >= 0;
        if (!hasEncodingMarker) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            int nonTextBytes = 0;
            for (byte item : decoded) {
                int unsigned = item & 0xff;
                if (unsigned == 0 || (unsigned < 0x20 && unsigned != '\r' && unsigned != '\n'
                    && unsigned != '\t')) {
                    nonTextBytes++;
                }
            }
            return decoded.length > 0 && nonTextBytes * 4 >= decoded.length;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static ServiceException invalid(String detailMessage) {
        return new ServiceException(ServiceExceptionEnum.TASK_PAYLOAD_INVALID).setDetailMessage(detailMessage);
    }
}
