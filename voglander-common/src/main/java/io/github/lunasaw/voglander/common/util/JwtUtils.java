package io.github.lunasaw.voglander.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 *
 * @author luna
 */
@Slf4j
public class JwtUtils {

    /**
     * JWT签名密钥
     */
    private static final String SECRET_KEY          = "voglander-jwt-secret-key-2024-very-long-secret-key-for-security";

    /**
     * JWT过期时间（小时）
     */
    private static final int    EXPIRE_HOURS        = 24;

    /**
     * 刷新token过期时间（天）
     */
    private static final int    REFRESH_EXPIRE_DAYS = 7;

    /**
     * JWT主题
     */
    private static final String SUBJECT             = "voglander-user";

    /**
     * 用户ID声明键
     */
    private static final String CLAIM_USER_ID       = "userId";

    /**
     * 用户名声明键
     */
    private static final String CLAIM_USERNAME      = "username";

    /**
     * 获取签名密钥
     */
    private static SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成访问token
     *
     * @param userId 用户ID
     * @param username 用户名
     * @return JWT token
     */
    public static String generateAccessToken(Long userId, String username) {
        LocalDateTime expireTime = LocalDateTime.now().plusHours(EXPIRE_HOURS);
        return generateToken(userId, username, expireTime);
    }

    /**
     * 生成刷新token
     *
     * @param userId 用户ID
     * @param username 用户名
     * @return JWT token
     */
    public static String generateRefreshToken(Long userId, String username) {
        LocalDateTime expireTime = LocalDateTime.now().plusDays(REFRESH_EXPIRE_DAYS);
        return generateToken(userId, username, expireTime);
    }

    /**
     * 生成token
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param expireTime 过期时间
     * @return JWT token
     */
    private static String generateToken(Long userId, String username, LocalDateTime expireTime) {
        Date expireDate = Date.from(expireTime.atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder()
            .setSubject(SUBJECT)
            .claim(CLAIM_USER_ID, userId)
            .claim(CLAIM_USERNAME, username)
            .setIssuedAt(new Date())
            .setExpiration(expireDate)
            .signWith(getSignKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * 解析token
     *
     * @param token JWT token
     * @return Claims
     */
    public static Claims parseToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }

        try {
            return Jwts.parser()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (Exception e) {
            log.warn("解析JWT token失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从token中获取用户ID
     *
     * @param token JWT token
     * @return 用户ID
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }

        Object userIdObj = claims.get(CLAIM_USER_ID);
        if (userIdObj instanceof Integer) {
            return ((Integer)userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            return (Long)userIdObj;
        }
        return null;
    }

    /**
     * 从token中获取用户名
     *
     * @param token JWT token
     * @return 用户名
     */
    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return claims.get(CLAIM_USERNAME, String.class);
    }

    /**
     * 验证token是否有效
     *
     * @param token JWT token
     * @return 是否有效
     */
    public static boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims != null && !isTokenExpired(claims);
        } catch (Exception e) {
            log.warn("验证JWT token失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查token是否过期
     *
     * @param claims JWT声明
     * @return 是否过期
     */
    private static boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }

    /**
     * 获取token过期时间
     *
     * @param token JWT token
     * @return 过期时间
     */
    public static Date getExpirationDate(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getExpiration() : null;
    }

    /**
     * 检查token是否即将过期（1小时内）
     *
     * @param token JWT token
     * @return 是否即将过期
     */
    public static boolean isTokenExpiringSoon(String token) {
        Date expiration = getExpirationDate(token);
        if (expiration == null) {
            return true;
        }

        long timeUntilExpiration = expiration.getTime() - System.currentTimeMillis();
        return timeUntilExpiration < 3600000; // 1小时
    }
}