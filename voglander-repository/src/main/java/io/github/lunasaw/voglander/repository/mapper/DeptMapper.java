package io.github.lunasaw.voglander.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lunasaw.voglander.repository.entity.DeptDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 部门Mapper
 *
 * @author luna
 */
@Mapper
public interface DeptMapper extends BaseMapper<DeptDO> {

}