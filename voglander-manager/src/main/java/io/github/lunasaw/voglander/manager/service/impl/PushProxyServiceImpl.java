package io.github.lunasaw.voglander.manager.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lunasaw.voglander.manager.service.PushProxyService;
import io.github.lunasaw.voglander.repository.entity.PushProxyDO;
import io.github.lunasaw.voglander.repository.mapper.PushProxyMapper;

/**
 * 推流代理服务实现
 *
 * @author luna
 * @since 2025-01-23
 */
@Service
public class PushProxyServiceImpl extends ServiceImpl<PushProxyMapper, PushProxyDO> implements PushProxyService {

}