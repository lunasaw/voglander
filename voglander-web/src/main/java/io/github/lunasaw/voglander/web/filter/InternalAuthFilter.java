package io.github.lunasaw.voglander.web.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 内部接口鉴权过滤器：仅拦截 /internal/** 路径，校验 IP 白名单 + HMAC 签名 + 时间戳。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "voglander.command.affinity-route.enabled", havingValue = "true", matchIfMissing = false)
public class InternalAuthFilter extends OncePerRequestFilter {

    private static final long TS_TOLERANCE_MS = 60_000L;

    @Value("${gateway.internal-auth.shared-secret:CHANGE_ME_IN_PROD}")
    private String secret;

    @Value("${gateway.internal-auth.allowed-ips:127.0.0.1}")
    private String allowedIpsRaw;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // IP 白名单校验
        String remoteIp = request.getRemoteAddr();
        List<String> allowedIps = Arrays.asList(allowedIpsRaw.split(","));
        if (!allowedIps.contains(remoteIp.trim())) {
            log.warn("internal-auth::IP 不在白名单, ip={}", remoteIp);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "IP not allowed");
            return;
        }

        // 时间戳校验
        String tsHeader = request.getHeader("X-Internal-Ts");
        if (tsHeader == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "missing timestamp");
            return;
        }
        long ts;
        try { ts = Long.parseLong(tsHeader); } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid timestamp");
            return;
        }
        if (Math.abs(System.currentTimeMillis() - ts) > TS_TOLERANCE_MS) {
            log.warn("internal-auth::时间戳超期, ts={}", ts);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "timestamp expired");
            return;
        }

        // HMAC 校验
        String nodeId = request.getHeader("X-Node-Id");
        String sigHeader = request.getHeader("X-Internal-Sig");
        if (sigHeader == null || nodeId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "missing auth headers");
            return;
        }
        try {
            String expected = hmacSha256(secret, nodeId + ":" + tsHeader);
            if (!expected.equals(sigHeader)) {
                log.warn("internal-auth::HMAC 校验失败, nodeId={}", nodeId);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid signature");
                return;
            }
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "auth error");
            return;
        }

        chain.doFilter(request, response);
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
