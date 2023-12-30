package io.github.lunasaw.voglander.client.service;

import io.github.lunasaw.voglander.client.domain.qo.DeviceReq;

/**
 * @author luna
 * @date 2023/12/29
 */
public interface LoginService {

    void login(DeviceReq device);

    void keepalive(String deviceId);

    void updateRemoteAddress(String deviceId, String ip, Integer port);

    void offline(String userId);
}
