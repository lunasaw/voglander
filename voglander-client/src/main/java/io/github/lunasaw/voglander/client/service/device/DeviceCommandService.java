package io.github.lunasaw.voglander.client.service.device;

import com.luna.common.dto.ResultDTO;
import io.github.lunasaw.voglander.client.domain.device.qo.DevicePlayReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DevicePlaybackReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DevicePtzReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceQueryReq;

/**
 * 设备命令服务（协议无关接口）
 * Phase 5-S3: 扩展协议无关方法，由具体协议实现类翻译为底层命令
 *
 * @author luna
 * @date 2023/12/31
 */
public interface DeviceCommandService {

    /**
     * 通道查询（向后兼容）
     *
     * @param deviceQueryReq 查询请求
     * @return 执行结果
     */
    ResultDTO<Void> queryChannel(DeviceQueryReq deviceQueryReq);

    /**
     * 设备查询（向后兼容）
     *
     * @param deviceQueryReq 查询请求
     * @return 执行结果
     */
    ResultDTO<Void> queryDevice(DeviceQueryReq deviceQueryReq);

    /**
     * 查询设备信息（协议无关）
     *
     * @param deviceId 设备ID
     * @return 执行结果
     */
    ResultDTO<Void> queryDeviceInfo(String deviceId);

    /**
     * 查询设备目录��协议无关）
     *
     * @param deviceId 设备ID
     * @return 执行结果
     */
    ResultDTO<Void> queryCatalog(String deviceId);

    /**
     * PTZ 控制（协议无关）
     *
     * @param req PTZ控制请求
     * @return 执行结果
     */
    ResultDTO<Void> ptzControl(DevicePtzReq req);

    /**
     * 启动实时播放（协议无关）
     * 注意：callId 由 InviteOk 事件异步产生并存入 MediaSession，本接口暂返回 null
     *
     * @param req 播放请求
     * @return callId（当前实现返回null，待下一轮改造为同步 future）
     */
    ResultDTO<String> startPlay(DevicePlayReq req);

    /**
     * 启动回放（协议无关）
     * 注意：callId 由 InviteOk 事件异步产生
     *
     * @param req 回放请求
     * @return callId（当前实现返回null）
     */
    ResultDTO<String> startPlayback(DevicePlaybackReq req);

    /**
     * 停止播放/回放（协议无关）
     *
     * @param callId 会话ID
     * @return 执行结果
     */
    ResultDTO<Void> stopPlay(String callId);

    /**
     * 重启设备（协议无关）
     *
     * @param deviceId 设备ID
     * @return 执行结果
     */
    ResultDTO<Void> reboot(String deviceId);

    /**
     * 回放控制（协议无关）。
     * <p>
     * L1：暂停/继续 —— {@code PLAY_RESUME}(暂停)/{@code PLAY_NOW}(继续) 无需 param。
     * 由 {@code streamId} 反查会话得 deviceId 后下发；会话不存在返回失败。
     * {@code PLAY_RANGE}(seek)/{@code PLAY_SPEED}(倍速) 本次显式不支持（envelope payload 当前仅含 action）。
     *
     * @param streamId 前端稳定主键
     * @param action 回放动作（PlayActionEnums 名：PLAY_RESUME/PLAY_NOW/PLAY_RANGE/PLAY_SPEED）
     * @param param seek 位置 / 倍速值（L1 不透传，预留）
     * @return 执行结果
     */
    ResultDTO<Void> controlPlayback(String streamId, String action, String param);
}
