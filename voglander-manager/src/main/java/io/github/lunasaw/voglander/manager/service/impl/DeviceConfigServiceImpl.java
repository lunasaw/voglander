package io.github.lunasaw.voglander.manager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lunasaw.voglander.manager.service.DeviceConfigService;
import io.github.lunasaw.voglander.repository.domain.entity.DeviceConfigDO;
import io.github.lunasaw.voglander.repository.mapper.DeviceConfigMapper;
import org.springframework.stereotype.Service;

/**
 * @author luna
 * @description 针对表【tb_device_config】的数据库操作Service实现
 * @createDate 2023-12-28 14:24:31
 */
@Service
public class DeviceConfigServiceImpl extends ServiceImpl<DeviceConfigMapper, DeviceConfigDO>
        implements DeviceConfigService {

}




