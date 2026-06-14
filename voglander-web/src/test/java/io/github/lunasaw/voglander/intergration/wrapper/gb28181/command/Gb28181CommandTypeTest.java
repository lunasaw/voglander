package io.github.lunasaw.voglander.intergration.wrapper.gb28181.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.common.constant.protocol.ProtocolConstants;

/**
 * S2（PROTOCOL-ARCHITECTURE-GENERICITY）出站命令 type 枚举冻结测试。
 * <p>
 * {@link Gb28181CommandType} 的每个 type 字符串是 voglander 传给框架
 * {@code CommandHandlerRegistry.require(type)} 的<strong>注册键</strong>，框架侧把它们作为私有字面值注册、
 * 不暴露 public 常量。一旦枚举值与框架注册键漂移一字，运行期即 404 静默丢命令。
 * </p>
 * <p>
 * 本测试逐字 pin 死 19 个出站 type（与改造前 6 个 command 类的私有常量完全一致），
 * 任何对枚举值的误改都会在此立即失败，迫使做出有意识的决定。
 * </p>
 *
 * @author luna
 */
@DisplayName("S2 — 出站命令 type 枚���冻结")
class Gb28181CommandTypeTest {

    /**
     * 改造前散落在 6 个 command 类的 19 个出站 type 字面值（单一事实源冻结基线）。
     */
    private static final Map<Gb28181CommandType, String> FROZEN = Map.ofEntries(
        Map.entry(Gb28181CommandType.QUERY_DEVICE_INFO, "gb28181.Query.DeviceInfo"),
        Map.entry(Gb28181CommandType.QUERY_DEVICE_STATUS, "gb28181.Query.DeviceStatus"),
        Map.entry(Gb28181CommandType.QUERY_CATALOG, "gb28181.Query.Catalog"),
        Map.entry(Gb28181CommandType.QUERY_PRESET, "gb28181.Query.PresetQuery"),
        Map.entry(Gb28181CommandType.QUERY_MOBILE_POSITION, "gb28181.Query.MobilePosition"),
        Map.entry(Gb28181CommandType.QUERY_ALARM, "gb28181.Query.AlarmQuery"),
        Map.entry(Gb28181CommandType.QUERY_RECORD_INFO, "gb28181.Query.RecordInfo"),
        Map.entry(Gb28181CommandType.CONTROL_PTZ, "gb28181.Control.Ptz"),
        Map.entry(Gb28181CommandType.CONTROL_REBOOT, "gb28181.Control.Reboot"),
        Map.entry(Gb28181CommandType.CONTROL_RECORD, "gb28181.Control.Record"),
        Map.entry(Gb28181CommandType.CONTROL_ALARM_RESET, "gb28181.Control.AlarmReset"),
        Map.entry(Gb28181CommandType.INVITE_PLAY, "gb28181.Invite.Play"),
        Map.entry(Gb28181CommandType.INVITE_PLAYBACK, "gb28181.Invite.Playback"),
        Map.entry(Gb28181CommandType.INVITE_PLAYBACK_CONTROL, "gb28181.Invite.PlaybackControl"),
        Map.entry(Gb28181CommandType.INVITE_ACK, "gb28181.Invite.Ack"),
        Map.entry(Gb28181CommandType.INVITE_BYE, "gb28181.Invite.Bye"),
        Map.entry(Gb28181CommandType.CONFIG_BASIC_PARAM, "gb28181.Config.BasicParam"),
        Map.entry(Gb28181CommandType.CONFIG_DOWNLOAD, "gb28181.Config.ConfigDownload"),
        Map.entry(Gb28181CommandType.DEVICE_BROADCAST, "gb28181.Device.Broadcast"),
        Map.entry(Gb28181CommandType.SUBSCRIBE_CATALOG, "gb28181.Subscribe.Catalog"),
        Map.entry(Gb28181CommandType.SUBSCRIBE_MOBILE_POSITION, "gb28181.Subscribe.MobilePosition"),
        Map.entry(Gb28181CommandType.SUBSCRIBE_ALARM, "gb28181.Subscribe.Alarm"),
        Map.entry(Gb28181CommandType.SUBSCRIBE_REFRESH, "gb28181.Subscribe.Refresh"),
        Map.entry(Gb28181CommandType.SUBSCRIBE_UNSUBSCRIBE, "gb28181.Subscribe.Unsubscribe"));

    @Test
    @DisplayName("24 个出站 type 逐字与冻结基线一致")
    void allTypes_matchFrozenBaseline() {
        FROZEN.forEach((enumConst, expected) -> assertEquals(expected, enumConst.type(),
            "枚举 " + enumConst.name() + " 的 type 已漂移，与框架注册键不一致将导致运行期丢命令"));
    }

    @Test
    @DisplayName("枚举数量恰为 24（冻结基线覆盖全部，无遗漏无新增未登记）")
    void enumCount_is24() {
        assertEquals(24, Gb28181CommandType.values().length);
        assertEquals(24, FROZEN.size());
    }

    @Test
    @DisplayName("无重复 type 值")
    void noDuplicateTypes() {
        Set<String> seen = new HashSet<>();
        for (Gb28181CommandType t : Gb28181CommandType.values()) {
            assertTrue(seen.add(t.type()), "重复 type: " + t.type());
        }
    }

    @Test
    @DisplayName("全部 type 以协议前缀 gb28181. 开头")
    void allTypes_haveProtocolPrefix() {
        String prefix = ProtocolConstants.GB28181 + ".";
        Arrays.stream(Gb28181CommandType.values())
            .forEach(t -> assertTrue(t.type().startsWith(prefix),
                t.name() + " 缺少协议前缀: " + t.type()));
    }
}
