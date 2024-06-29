package io.github.lunasaw.voglander.manager.manager;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.luna.common.check.AssertUtil;

import io.github.lunasaw.voglander.common.constant.SqlConstant;
import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.manager.service.DeviceConfigService;
import io.github.lunasaw.voglander.repository.entity.DeviceConfigDO;

/**
 * @author luna
 * @date 2024/1/1
 */
@Component
public class DeviceConfigManager {

    @Autowired
    private DeviceConfigService deviceConfigService;

    public String getSystemValueWithDefault(String key, String defaultValue) {
        return getByValue(DeviceConstant.LocalConfig.DEVICE_ID, key, defaultValue);
    }

    public String getSystemValue(String key) {
        return getByValue(DeviceConstant.LocalConfig.DEVICE_ID, key, null);
    }

    public String getByValue(String deviceId, String key, String defaultValue) {
        DeviceConfigDO byKey = getByKey(deviceId, key);
        return Optional.ofNullable(byKey).map(DeviceConfigDO::getConfigValue).orElse(defaultValue);
    }

    public DeviceConfigDO getByKey(String deviceId, String key) {
        AssertUtil.notNull(deviceId, ServiceException.PARAMETER_ERROR);
        AssertUtil.notNull(key, ServiceException.PARAMETER_ERROR);

        LambdaQueryWrapper<DeviceConfigDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(DeviceConfigDO::getDeviceId, deviceId)
            .eq(DeviceConfigDO::getConfigKey, key).last(SqlConstant.LIMIT_ONE);

        return deviceConfigService.getOne(queryWrapper);
    }

}
