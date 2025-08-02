package io.github.lunasaw.voglander.client.service.device;

import io.github.lunasaw.voglander.client.domain.device.qo.DeviceChannelReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceInfoReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceRegisterReq;
import io.github.lunasaw.voglander.common.domain.AjaxResult;

/**
 * 设备注册服务接口
 * 处理设备注册、登录、心跳等核心业务
 *
 * @author luna
 * @date 2023/12/29
 */
public interface DeviceRegisterService {

    /**
     * 设备注册登录
     *
     * @param device 设备注册请求信息
     * @return 操作结果
     */
    AjaxResult<Void> login(DeviceRegisterReq device);

    /**
     * 设备保持活跃（心跳）
     *
     * @param deviceId 设备ID
     * @return 心跳处理结果，包含是否成功的标识
     */
    AjaxResult<Boolean> keepalive(String deviceId);

    /**
     * 更新设备远程地址
     *
     * @param deviceId 设备ID
     * @param ip IP地址
     * @param port 端口
     * @return 更新结果，包含数据库记录ID
     */
    AjaxResult<Long> updateRemoteAddress(String deviceId, String ip, Integer port);

    /**
     * 设备下线
     *
     * @param deviceId 设备ID（注意：参数名已从userId修正为deviceId）
     * @return 操作结果
     */
    AjaxResult<Void> offline(String deviceId);

    /**
     * 添加设备通道
     *
     * @param req 设备通道请求信息
     * @return 添加结果，包含通道数据库ID
     */
    AjaxResult<Long> addChannel(DeviceChannelReq req);

    /**
     * 更新设备信息
     *
     * @param req 设备信息更新请求
     * @return 操作结果
     */
    AjaxResult<Void> updateDeviceInfo(DeviceInfoReq req);
}
