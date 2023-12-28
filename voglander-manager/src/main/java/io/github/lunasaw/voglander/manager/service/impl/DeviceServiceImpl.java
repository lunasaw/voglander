package io.github.lunasaw.voglander.manager.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.mapper.DeviceMapper;
import io.github.lunasaw.voglander.repository.domain.entity.DeviceDO;
import org.springframework.stereotype.Service;

/**
 * (DeviceDO)表服务实现类
 *
 * @author chenzhangyue
 * @since 2023-12-28 11:11:54
 */
@Service("deviceService")
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, DeviceDO> implements DeviceService {

}
