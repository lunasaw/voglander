package io.github.lunasaw.voglander.intergration.wrapper.zlm.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;

@Slf4j
@Service
public class ZlmHookAuthService {

    @Value("${zlm.hook.auth.enabled:true}")
    private boolean enabled;

    @Value("${zlm.hook.auth.play-secret:}")
    private String playSecret;

    @Value("${zlm.hook.auth.publish-secret:}")
    private String publishSecret;

    @Value("${zlm.hook.auth.ip-whitelist:127.0.0.1}")
    private String ipWhitelist;

    public boolean validatePlay(String ip, String app, String stream) {
        return validate(ip, app, stream, playSecret);
    }

    public boolean validatePublish(String ip, String app, String stream) {
        return validate(ip, app, stream, publishSecret);
    }

    private boolean validate(String ip, String app, String stream, String secret) {
        if (!enabled) {
            return true;
        }
        if (isIpAllowed(ip)) {
            return true;
        }
        if (stream != null && stream.contains("?")) {
            return validateToken(app, stream, secret);
        }
        return false;
    }

    private boolean isIpAllowed(String ip) {
        if (ip == null) {
            return false;
        }
        return Arrays.asList(ipWhitelist.split(",")).stream()
                .map(String::trim)
                .anyMatch(allowed -> allowed.equals(ip));
    }

    private boolean validateToken(String app, String stream, String secret) {
        int idx = stream.indexOf('?');
        String streamBase = stream.substring(0, idx);
        String query = stream.substring(idx + 1);

        String token = null;
        String ts = null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2) {
                if ("token".equals(kv[0])) token = kv[1];
                if ("ts".equals(kv[0])) ts = kv[1];
            }
        }

        if (token == null || ts == null) {
            return false;
        }

        try {
            long timestamp = Long.parseLong(ts);
            long now = System.currentTimeMillis() / 1000;
            if (Math.abs(now - timestamp) > 300) {
                return false;
            }
            String data = app + "&" + streamBase + "&" + ts;
            String expected = hmacSha256(secret, data);
            return expected.equalsIgnoreCase(token);
        } catch (Exception e) {
            log.warn("token验证异常: {}", e.getMessage());
            return false;
        }
    }

    private String hmacSha256(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(bytes);
    }
}
