package io.github.lunasaw.voglander.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lunasaw.voglander.repository.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper
 *
 * 注意：原来的手写SQL查询已移至UserManager中，使用MyBatis Plus方式实现
 * - selectByUsername -> UserManager.getUserByUsername
 * - selectUserPermissions -> UserManager.getUserPermissions
 * - selectUserMenus -> UserManager.getUserMenus
 * - selectUserRoleIds -> UserManager.getUserRoleIds
 * - deleteUserRolesByUserId -> UserManager.deleteUserRolesByUserId
 * - batchInsertUserRoles -> UserManager.batchInsertUserRoles
 *
 * @author luna
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

}