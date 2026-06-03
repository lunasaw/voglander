package io.github.lunasaw.voglander.manager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lunasaw.voglander.manager.service.CascadeChannelService;
import io.github.lunasaw.voglander.repository.entity.CascadeChannelDO;
import io.github.lunasaw.voglander.repository.mapper.CascadeChannelMapper;
import org.springframework.stereotype.Service;

@Service
public class CascadeChannelServiceImpl extends ServiceImpl<CascadeChannelMapper, CascadeChannelDO>
    implements CascadeChannelService {
}
