package io.github.lunasaw.voglander.manager.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lunasaw.voglander.manager.service.BizTaskEventService;
import io.github.lunasaw.voglander.repository.entity.BizTaskEventDO;
import io.github.lunasaw.voglander.repository.mapper.BizTaskEventMapper;

@Service
public class BizTaskEventServiceImpl extends ServiceImpl<BizTaskEventMapper, BizTaskEventDO>
    implements BizTaskEventService {
}
