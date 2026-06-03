package io.github.lunasaw.voglander.manager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lunasaw.voglander.manager.service.CascadePlatformService;
import io.github.lunasaw.voglander.repository.entity.CascadePlatformDO;
import io.github.lunasaw.voglander.repository.mapper.CascadePlatformMapper;
import org.springframework.stereotype.Service;

@Service
public class CascadePlatformServiceImpl extends ServiceImpl<CascadePlatformMapper, CascadePlatformDO>
    implements CascadePlatformService {
}
