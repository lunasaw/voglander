package io.github.lunasaw.voglander.manager.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import io.github.lunasaw.voglander.manager.service.AlarmService;
import io.github.lunasaw.voglander.repository.entity.AlarmDO;
import io.github.lunasaw.voglander.repository.mapper.AlarmMapper;

/**
 * 告警核心服务实现。
 *
 * @author luna
 */
@Service
public class AlarmServiceImpl extends ServiceImpl<AlarmMapper, AlarmDO> implements AlarmService {
}
