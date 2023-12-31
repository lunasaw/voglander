package io.github.lunasaw.voglander.client.service;

import io.github.lunasaw.voglander.client.domain.qo.DeviceChannelReq;
import io.github.lunasaw.voglander.client.domain.qo.DeviceReq;

/**
 * @author luna
 * @date 2023/12/29
 */
public interface DeviceRegisterService {

    /**
     * 注册登陆
     *
     * @param device
     */
    void login(DeviceReq device);

    /**
     * 保持活跃
     * @param deviceId
     */
    void keepalive(String deviceId);

    /**
     * 更新设备地址
     * @param deviceId
     * @param ip
     * @param port
     */
    void updateRemoteAddress(String deviceId, String ip, Integer port);

    /**
     * 设备离线
     * @param userId
     */
    void offline(String userId);

    /**
     * 添加设备通道
     *
     * @param req
     */
    void addChannel(DeviceChannelReq req);
}
