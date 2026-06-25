package io.github.lunasaw.voglander.manager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lunasaw.voglander.manager.service.CascadeSubscribeService;
import io.github.lunasaw.voglander.repository.entity.CascadeSubscribeDO;
import io.github.lunasaw.voglander.repository.mapper.CascadeSubscribeMapper;
import org.springframework.stereotype.Service;

@Service
public class CascadeSubscribeServiceImpl extends ServiceImpl<CascadeSubscribeMapper, CascadeSubscribeDO>
    implements CascadeSubscribeService {
}
