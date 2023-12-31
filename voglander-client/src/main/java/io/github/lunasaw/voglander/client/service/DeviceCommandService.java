package io.github.lunasaw.voglander.client.service;

import io.github.lunasaw.voglander.client.domain.qo.DeviceQueryReq;

/**
 * @author luna
 * @date 2023/12/31
 */
public interface DeviceCommandService {

    void queryChannle(DeviceQueryReq deviceQueryReq);

}
