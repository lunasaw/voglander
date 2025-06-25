package io.github.lunasaw.voglander.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lunasaw.voglander.repository.entity.MenuDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜单Mapper
 *
 * @author luna
 */
@Mapper
public interface MenuMapper extends BaseMapper<MenuDO> {

}