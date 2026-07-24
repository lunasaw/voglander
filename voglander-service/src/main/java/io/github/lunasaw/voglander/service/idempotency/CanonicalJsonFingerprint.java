package io.github.lunasaw.voglander.service.idempotency;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.alibaba.fastjson2.JSON;

/** Produces deterministic JSON and a full SHA-256 digest for explicit business fields. */
public final class CanonicalJsonFingerprint {

    private CanonicalJsonFingerprint() {
    }

    public static String canonicalJson(Map<String, ?> fields) {
        return JSON.toJSONString(canonicalize(fields));
    }

    public static String sha256(Map<String, ?> fields) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(canonicalJson(fields).getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                value.append(Character.forDigit((item >>> 4) & 0x0f, 16));
                value.append(Character.forDigit(item & 0x0f, 16));
            }
            return value.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static Object canonicalize(Object value) {
        if (value instanceof Map<?, ?>) {
            Map<?, ?> source = (Map<?, ?>)value;
            Map<String, Object> sorted = new TreeMap<String, Object>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    throw new IllegalArgumentException("Canonical JSON object keys must be strings");
                }
                sorted.put((String)entry.getKey(), canonicalize(entry.getValue()));
            }
            return sorted;
        }
        if (value instanceof Collection<?>) {
            Collection<?> source = (Collection<?>)value;
            List<Object> ordered = new ArrayList<Object>(source.size());
            for (Object item : source) {
                ordered.add(canonicalize(item));
            }
            return ordered;
        }
        if (value != null && value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> ordered = new ArrayList<Object>(length);
            for (int index = 0; index < length; index++) {
                ordered.add(canonicalize(Array.get(value, index)));
            }
            return ordered;
        }
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        throw new IllegalArgumentException("Unsupported canonical JSON value: " + value.getClass().getName());
    }
}
