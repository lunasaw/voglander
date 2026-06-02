package io.github.lunasaw.voglander.intergration.zlm;

import io.github.lunasaw.voglander.intergration.wrapper.zlm.auth.ZlmHookAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class ZlmHookAuthServiceTest {

    private ZlmHookAuthService service;

    @BeforeEach
    void setUp() {
        service = new ZlmHookAuthService();
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "playSecret", "test-secret");
        ReflectionTestUtils.setField(service, "publishSecret", "test-secret");
        ReflectionTestUtils.setField(service, "ipWhitelist", "127.0.0.1");
    }

    @Test
    void validatePlay_disabled_returnsTrue() {
        ReflectionTestUtils.setField(service, "enabled", false);
        assertTrue(service.validatePlay("1.2.3.4", "live", "stream1"));
    }

    @Test
    void validatePlay_ipNotInWhitelist_returnsFalse() {
        assertFalse(service.validatePlay("1.2.3.4", "live", "stream1"));
    }

    @Test
    void validatePlay_validToken_returnsTrue() throws Exception {
        String app = "live";
        String streamBase = "stream1";
        String ts = String.valueOf(System.currentTimeMillis() / 1000);
        String data = app + "&" + streamBase + "&" + ts;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String token = HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        String stream = streamBase + "?token=" + token + "&ts=" + ts;
        assertTrue(service.validatePlay("1.2.3.4", app, stream));
    }

    @Test
    void validatePlay_expiredTimestamp_returnsFalse() throws Exception {
        String app = "live";
        String streamBase = "stream1";
        String ts = String.valueOf(System.currentTimeMillis() / 1000 - 400);
        String data = app + "&" + streamBase + "&" + ts;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String token = HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        String stream = streamBase + "?token=" + token + "&ts=" + ts;
        assertFalse(service.validatePlay("1.2.3.4", app, stream));
    }
}
