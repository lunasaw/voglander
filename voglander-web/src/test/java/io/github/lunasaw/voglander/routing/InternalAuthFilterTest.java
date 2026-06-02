package io.github.lunasaw.voglander.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.github.lunasaw.voglander.web.filter.InternalAuthFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;

/**
 * InternalAuthFilter 单元测试：覆盖 IP 白名单、HMAC 校验、时间戳过期三种失败场景。
 */
@ExtendWith(MockitoExtension.class)
public class InternalAuthFilterTest {

    private static final String SECRET   = "test-secret-123";
    private static final String NODE_ID  = "node-2";

    private InternalAuthFilter filter;

    @Mock
    private FilterChain        chain;

    @BeforeEach
    void setUp() throws Exception {
        filter = new InternalAuthFilter();
        setField(filter, "secret", SECRET);
        setField(filter, "allowedIpsRaw", "127.0.0.1,10.0.0.1");
    }

    @Test
    void testIpNotInWhitelistReturns403() throws Exception {
        MockHttpServletRequest req = newReq("8.8.8.8");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        long ts = System.currentTimeMillis();
        req.addHeader("X-Internal-Ts", String.valueOf(ts));
        req.addHeader("X-Node-Id", NODE_ID);
        req.addHeader("X-Internal-Sig", hmacSha256(SECRET, NODE_ID + ":" + ts));

        filter.doFilter(req, resp, chain);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, resp.getStatus());
        verify(chain, never()).doFilter(req, resp);
    }

    @Test
    void testInvalidHmacReturns401() throws Exception {
        MockHttpServletRequest req = newReq("127.0.0.1");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        long ts = System.currentTimeMillis();
        req.addHeader("X-Internal-Ts", String.valueOf(ts));
        req.addHeader("X-Node-Id", NODE_ID);
        req.addHeader("X-Internal-Sig", "deadbeef-wrong-sig");

        filter.doFilter(req, resp, chain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, resp.getStatus());
        verify(chain, never()).doFilter(req, resp);
    }

    @Test
    void testTimestampExpiredReturns401() throws Exception {
        MockHttpServletRequest req = newReq("127.0.0.1");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        long ts = System.currentTimeMillis() - 120_000L; // 2 分钟前，超过 ±60s
        req.addHeader("X-Internal-Ts", String.valueOf(ts));
        req.addHeader("X-Node-Id", NODE_ID);
        req.addHeader("X-Internal-Sig", hmacSha256(SECRET, NODE_ID + ":" + ts));

        filter.doFilter(req, resp, chain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, resp.getStatus());
        verify(chain, never()).doFilter(req, resp);
    }

    private static MockHttpServletRequest newReq(String remoteIp) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/internal/sip/command");
        req.setRequestURI("/internal/sip/command");
        req.setRemoteAddr(remoteIp);
        return req;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static String hmacSha256(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : raw) { sb.append(String.format("%02x", b)); }
        return sb.toString();
    }
}
