package io.github.lunasaw.voglander.repository.service;

import com.alibaba.fastjson.JSON;
import com.luna.common.check.Assert;
import com.luna.common.dto.ResultDTO;
import com.luna.common.os.SystemInfoUtil;
import com.luna.common.text.RandomStrUtil;
import io.github.lunasaw.voglander.client.service.device.DeviceRegisterService;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
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
        deviceDO.setDeviceId(RandomStrUtil.generateNonceStr());
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

    @Autowired
    private DeviceRegisterService deviceRegisterService;

    @Test
    public void ctest() {
        ResultDTO<Boolean> keepalive = deviceRegisterService.keepalive("");
        System.out.println(JSON.toJSONString(keepalive));
    }
}
