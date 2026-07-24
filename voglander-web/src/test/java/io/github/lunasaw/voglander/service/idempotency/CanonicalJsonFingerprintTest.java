package io.github.lunasaw.voglander.service.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CanonicalJsonFingerprintTest {

    @Test
    void fingerprintSortsObjectFieldsRecursivelyButPreservesArrayOrder() {
        Map<String, Object> firstNested = new LinkedHashMap<String, Object>();
        firstNested.put("z", 2);
        firstNested.put("a", 1);
        Map<String, Object> first = new LinkedHashMap<String, Object>();
        first.put("payload", firstNested);
        first.put("owner", "7");
        first.put("items", Arrays.asList("a", "b"));

        Map<String, Object> secondNested = new LinkedHashMap<String, Object>();
        secondNested.put("a", 1);
        secondNested.put("z", 2);
        Map<String, Object> second = new LinkedHashMap<String, Object>();
        second.put("items", Arrays.asList("a", "b"));
        second.put("owner", "7");
        second.put("payload", secondNested);

        assertEquals("{\"items\":[\"a\",\"b\"],\"owner\":\"7\",\"payload\":{\"a\":1,\"z\":2}}",
            CanonicalJsonFingerprint.canonicalJson(first));
        assertEquals(CanonicalJsonFingerprint.sha256(first), CanonicalJsonFingerprint.sha256(second));

        second.put("items", Arrays.asList("b", "a"));
        assertNotEquals(CanonicalJsonFingerprint.sha256(first), CanonicalJsonFingerprint.sha256(second));
    }

    @Test
    void fingerprintUsesLowercaseFullSha256Hex() {
        assertEquals("015abd7f5cc57a2dd94b7590f04ad8084273905ee33ec5cebeae62276a97f862",
            CanonicalJsonFingerprint.sha256(Map.of("a", 1)));
    }
}
