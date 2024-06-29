package io.github.lunasaw.voglander.service.command;

import com.luna.common.check.AssertUtil;
import io.github.lunasaw.sip.common.utils.SpringBeanFactory;
import io.github.lunasaw.voglander.client.service.device.DeviceCommandService;
import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * @author luna
 * @date 2023/12/31
 */
@Service
public class DeviceAgreementService {

    public DeviceCommandService getCommandService(Integer type) {
        Assert.notNull(type, "协议类型不能为空");


        DeviceCommandService deviceCommandService = null;
        if (type.equals(DeviceAgreementEnum.GB28181.getType())) {
            deviceCommandService = SpringBeanFactory.getBean(DeviceConstant.DeviceCommandService.DEVICE_AGREEMENT_SERVICE_NAME_GB28181);
        }

        AssertUtil.notNull(deviceCommandService, ServiceException.PARAMETER_ERROR, "该协议没有对应的实现方法");

        return deviceCommandService;
    }

}
