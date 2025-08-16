package io.github.lunasaw.voglander.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lunasaw.voglander.repository.entity.PushProxyDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 推流代理映射器
 *
 * @author luna
 * @since 2025-01-23
 */
@Mapper
public interface PushProxyMapper extends BaseMapper<PushProxyDO> {

}