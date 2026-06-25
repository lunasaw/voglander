package io.github.lunasaw.voglander.common.event;

/**
 * 本地录像信息响应事件（真实设备 RecordInfo 响应到达）。
 *
 * <p>由 {@code Gb28181ProtocolHandler.handleRecordInfo} 落缓存后发布，
 * 级联事件桥接监听后按 (localDeviceId + 时间窗) 找回上级查询请求，主动回包上级（C6）。
 *
 * <p>携带 DeviceRecord 的 FastJSON 字符串，避免 common 模块依赖 gb28181 实体；
 * 消费方（integration 层）用 FastJSON2 反序列化重建并重映射 deviceId/sn。
 *
 * @author luna
 */
public class LocalRecordInfoEvent {

    private final String deviceId;
    /** DeviceRecord 的 FastJSON2 字符串（含 recordList/sumNum 等） */
    private final String recordJson;

    public LocalRecordInfoEvent(String deviceId, String recordJson) {
        this.deviceId = deviceId;
        this.recordJson = recordJson;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getRecordJson() {
        return recordJson;
    }
}
