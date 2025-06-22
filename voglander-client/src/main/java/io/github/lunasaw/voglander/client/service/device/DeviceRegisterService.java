package io.github.lunasaw.voglander.client.service.device;

import com.luna.common.dto.ResultDTO;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceChannelReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceInfoReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceRegisterReq;

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
    ResultDTO<Void> login(DeviceRegisterReq device);

    /**
     * 保持活跃
     * @param deviceId
     */
    ResultDTO<Boolean> keepalive(String deviceId);

    /**
     * 更新设备地址
     * @param deviceId
     * @param ip
     * @param port
     */
    ResultDTO<Long> updateRemoteAddress(String deviceId, String ip, Integer port);

    /**
     * 设备离线
     * @param userId
     */
    ResultDTO<Void> offline(String userId);

    /**
     * 添加设备通道
     *
     * @param req
     */
    ResultDTO<Long> addChannel(DeviceChannelReq req);

    /**
     * 更新设备信息
     *
     * @param req
     */
    ResultDTO<Void> updateDeviceInfo(DeviceInfoReq req);
}
