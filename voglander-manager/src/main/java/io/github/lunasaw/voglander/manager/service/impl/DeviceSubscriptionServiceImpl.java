package io.github.lunasaw.voglander.manager.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import io.github.lunasaw.voglander.manager.service.DeviceSubscriptionService;
import io.github.lunasaw.voglander.repository.entity.DeviceSubscriptionDO;
import io.github.lunasaw.voglander.repository.mapper.DeviceSubscriptionMapper;

/**
 * 针对表【tb_device_subscription】的数据库操作 Service 实现。
 *
 * @author luna
 */
@Service
public class DeviceSubscriptionServiceImpl extends ServiceImpl<DeviceSubscriptionMapper, DeviceSubscriptionDO>
    implements DeviceSubscriptionService {

}
