package io.github.lunasaw.voglander.client.service;

import io.github.lunasaw.voglander.client.domain.qo.DeviceQueryReq;

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
