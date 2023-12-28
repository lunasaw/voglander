package io.github.lunasaw.voglander.repository.service;

import com.luna.common.check.Assert;
import com.luna.common.net.IPAddressUtil;
import com.luna.common.os.SystemInfoUtil;
import io.github.lunasaw.voglander.common.util.IpUtils;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.domain.entity.DeviceDO;
import io.github.lunasaw.voglander.web.ApplicationWeb;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

/**
 * @author luna
 * @date 2023/12/28
 */
@SpringBootTest(classes = ApplicationWeb.class)
public class DeviceServiceTest {

    @Autowired
    private DeviceService deviceService;

    @Test
    public void atest() {
        DeviceDO deviceDO = new DeviceDO();
        deviceDO.setDeviceId("1");
        deviceDO.setIp("0.0.0.0");
        deviceDO.setPort(8117);
        deviceDO.setKeepaliveTime(new Date());
        deviceDO.setRegisterTime(new Date());
        deviceDO.setServerIp(SystemInfoUtil.getIpv4());
        deviceService.save(deviceDO);
        Long id = deviceDO.getId();
        Assert.notNull(id, "id is null");

        DeviceDO byId = deviceService.getById(id);
        Assert.notNull(byId, "byId is null");

        deviceService.removeById(deviceDO);
        DeviceDO deviceDO1 = deviceService.getById(id);
        Assert.isNull(deviceDO1, "byId not null");

    }

}
