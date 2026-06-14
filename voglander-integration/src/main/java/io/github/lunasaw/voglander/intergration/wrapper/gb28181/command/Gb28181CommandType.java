package io.github.lunasaw.voglander.intergration.wrapper.gb28181.command;

import io.github.lunasaw.voglander.common.constant.protocol.ProtocolConstants;

/**
 * GB28181 出站命令 type 枚举（PROTOCOL-S2：单一事实源）。
 * <p>
 * 收敛改造前散落在 6 个 {@code VoglanderServer*Command} 类中的 19 个 {@code private static final String TYPE_*}
 * 三段式字面值。每个 type 是 voglander 传给框架 {@code CommandHandlerRegistry.require(type)} 的<strong>注册键</strong>
 * （框架 {@code Gb28181CommandSpecs}/{@code Gb28181WhitelistHandlers} 侧以私有字面值注册，不暴露 public 常量），
 * 故枚举值须与框架注册键逐字一致——由 {@code Gb28181CommandTypeTest} 冻结基线守护，误改即编译期/测试期暴露。
 * </p>
 * <p>
 * 三段式结构：{@code protocol.group.name}，协议前缀段统一引用 {@link ProtocolConstants#GB28181}。
 * </p>
 *
 * @author luna
 */
public enum Gb28181CommandType {

    // ==================== Query 查询类 ====================
    QUERY_DEVICE_INFO("Query.DeviceInfo"),
    QUERY_DEVICE_STATUS("Query.DeviceStatus"),
    QUERY_CATALOG("Query.Catalog"),
    QUERY_PRESET("Query.PresetQuery"),
    QUERY_MOBILE_POSITION("Query.MobilePosition"),
    QUERY_ALARM("Query.AlarmQuery"),
    QUERY_RECORD_INFO("Query.RecordInfo"),

    // ==================== Control 控制类 ====================
    CONTROL_PTZ("Control.Ptz"),
    CONTROL_REBOOT("Control.Reboot"),
    CONTROL_RECORD("Control.Record"),
    CONTROL_ALARM_RESET("Control.AlarmReset"),

    // ==================== Invite 媒体会话类 ====================
    INVITE_PLAY("Invite.Play"),
    INVITE_PLAYBACK("Invite.Playback"),
    INVITE_PLAYBACK_CONTROL("Invite.PlaybackControl"),
    INVITE_ACK("Invite.Ack"),
    INVITE_BYE("Invite.Bye"),

    // ==================== Config 配置类 ====================
    CONFIG_BASIC_PARAM("Config.BasicParam"),
    CONFIG_DOWNLOAD("Config.ConfigDownload"),

    // ==================== Device 设备类 ====================
    DEVICE_BROADCAST("Device.Broadcast"),

    // ==================== Subscribe 订阅类（GB28181-2022 §9.11） ====================
    SUBSCRIBE_CATALOG("Subscribe.Catalog"),
    SUBSCRIBE_MOBILE_POSITION("Subscribe.MobilePosition"),
    SUBSCRIBE_ALARM("Subscribe.Alarm"),
    SUBSCRIBE_REFRESH("Subscribe.Refresh"),
    SUBSCRIBE_UNSUBSCRIBE("Subscribe.Unsubscribe"),

    ;

    /**
     * 完整三段式 type 字符串（含协议前缀），即下发时传给框架 registry 的注册键。
     */
    private final String type;

    Gb28181CommandType(String groupName) {
        this.type = ProtocolConstants.GB28181 + "." + groupName;
    }

    /**
     * @return 完整三段式 type，如 {@code gb28181.Control.Ptz}
     */
    public String type() {
        return type;
    }
}
