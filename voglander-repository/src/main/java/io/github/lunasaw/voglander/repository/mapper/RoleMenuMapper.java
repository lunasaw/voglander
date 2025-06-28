package io.github.lunasaw.voglander.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lunasaw.voglander.repository.entity.RoleMenuDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色菜单关联Mapper
 *
 * @author luna
 */
@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenuDO> {

}