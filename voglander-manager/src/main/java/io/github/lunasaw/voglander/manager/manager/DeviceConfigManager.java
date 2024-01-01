package io.github.lunasaw.voglander.manager.manager;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.github.lunasaw.voglander.common.constant.DeviceConstant;
import io.github.lunasaw.voglander.manager.service.DeviceConfigService;
import io.github.lunasaw.voglander.repository.entity.DeviceConfigDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Optional;

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
        Assert.notNull(deviceId, "deviceId can not be null");

        Assert.notNull(key, "key can not be null");
        QueryWrapper<DeviceConfigDO> queryWrapper = new QueryWrapper<DeviceConfigDO>().eq("device_id", deviceId).eq("config_key", key).last("limit 1");
        return deviceConfigService.getOne(queryWrapper);
    }

}
