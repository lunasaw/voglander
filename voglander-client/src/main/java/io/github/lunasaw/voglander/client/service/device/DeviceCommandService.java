package io.github.lunasaw.voglander.client.service.device;

import io.github.lunasaw.voglander.client.domain.device.qo.DeviceQueryReq;

/**
 * @author luna
 * @date 2023/12/31
 */
public interface DeviceCommandService {

    /**
     * 通道查询
     *
     * @param deviceQueryReq
     */
    void queryChannel(DeviceQueryReq deviceQueryReq);

    /**
     * 设备查询
     *
     * @param deviceQueryReq
     */
    void queryDevice(DeviceQueryReq deviceQueryReq);

}
