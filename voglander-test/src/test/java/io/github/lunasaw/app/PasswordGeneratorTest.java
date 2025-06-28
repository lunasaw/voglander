package io.github.lunasaw.app;

import io.github.lunasaw.voglander.common.util.PasswordUtils;
import org.junit.jupiter.api.Test;

/**
 * 密码生成测试
 *
 * @author luna
 */
public class PasswordGeneratorTest {

    @Test
    public void generateAdminPassword() {
        String password = "admin123";
        String encodedPassword = PasswordUtils.encode(password);
        System.out.println("原始密码: " + password);
        System.out.println("加密后密码: " + encodedPassword);

        // 验证密码
        boolean matches = PasswordUtils.matches(password, encodedPassword);
        System.out.println("密码验证结果: " + matches);
    }
}