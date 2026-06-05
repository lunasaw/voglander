package io.github.lunasaw.voglander.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lunasaw.voglander.repository.entity.AlarmDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警表 Mapper。
 *
 * @author luna
 */
@Mapper
public interface AlarmMapper extends BaseMapper<AlarmDO> {
}
