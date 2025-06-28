package io.github.lunasaw.voglander.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lunasaw.voglander.repository.entity.UserRoleDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联Mapper
 *
 * @author luna
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleDO> {

}