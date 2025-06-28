package io.github.lunasaw.voglander.manager.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;

/**
 * 用户服务接口
 *
 * @author luna
 */
public interface UserService {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    UserDTO getUserByUsername(String username);

    /**
     * 根据用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    UserDTO getUserById(Long userId);

    /**
     * 验证用户密码
     *
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    boolean validatePassword(String rawPassword, String encodedPassword);

    /**
     * 更新用户最后登录时间
     *
     * @param userId 用户ID
     */
    void updateLastLoginTime(Long userId);

    /**
     * 分页查询用户列表
     *
     * @param dto 查询条件
     * @return 用户列表分页数据
     */
    IPage<UserDTO> getUserList(UserDTO dto);

    /**
     * 创建用户
     *
     * @param dto 用户信息
     * @return 创建的用户ID
     */
    Long createUser(UserDTO dto);

    /**
     * 更新用户
     *
     * @param id 用户ID
     * @param dto 用户信息
     * @return 是否更新成功
     */
    boolean updateUser(Long id, UserDTO dto);

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 是否删除成功
     */
    boolean deleteUser(Long id);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @param excludeId 排除的用户ID
     * @return 是否存在
     */
    boolean isUsernameExists(String username, Long excludeId);

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱
     * @param excludeId 排除的用户ID
     * @return 是否存在
     */
    boolean isEmailExists(String email, Long excludeId);

    /**
     * 检查手机号是否存在
     *
     * @param phone 手机号
     * @param excludeId 排除的用户ID
     * @return 是否存在
     */
    boolean isPhoneExists(String phone, Long excludeId);
}