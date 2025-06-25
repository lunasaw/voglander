package io.github.lunasaw.voglander.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lunasaw.voglander.repository.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户Mapper
 *
 * @author luna
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Select("SELECT * FROM tb_user WHERE username = #{username} AND status = 1")
    UserDO selectByUsername(@Param("username") String username);

    /**
     * 根据用户ID查询用户角色权限
     *
     * @param userId 用户ID
     * @return 权限码列表
     */
    @Select("SELECT DISTINCT m.permission FROM tb_user u " +
        "INNER JOIN tb_user_role ur ON u.id = ur.user_id " +
        "INNER JOIN tb_role r ON ur.role_id = r.id " +
        "INNER JOIN tb_role_menu rm ON r.id = rm.role_id " +
        "INNER JOIN tb_menu m ON rm.menu_id = m.id " +
        "WHERE u.id = #{userId} AND u.status = 1 AND r.status = 1 AND m.status = 1 " +
        "AND m.permission IS NOT NULL AND m.permission != ''")
    List<String> selectUserPermissions(@Param("userId") Long userId);

    /**
     * 根据用户ID查询用户菜单
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    @Select("SELECT DISTINCT m.* FROM tb_user u " +
        "INNER JOIN tb_user_role ur ON u.id = ur.user_id " +
        "INNER JOIN tb_role r ON ur.role_id = r.id " +
        "INNER JOIN tb_role_menu rm ON r.id = rm.role_id " +
        "INNER JOIN tb_menu m ON rm.menu_id = m.id " +
        "WHERE u.id = #{userId} AND u.status = 1 AND r.status = 1 AND m.status = 1 AND m.visible = 1 " +
        "ORDER BY m.sort_order ASC")
    List<io.github.lunasaw.voglander.repository.entity.MenuDO> selectUserMenus(@Param("userId") Long userId);
}