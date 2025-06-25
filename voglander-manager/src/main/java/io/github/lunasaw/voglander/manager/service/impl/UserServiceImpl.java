package io.github.lunasaw.voglander.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.lunasaw.voglander.common.util.PasswordUtils;
import io.github.lunasaw.voglander.manager.assembler.UserAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.service.UserService;
import io.github.lunasaw.voglander.repository.entity.UserDO;
import io.github.lunasaw.voglander.repository.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户服务实现类
 *
 * @author luna
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDTO getUserByUsername(String username) {
        UserDO userDO = userMapper.selectByUsername(username);
        return UserAssembler.toDTO(userDO);
    }

    @Override
    public UserDTO getUserById(Long userId) {
        UserDO userDO = userMapper.selectById(userId);
        return UserAssembler.toDTO(userDO);
    }

    @Override
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return PasswordUtils.matches(rawPassword, encodedPassword);
    }

    @Override
    public void updateLastLoginTime(Long userId) {
        LambdaUpdateWrapper<UserDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserDO::getId, userId)
            .set(UserDO::getLastLogin, LocalDateTime.now());
        userMapper.update(null, updateWrapper);
    }
}