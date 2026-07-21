package io.github.lunasaw.voglander.manager.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lunasaw.voglander.manager.service.BizTaskService;
import io.github.lunasaw.voglander.repository.entity.BizTaskDO;
import io.github.lunasaw.voglander.repository.mapper.BizTaskMapper;

@Service
public class BizTaskServiceImpl extends ServiceImpl<BizTaskMapper, BizTaskDO> implements BizTaskService {
}
