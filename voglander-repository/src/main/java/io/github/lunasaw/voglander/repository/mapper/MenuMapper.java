package io.github.lunasaw.voglander.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lunasaw.voglander.repository.entity.MenuDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单Mapper
 *
 * @author luna
 */
@Mapper
public interface MenuMapper extends BaseMapper<MenuDO> {

    /**
     * 根据角色ID查询菜单列表
     *
     * @param roleId 角色ID
     * @return 菜单列表
     */
    @Select("SELECT m.* FROM tb_menu m " +
        "INNER JOIN tb_role_menu rm ON m.id = rm.menu_id " +
        "WHERE rm.role_id = #{roleId} AND m.status = 1")
    List<MenuDO> selectMenusByRoleId(@Param("roleId") Long roleId);
}