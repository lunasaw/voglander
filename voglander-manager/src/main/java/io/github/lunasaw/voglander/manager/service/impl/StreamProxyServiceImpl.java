package io.github.lunasaw.voglander.manager.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lunasaw.voglander.manager.service.StreamProxyService;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
import io.github.lunasaw.voglander.repository.mapper.StreamProxyMapper;

/**
 * 拉流代理服务实现
 *
 * @author luna
 * @since 2025-01-23
 */
@Service
public class StreamProxyServiceImpl extends ServiceImpl<StreamProxyMapper, StreamProxyDO> implements StreamProxyService {

}