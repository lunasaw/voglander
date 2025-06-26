package io.github.lunasaw.voglander.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lunasaw.voglander.repository.entity.RoleDO;
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
     * 根据角色编码查询角色
     *
     * @param roleCode 角色编码
     * @return 角色信息
     */
    @Select("SELECT * FROM tb_role WHERE role_code = #{roleCode}")
    RoleDO selectByRoleCode(@Param("roleCode") String roleCode);
}