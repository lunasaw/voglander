package io.github.lunasaw.voglander.manager.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lunasaw.voglander.manager.service.BizTaskExecutionService;
import io.github.lunasaw.voglander.repository.entity.BizTaskExecutionDO;
import io.github.lunasaw.voglander.repository.mapper.BizTaskExecutionMapper;

@Service
public class BizTaskExecutionServiceImpl extends ServiceImpl<BizTaskExecutionMapper, BizTaskExecutionDO>
    implements BizTaskExecutionService {
}
