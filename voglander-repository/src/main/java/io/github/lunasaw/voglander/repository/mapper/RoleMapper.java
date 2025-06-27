package io.github.lunasaw.voglander.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lunasaw.voglander.repository.entity.RoleDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色Mapper
 *
 * @author luna
 */
@Mapper
public interface RoleMapper extends BaseMapper<RoleDO> {

    /**
     * 根据用户ID查询角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @Select("SELECT r.* FROM tb_role r " +
        "INNER JOIN tb_user_role ur ON r.id = ur.role_id " +
        "WHERE ur.user_id = #{userId} AND r.status = 1")
    List<RoleDO> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 删除角色权限关联
     *
     * @param roleId 角色ID
     * @return 删除数量
     */
    @Delete("DELETE FROM tb_role_menu WHERE role_id = #{roleId}")
    int deleteRoleMenuByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入角色权限关联
     *
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     * @return 插入数量
     */
    @Insert("<script>" +
        "INSERT INTO tb_role_menu (role_id, menu_id) VALUES " +
        "<foreach collection='menuIds' item='menuId' separator=','>" +
        "(#{roleId}, #{menuId})" +
        "</foreach>" +
        "</script>")
    int batchInsertRoleMenu(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);
}