package io.github.lunasaw.voglander.repository.mapper;

import java.util.List;
import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import io.github.lunasaw.voglander.repository.entity.BizTaskEventDO;

@Mapper
public interface BizTaskEventMapper extends BaseMapper<BizTaskEventDO> {
    int insertIfAbsent(@Param("event") BizTaskEventDO event);
    List<BizTaskEventDO> selectTimeline(@Param("taskId") String taskId,
        @Param("executionId") String executionId, @Param("limit") int limit);

    List<Long> selectIdsBefore(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);

    int deleteByIds(@Param("ids") List<Long> ids);
}
