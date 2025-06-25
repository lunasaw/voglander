package io.github.lunasaw.voglander.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码工具类
 *
 * @author luna
 */
@Slf4j
public class PasswordUtils {

    /**
     * 盐值长度
     */
    private static final int SALT_LENGTH = 16;

    /**
     * 迭代次数
     */
    private static final int ITERATIONS  = 10000;

    /**
     * 加密密码（BCrypt风格的简化实现）
     *
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public static String encode(String rawPassword) {
        if (StringUtils.isBlank(rawPassword)) {
            throw new IllegalArgumentException("密码不能为空");
        }

        try {
            // 生成随机盐值
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            // 加密密码
            byte[] hash = hashPassword(rawPassword, salt);

            // 组合盐值和哈希值
            byte[] combined = new byte[salt.length + hash.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hash, 0, combined, salt.length, hash.length);

            return "$2a$10$" + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("密码加密失败", e);
            throw new RuntimeException("密码加密失败", e);
        }
    }

    /**
     * 验证密码
     *
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (StringUtils.isBlank(rawPassword) || StringUtils.isBlank(encodedPassword)) {
            return false;
        }

        try {
            // 解析加密后的密码
            if (!encodedPassword.startsWith("$2a$10$")) {
                return false;
            }

            String encodedPart = encodedPassword.substring(7);
            byte[] combined = Base64.getDecoder().decode(encodedPart);

            if (combined.length < SALT_LENGTH + 32) {
                return false;
            }

            // 提取盐值和哈希值
            byte[] salt = new byte[SALT_LENGTH];
            byte[] hash = new byte[combined.length - SALT_LENGTH];
            System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);
            System.arraycopy(combined, SALT_LENGTH, hash, 0, hash.length);

            // 使用相同的盐值加密输入的密码
            byte[] testHash = hashPassword(rawPassword, salt);

            // 比较哈希值
            return MessageDigest.isEqual(hash, testHash);
        } catch (Exception e) {
            log.warn("密码验证失败", e);
            return false;
        }
    }

    /**
     * 生成默认管理员密码
     *
     * @return 加密后的默认密码
     */
    public static String getDefaultAdminPassword() {
        return encode("admin123");
    }

    /**
     * 使用PBKDF2算法加密密码
     *
     * @param password 密码
     * @param salt 盐值
     * @return 哈希值
     */
    private static byte[] hashPassword(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);

            byte[] hash = password.getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < ITERATIONS; i++) {
                md.reset();
                md.update(salt);
                hash = md.digest(hash);
            }

            return hash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    /**
     * 生成随机密码
     *
     * @param length 密码长度
     * @return 随机密码
     */
    public static String generateRandomPassword(int length) {
        if (length < 6) {
            throw new IllegalArgumentException("密码长度不能小于6位");
        }

        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }

    /**
     * 验证密码强度
     *
     * @param password 密码
     * @return 是否符合强度要求
     */
    public static boolean isStrongPassword(String password) {
        if (StringUtils.isBlank(password) || password.length() < 8) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if ("!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(c) >= 0) {
                hasSpecial = true;
            }
        }

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}