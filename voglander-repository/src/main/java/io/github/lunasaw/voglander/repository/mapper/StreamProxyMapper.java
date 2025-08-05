package io.github.lunasaw.voglander.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 拉流代理映射器
 *
 * @author luna
 * @since 2025-01-23
 */
@Mapper
public interface StreamProxyMapper extends BaseMapper<StreamProxyDO> {

}