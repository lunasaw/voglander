package io.github.lunasaw.voglander.client.service.device;

import com.luna.common.dto.ResultDTO;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceAlarmQueryReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceConfigReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceRecordQueryReq;

/**
 * GB28181 专属命令服务（承载国标特有动作）。
 *
 * <p>
 * 架构约束（ARCHITECTURE.md 1.0.7 §8.1 出站 SPI 对称化）：
 * </p>
 * <ul>
 * <li>{@link DeviceCommandService} 是<b>协议无关 SPI</b>，由 {@code DeviceAgreementService} 按
 * {@code supportProtocols()} 表驱动路由。预置位/SD卡/报警复位/广播/配置下载等是 GB28181 特有动作，
 * <b>无多协议语义</b>，若塞入协议无关 SPI 会强迫未来 ONVIF/RTSP 实现一堆空桩——违反 ISP。</li>
 * <li>故本子接口承载 GB 专属动作；{@code GbDeviceCommandService} 实现本接口即同时是
 * {@link DeviceCommandService} 的 GB 实现，{@code DeviceAgreementService} 仍能正常路由，零影响。</li>
 * <li>Web 控制器对 GB 专属端点<b>直接注入本子接口</b>（按类型或 bean 名），不经 DeviceAgreementService 多协议路由。</li>
 * </ul>
 *
 * <p>
 * 所有方法仅做参数翻译 + 委托底层 6 个 VoglanderServerXxxCommand（命令 bean 内部走
 * dispatchEnvelope + Gb28181CommandType 枚举），返回 {@code ResultDTO<Void>}（取错误用 getMessage）。
 * </p>
 *
 * @author luna
 * @date 2026/06/13
 */
public interface Gb28181DeviceCommandService extends DeviceCommandService {

    /**
     * 查询设备状态（Query.DeviceStatus）
     *
     * @param deviceId 设备国标 ID
     * @return 执行结果
     */
    ResultDTO<Void> queryDeviceStatus(String deviceId);

    /**
     * 查询设备预置位（Query.PresetQuery）
     *
     * @param deviceId 设备国标 ID
     * @return 执行结果
     */
    ResultDTO<Void> queryPreset(String deviceId);

    /**
     * 查询移动位置订阅（Query.MobilePosition）
     *
     * @param deviceId 设备国标 ID
     * @param interval 上报间隔（秒），可空走底层默认
     * @return 执行结果
     */
    ResultDTO<Void> queryMobilePosition(String deviceId, String interval);

    /**
     * 下载设备配置（Config.ConfigDownload）
     *
     * @param deviceId   设备国标 ID
     * @param configType 配置类型 BASIC/VIDEO/AUDIO
     * @return 执行结果
     */
    ResultDTO<Void> downloadConfig(String deviceId, String configType);

    /**
     * 下发设备配置（Config.BasicParam）
     *
     * @param req 配置请求
     * @return 执行结果
     */
    ResultDTO<Void> setDeviceConfig(DeviceConfigReq req);

    /**
     * 录像控制（Control.Record，start/stop）
     *
     * @param deviceId 设备国标 ID
     * @param start    true=开始 / false=停止
     * @return 执行结果
     */
    ResultDTO<Void> controlRecord(String deviceId, boolean start);

    /**
     * 录像信息查询（Query.RecordInfo，结果由入站 Response.RecordInfo 缓存）
     *
     * @param req 录像查询请求
     * @return 执行结果
     */
    ResultDTO<Void> queryRecord(DeviceRecordQueryReq req);

    /**
     * 报警查询（Query.AlarmQuery）
     *
     * @param req 报警查询请求
     * @return 执行结果
     */
    ResultDTO<Void> queryAlarm(DeviceAlarmQueryReq req);

    /**
     * 报警控制/复位（Control.AlarmReset）
     *
     * @param deviceId    设备国标 ID
     * @param alarmMethod 报警方式
     * @param alarmType   报警类型
     * @return 执行结果
     */
    ResultDTO<Void> controlAlarm(String deviceId, String alarmMethod, String alarmType);

    /**
     * 语音广播（Device.Broadcast）
     *
     * @param deviceId 设备国标 ID
     * @return 执行结果
     */
    ResultDTO<Void> broadcast(String deviceId);
}
