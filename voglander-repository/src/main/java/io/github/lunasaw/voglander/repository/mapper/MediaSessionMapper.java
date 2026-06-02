package io.github.lunasaw.voglander.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lunasaw.voglander.repository.entity.MediaSessionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 媒体会话映射器
 *
 * @author luna
 * @since 2025-05-29
 */
@Mapper
public interface MediaSessionMapper extends BaseMapper<MediaSessionDO> {

}
