package io.github.lunasaw.voglander.intergration.wrapper.gb28181.supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import javax.sip.RequestEvent;
import javax.sip.header.ToHeader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import gov.nist.javax.sip.address.AddressFactoryImpl;
import gov.nist.javax.sip.header.HeaderFactoryImpl;
import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipServerProperties;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab.LabChannelHolder;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;

/**
 * VoglanderClientDeviceSupplier.checkDevice 单元测试。
 * <p>
 * 设备端 UA 判定入站请求是否归己处理：
 * <ol>
 * <li>To 头 userId == 本端 clientId —— 寻址到设备本身（MESSAGE/查询等）。</li>
 * <li>To 头 userId 是本设备拥有的通道编码 —— GB28181 点播寻址到通道（INVITE To=channelId）。
 * 通道归属委托 {@link LabChannelHolder#ownsChannel}，与目录回包同一编码规则单点维护。</li>
 * </ol>
 * <p>
 * <b>重要</b>：{@code clientId + 两位序号} 是 Lab 测试台简化约定，<b>非 GB28181 标准</b>
 * （标准通道为独立 20 位编码，从属靠 Catalog ParentID 表达）。故 lab 关闭（holder 不存在）时
 * 仅认 clientId，绝不放行任何通道寻址——真实设备接入由各自 supplier 按其真实通道清单实现。
 * <p>
 * 用真实 {@link SIPRequest} + 真实 To 头驱动真实 {@code SipUtils} 解析，避免 mock final 方法串读。
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
class VoglanderClientDeviceSupplierCheckDeviceTest {

    /**
     * 设备编码以 "00" 结尾：Lab 通道编码规则为 {@code clientId.substring(0,18) + 两位序号}，
     * 取 "...0000" 可使设备本身（"...0000"）与通道 1~N（"...0001".."...000N"）严格互不重叠，
     * 避免设备ID恰好等于某通道编码造成的语义歧义。
     */
    private static final String              CLIENT_ID  = "34020000001320000000";
    private static final String              HOST       = "127.0.0.1";

    private static final AddressFactoryImpl  ADDR_FAC   = new AddressFactoryImpl();
    private static final HeaderFactoryImpl   HEADER_FAC = new HeaderFactoryImpl();

    @Mock
    private DeviceManager                    deviceManager;
    @Mock
    private VoglanderSipClientProperties     clientProperties;
    @Mock
    private VoglanderSipServerProperties     serverProperties;

    @Mock
    private RequestEvent                     requestEvent;

    @InjectMocks
    private VoglanderClientDeviceSupplier    supplier;

    @BeforeEach
    void setUp() {
        // getClientFromDevice() 懒构造客户端 FromDevice 所需字段
        lenient().when(clientProperties.getClientId()).thenReturn(CLIENT_ID);
        lenient().when(clientProperties.getDomain()).thenReturn(HOST);
        lenient().when(clientProperties.getPort()).thenReturn(5061);
        lenient().when(clientProperties.getRealm()).thenReturn(CLIENT_ID.substring(0, 8));
    }

    /** 构造携带真实 To 头的真实 SIPRequest，并挂到 requestEvent 上。 */
    private void givenToHeader(String toUserId) throws Exception {
        SIPRequest request = new SIPRequest();
        ToHeader to = HEADER_FAC.createToHeader(
            ADDR_FAC.createAddress(ADDR_FAC.createSipURI(toUserId, HOST)), null);
        request.setHeader(to);
        when(requestEvent.getRequest()).thenReturn(request);
    }

    /**
     * 按 Lab 通道编码规则生成第 n 个通道编码（{@code clientId.substring(0,18) + 两位序号}），
     * 与 {@link LabChannelHolder#channelIdOf} 同源，避免测试自造与实现不一致的编码。
     */
    private static String channelId(int n) {
        return CLIENT_ID.substring(0, 18) + String.format("%02d", n);
    }

    /** 注入真实 LabChannelHolder（指定通道数量），模拟 lab 开启。 */
    @SuppressWarnings("unchecked")
    private void injectChannelHolder(int count) {
        LabChannelHolder holder = new LabChannelHolder();
        holder.update(count, null);
        ObjectProvider<LabChannelHolder> provider = org.mockito.Mockito.mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(holder);
        ReflectionTestUtils.setField(supplier, "labChannelHolderProvider", provider);
    }

    // ── 寻址到设备本身 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("To = clientId → 通过（寻址到设备本身，无需通道判定）")
    void accept_when_addressed_to_device_itself() throws Exception {
        givenToHeader(CLIENT_ID);
        // 不注入 holder 也应通过
        assertThat(supplier.checkDevice(requestEvent)).isTrue();
    }

    // ── 寻址到通道（GB28181 点播） ────────────────────────────────────────

    @Test
    @DisplayName("To = 本设备通道(第1通道)，lab 开启且在通道范围内 → 通过")
    void accept_when_addressed_to_own_channel() throws Exception {
        injectChannelHolder(4);
        givenToHeader(channelId(1));
        assertThat(supplier.checkDevice(requestEvent)).isTrue();
    }

    @Test
    @DisplayName("To = 本设备末位通道(第4通道)，count=4 边界 → 通过")
    void accept_when_addressed_to_last_channel() throws Exception {
        injectChannelHolder(4);
        givenToHeader(channelId(4));
        assertThat(supplier.checkDevice(requestEvent)).isTrue();
    }

    @Test
    @DisplayName("To = 超出通道数量(第5通道)，count=4 → 拒绝")
    void reject_when_channel_index_out_of_range() throws Exception {
        injectChannelHolder(4);
        givenToHeader(channelId(5));
        assertThat(supplier.checkDevice(requestEvent)).isFalse();
    }

    @Test
    @DisplayName("To = clientId+1234（4 位后缀，非两位序号格式）→ 拒绝（不前缀串扰）")
    void reject_when_suffix_not_two_digit() throws Exception {
        injectChannelHolder(99);
        givenToHeader(CLIENT_ID + "1234");
        assertThat(supplier.checkDevice(requestEvent)).isFalse();
    }

    @Test
    @DisplayName("To 与 clientId 仅前缀相同的他设备编码 → 拒绝（精确比对，非 startsWith）")
    void reject_when_only_prefix_matches() throws Exception {
        injectChannelHolder(4);
        // 前 8 位相同但整体不同的另一设备
        givenToHeader("34020000009990000009");
        assertThat(supplier.checkDevice(requestEvent)).isFalse();
    }

    // ── lab 关闭：仅认 clientId，不放行任何通道寻址 ─────────────────────────

    @Test
    @DisplayName("lab 关闭（holder 不存在）+ To=通道编码 → 拒绝（不走 Lab 兜底）")
    void reject_channel_when_lab_disabled() throws Exception {
        // 不注入 labChannelHolderProvider（保持 null）
        givenToHeader(CLIENT_ID + "01");
        assertThat(supplier.checkDevice(requestEvent)).isFalse();
    }

    @Test
    @DisplayName("lab 关闭 + To=clientId 本身 → 仍通过")
    void accept_device_itself_when_lab_disabled() throws Exception {
        givenToHeader(CLIENT_ID);
        assertThat(supplier.checkDevice(requestEvent)).isTrue();
    }

    // ── 兜底 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("解析过程抛异常（request 为 null）→ 兜底返回 false，不外抛")
    void reject_and_swallow_on_exception() {
        when(requestEvent.getRequest()).thenReturn(null);
        assertThat(supplier.checkDevice(requestEvent)).isFalse();
    }
}
