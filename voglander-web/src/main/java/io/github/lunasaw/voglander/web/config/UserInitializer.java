package io.github.lunasaw.voglander.web.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.lunasaw.voglander.common.util.PasswordUtils;
import io.github.lunasaw.voglander.repository.entity.UserDO;
import io.github.lunasaw.voglander.repository.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 用户初始化器
 * 系统启动时自动创建默认管理员账户
 *
 * @author luna
 */
@Slf4j
@Component
public class UserInitializer implements CommandLineRunner {

    @Autowired
    private UserMapper userMapper;

    @Override
    public void run(String... args) throws Exception {
        initDefaultAdmin();
    }

    /**
     * 初始化默认管理员账户
     */
    private void initDefaultAdmin() {
        try {
            // 检查是否已存在管理员账户
            LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserDO::getUsername, "admin");
            UserDO existingAdmin = userMapper.selectOne(queryWrapper);

            if (existingAdmin != null) {
                log.info("管理员账户已存在，无需创建");
                return;
            }

            // 创建默认管理员账户
            UserDO adminUser = new UserDO();
            adminUser.setUsername("admin");
            adminUser.setPassword(PasswordUtils.encode("admin123"));
            adminUser.setNickname("系统管理员");
            adminUser.setEmail("admin@voglander.com");
            adminUser.setStatus(1);
            adminUser.setCreateTime(LocalDateTime.now());
            adminUser.setUpdateTime(LocalDateTime.now());

            int result = userMapper.insert(adminUser);
            if (result > 0) {
                log.info("默认管理员账户创建成功 - 用户名: admin, 密码: admin123");
            } else {
                log.error("默认管理员账户创建失败");
            }
        } catch (Exception e) {
            log.error("初始化默认管理员账户时发生错误", e);
        }
    }
}