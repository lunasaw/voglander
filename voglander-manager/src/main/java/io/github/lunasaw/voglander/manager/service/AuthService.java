package io.github.lunasaw.voglander.manager.service;

import io.github.lunasaw.voglander.manager.domaon.dto.LoginDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;

import java.util.List;

/**
 * 认证服务接口
 *
 * @author luna
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return 登录结果
     */
    String login(LoginDTO loginDTO);

    /**
     * 用户登出
     *
     * @param userId 用户ID
     */
    void logout(Long userId);

    /**
     * 刷新token
     *
     * @param userId 用户ID
     * @return 新的token
     */
    String refreshToken(Long userId);

    /**
     * 获取用户权限码
     *
     * @param userId 用户ID
     * @return 权限码列表
     */
    List<String> getUserPermissions(Long userId);

    /**
     * 根据token获取用户信息
     *
     * @param token 访问令牌
     * @return 用户信息
     */
    UserDTO getUserByToken(String token);
}