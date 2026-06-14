package io.github.lunasaw.voglander.manager.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import io.github.lunasaw.voglander.manager.service.DevicePositionService;
import io.github.lunasaw.voglander.repository.entity.DevicePositionDO;
import io.github.lunasaw.voglander.repository.mapper.DevicePositionMapper;

/**
 * 针对表【tb_device_position】的数据库操作 Service 实现。
 *
 * @author luna
 */
@Service
public class DevicePositionServiceImpl extends ServiceImpl<DevicePositionMapper, DevicePositionDO>
    implements DevicePositionService {

}
