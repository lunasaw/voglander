package io.github.lunasaw.voglander.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luna.common.check.Assert;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.common.util.PasswordUtils;
import io.github.lunasaw.voglander.manager.assembler.UserAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.manager.UserManager;
import io.github.lunasaw.voglander.manager.service.RoleService;
import io.github.lunasaw.voglander.manager.service.UserService;
import io.github.lunasaw.voglander.repository.entity.UserDO;
import io.github.lunasaw.voglander.repository.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务实现类
 *
 * @author luna
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    @Autowired
    private UserManager userManager;

    @Autowired
    private RoleService roleService;

    @Override
    public UserDTO getUserByUsername(String username) {
        UserDO userDO = userManager.getUserByUsername(username);
        UserDTO userDTO = UserAssembler.toDTO(userDO);
        if (userDTO != null) {
            // 查询用户角色信息
            List<Long> roleIds = userManager.getUserRoleIds(userDTO.getId());
            userDTO.setRoleIds(roleIds);

            // 查询完整角色信息
            List<RoleDTO> roles = roleService.getRolesByUserId(userDTO.getId());
            userDTO.setRoles(roles);
        }
        return userDTO;
    }

    @Override
    public UserDTO getUserById(Long userId) {
        UserDO userDO = this.getById(userId);
        UserDTO userDTO = UserAssembler.toDTO(userDO);
        if (userDTO != null) {
            // 查询用户角色信息
            List<Long> roleIds = userManager.getUserRoleIds(userId);
            userDTO.setRoleIds(roleIds);

            // 查询完整角色信息
            List<RoleDTO> roles = roleService.getRolesByUserId(userId);
            userDTO.setRoles(roles);
        }
        return userDTO;
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
        this.update(null, updateWrapper);
    }

    @Override
    public IPage<UserDTO> getUserList(UserDTO dto) {
        Page<UserDO> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(dto.getUsername()), UserDO::getUsername, dto.getUsername())
            .like(StringUtils.isNotBlank(dto.getNickname()), UserDO::getNickname, dto.getNickname())
            .like(StringUtils.isNotBlank(dto.getEmail()), UserDO::getEmail, dto.getEmail())
            .like(StringUtils.isNotBlank(dto.getPhone()), UserDO::getPhone, dto.getPhone())
            .eq(dto.getStatus() != null, UserDO::getStatus, dto.getStatus())
            .orderByDesc(UserDO::getCreateTime);

        IPage<UserDO> pageResult = this.page(page, queryWrapper);
        IPage<UserDTO> result = UserAssembler.toDTOPage(pageResult);

        // 为每个用户查询角色信息
        if (result.getRecords() != null) {
            result.getRecords().forEach(userDTO -> {
                List<Long> roleIds = userManager.getUserRoleIds(userDTO.getId());
                userDTO.setRoleIds(roleIds);

                // 查询完整角色信息
                List<RoleDTO> roles = roleService.getRolesByUserId(userDTO.getId());
                userDTO.setRoles(roles);
            });
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserDTO dto) {
        Assert.notNull(dto, "用户信息不能为空");
        Assert.hasText(dto.getUsername(), "用户名不能为空");
        Assert.hasText(dto.getPassword(), "密码不能为空");

        // 检查用户名是否已存在
        if (isUsernameExists(dto.getUsername(), null)) {
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "用户名已存在");
        }

        // 检查邮箱是否已存在
        if (StringUtils.isNotBlank(dto.getEmail()) && isEmailExists(dto.getEmail(), null)) {
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "邮箱已存在");
        }

        // 检查手机号是否已存在
        if (StringUtils.isNotBlank(dto.getPhone()) && isPhoneExists(dto.getPhone(), null)) {
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "手机号已存在");
        }

        UserDO userDO = UserAssembler.toDO(dto);
        userDO.setCreateTime(LocalDateTime.now());
        userDO.setUpdateTime(LocalDateTime.now());

        // 加密密码
        userDO.setPassword(PasswordUtils.encode(dto.getPassword()));

        boolean result = this.save(userDO);
        if (result) {
            log.info("创建用户成功，用户名：{}，用户ID：{}", dto.getUsername(), userDO.getId());

            // 处理用户角色关系
            if (!CollectionUtils.isEmpty(dto.getRoleIds())) {
                updateUserRoles(userDO.getId(), dto.getRoleIds());
                log.info("用户角色关系创建成功，用户ID：{}，角色数量：{}", userDO.getId(), dto.getRoleIds().size());
            }

            return userDO.getId();
        }

        throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "创建用户失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(Long id, UserDTO dto) {
        Assert.notNull(id, "用户ID不能为空");
        Assert.notNull(dto, "用户信息不能为空");

        UserDO existUser = this.getById(id);
        if (existUser == null) {
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "用户不存在");
        }

        // 检查邮箱是否已存在
        if (StringUtils.isNotBlank(dto.getEmail()) && isEmailExists(dto.getEmail(), id)) {
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "邮箱已存在");
        }

        // 检查手机号是否已存在
        if (StringUtils.isNotBlank(dto.getPhone()) && isPhoneExists(dto.getPhone(), id)) {
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "手机号已存在");
        }

        UserDO userDO = new UserDO();
        userDO.setId(id);
        userDO.setNickname(dto.getNickname());
        userDO.setEmail(dto.getEmail());
        userDO.setPhone(dto.getPhone());
        userDO.setAvatar(dto.getAvatar());
        userDO.setStatus(dto.getStatus());
        userDO.setUpdateTime(LocalDateTime.now());

        // 如果密码不为空，则更新密码
        if (StringUtils.isNotBlank(dto.getPassword())) {
            userDO.setPassword(PasswordUtils.encode(dto.getPassword()));
        }

        boolean result = this.updateById(userDO);
        if (result) {
            log.info("更新用户成功，用户ID：{}", id);

            // 处理用户角色关系（无论角色列表是否为空都进行更新）
            updateUserRoles(id, dto.getRoleIds());
            log.info("用户角色关系更新成功，用户ID：{}，角色数量：{}",
                id, dto.getRoleIds() != null ? dto.getRoleIds().size() : 0);

            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long id) {
        Assert.notNull(id, "用户ID不能为空");

        UserDO userDO = this.getById(id);
        if (userDO == null) {
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "用户不存在");
        }

        // 先删除用户角色关系
        userManager.deleteUserRolesByUserId(id);
        log.info("删除用户角色关系成功，用户ID：{}", id);

        // 删除用户
        boolean result = this.removeById(id);
        if (result) {
            log.info("删除用户成功，用户ID：{}", id);
            return true;
        }
        return false;
    }

    @Override
    public boolean isUsernameExists(String username, Long excludeId) {
        if (StringUtils.isBlank(username)) {
            return false;
        }

        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDO::getUsername, username);

        if (excludeId != null) {
            queryWrapper.ne(UserDO::getId, excludeId);
        }

        return this.count(queryWrapper) > 0;
    }

    @Override
    public boolean isEmailExists(String email, Long excludeId) {
        if (StringUtils.isBlank(email)) {
            return false;
        }

        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDO::getEmail, email);

        if (excludeId != null) {
            queryWrapper.ne(UserDO::getId, excludeId);
        }

        return this.count(queryWrapper) > 0;
    }

    @Override
    public boolean isPhoneExists(String phone, Long excludeId) {
        if (StringUtils.isBlank(phone)) {
            return false;
        }

        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDO::getPhone, phone);

        if (excludeId != null) {
            queryWrapper.ne(UserDO::getId, excludeId);
        }

        return this.count(queryWrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserRoles(Long userId, List<Long> roleIds) {
        return userManager.updateUserRoles(userId, roleIds);
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        return userManager.getUserRoleIds(userId);
    }
}