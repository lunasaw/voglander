package io.github.lunasaw.voglander.manager.service.impl;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.common.util.JwtUtils;
import io.github.lunasaw.voglander.manager.domaon.dto.LoginDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.manager.UserManager;
import io.github.lunasaw.voglander.manager.service.AuthService;
import io.github.lunasaw.voglander.manager.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 认证服务实现类
 *
 * @author luna
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private UserManager userManager;

    @Override
    public String login(LoginDTO loginDTO) {
        // 参数校验
        if (loginDTO == null || StringUtils.isBlank(loginDTO.getUsername()) || StringUtils.isBlank(loginDTO.getPassword())) {
            throw new ServiceException(ServiceExceptionEnum.PARAM_ERROR.getCode(), "用户名和密码不能为空");
        }

        // 查询用户
        UserDTO user = userService.getUserByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new ServiceException(ServiceExceptionEnum.USER_NOT_FOUND.getCode(), "用户不存在");
        }

        // 验证密码
        if (!userService.validatePassword(loginDTO.getPassword(), user.getPassword())) {
            throw new ServiceException(ServiceExceptionEnum.PASSWORD_ERROR.getCode(), "密码错误");
        }

        // 检查用户状态
        if (user.getStatus() != 1) {
            throw new ServiceException(ServiceExceptionEnum.USER_DISABLED.getCode(), "用户已被禁用");
        }

        // 生成token
        String accessToken = JwtUtils.generateAccessToken(user.getId(), user.getUsername());

        // 更新最后登录时间
        userService.updateLastLoginTime(user.getId());

        log.info("用户登录成功: {}", user.getUsername());

        return accessToken;
    }

    @Override
    public void logout(Long userId) {
        log.info("用户退出登录: {}", userId);
        // 这里可以实现token黑名单机制
        // 暂时只记录日志
    }

    @Override
    public String refreshToken(Long userId) {
        UserDTO user = userService.getUserById(userId);
        if (user == null) {
            throw new ServiceException(ServiceExceptionEnum.USER_NOT_FOUND.getCode(), "用户不存在");
        }

        if (user.getStatus() != 1) {
            throw new ServiceException(ServiceExceptionEnum.USER_DISABLED.getCode(), "用户已被禁用");
        }

        return JwtUtils.generateAccessToken(user.getId(), user.getUsername());
    }

    @Override
    public List<String> getUserPermissions(Long userId) {
        return userManager.getUserPermissions(userId);
    }

    @Override
    public UserDTO getUserByToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }

        // 验证token
        if (!JwtUtils.validateToken(token)) {
            return null;
        }

        // 获取用户ID
        Long userId = JwtUtils.getUserId(token);
        if (userId == null) {
            return null;
        }

        // 查询用户信息
        UserDTO user = userService.getUserById(userId);
        if (user == null || user.getStatus() != 1) {
            return null;
        }

        // 查询用户权限
        List<String> permissions = getUserPermissions(userId);
        user.setPermissions(permissions);

        return user;
    }
}