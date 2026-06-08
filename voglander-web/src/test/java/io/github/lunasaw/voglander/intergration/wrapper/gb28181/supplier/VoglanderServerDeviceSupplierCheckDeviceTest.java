package io.github.lunasaw.voglander.intergration.wrapper.gb28181.supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.sip.RequestEvent;
import javax.sip.header.FromHeader;
import javax.sip.header.ToHeader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.nist.javax.sip.address.AddressFactoryImpl;
import gov.nist.javax.sip.header.HeaderFactoryImpl;
import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipServerProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;

/**
 * VoglanderServerDeviceSupplier.checkDevice 单元测试。
 * <p>
 * 设计约束（来自联调结论）：SIP 协议层与上下游始终互通，每条 MESSAGE 都会被分发到本服务端处理器；
 * checkDevice 仅承担「这条消息是否归我处理」的本地判定，不影响协议互通。判定规则两步：
 * <ol>
 * <li>To 头 userId == 本端 serverId —— 确认消息发给本平台；否则放弃（这是 Lab 自环下避免
 * 服务端抢应答客户端消息、触发 "Transaction exists" 的关键）。</li>
 * <li>From 头 userId 对应设备已注册且在线 —— 真正的发送方权限校验（From 才是设备，To 是平台自身）。</li>
 * </ol>
 * <p>
 * 用真实 {@link SIPRequest} + 真实 To/From 头驱动真实 {@code SipUtils} 解析，
 * 避免 mock final 方法 {@code SIPRequest.getHeader} 导致 To/From 串读。
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
class VoglanderServerDeviceSupplierCheckDeviceTest {

    private static final String              SERVER_ID  = "34020000002000000001";
    private static final String              DEVICE_ID  = "34020000001320000001";
    private static final String              HOST       = "127.0.0.1";

    private static final AddressFactoryImpl  ADDR_FAC   = new AddressFactoryImpl();
    private static final HeaderFactoryImpl   HEADER_FAC = new HeaderFactoryImpl();

    @Mock
    private DeviceManager                    deviceManager;
    @Mock
    private VoglanderSipServerProperties     serverProperties;

    @Mock
    private RequestEvent                     requestEvent;

    @InjectMocks
    private VoglanderServerDeviceSupplier    supplier;

    @BeforeEach
    void setUp() {
        // getServerFromDevice() 懒构造平台 FromDevice 所需字段
        lenient().when(serverProperties.getServerId()).thenReturn(SERVER_ID);
        lenient().when(serverProperties.getIp()).thenReturn(HOST);
        lenient().when(serverProperties.getPort()).thenReturn(5060);
        lenient().when(serverProperties.getDomain()).thenReturn(SERVER_ID);
    }

    /** 构造携带真实 From/To 头的真实 SIPRequest，并挂到 requestEvent 上。 */
    private void givenRequest(String fromUserId, String toUserId) throws Exception {
        SIPRequest request = new SIPRequest();
        ToHeader to = HEADER_FAC.createToHeader(
            ADDR_FAC.createAddress(ADDR_FAC.createSipURI(toUserId, HOST)), null);
        FromHeader from = HEADER_FAC.createFromHeader(
            ADDR_FAC.createAddress(ADDR_FAC.createSipURI(fromUserId, HOST)), "tag-" + fromUserId);
        request.setHeader(to);
        request.setHeader(from);
        when(requestEvent.getRequest()).thenReturn(request);
    }

    private DeviceDTO deviceWithStatus(Integer status) {
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(DEVICE_ID);
        dto.setStatus(status);
        return dto;
    }

    @Test
    @DisplayName("client→server：To=本平台 且 From 设备在线 → 通过")
    void accept_when_addressed_to_platform_and_sender_online() throws Exception {
        givenRequest(DEVICE_ID, SERVER_ID);
        when(deviceManager.getDtoByDeviceId(DEVICE_ID)).thenReturn(deviceWithStatus(1));

        assertThat(supplier.checkDevice(requestEvent)).isTrue();
    }

    @Test
    @DisplayName("server→client：To≠本平台(=客户端ID) → 放弃，且不查库（避免抢应答触发 Transaction exists）")
    void reject_when_not_addressed_to_platform_without_db_lookup() throws Exception {
        // 平台下发给客户端的 MESSAGE：To 是客户端 ID，From 是平台自身
        givenRequest(SERVER_ID, DEVICE_ID);

        assertThat(supplier.checkDevice(requestEvent)).isFalse();

        // 关键：未命中本平台时绝不触碰设备库，更不会进入业务应答路径
        verify(deviceManager, never()).getDtoByDeviceId(ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("To=本平台 但 From 设备未注册 → 拒绝")
    void reject_when_sender_not_registered() throws Exception {
        givenRequest(DEVICE_ID, SERVER_ID);
        when(deviceManager.getDtoByDeviceId(DEVICE_ID)).thenReturn(null);

        assertThat(supplier.checkDevice(requestEvent)).isFalse();
    }

    @Test
    @DisplayName("To=本平台 但 From 设备离线(status=0) → 拒绝")
    void reject_when_sender_offline() throws Exception {
        givenRequest(DEVICE_ID, SERVER_ID);
        when(deviceManager.getDtoByDeviceId(DEVICE_ID)).thenReturn(deviceWithStatus(0));

        assertThat(supplier.checkDevice(requestEvent)).isFalse();
    }

    @Test
    @DisplayName("To=本平台 但 From 设备状态为 null → 拒绝（不抛 NPE）")
    void reject_when_sender_status_null() throws Exception {
        givenRequest(DEVICE_ID, SERVER_ID);
        when(deviceManager.getDtoByDeviceId(DEVICE_ID)).thenReturn(deviceWithStatus(null));

        assertThat(supplier.checkDevice(requestEvent)).isFalse();
    }

    @Test
    @DisplayName("解析过程抛异常（request 为 null）→ 兜底返回 false，不外抛")
    void reject_and_swallow_on_exception() {
        when(requestEvent.getRequest()).thenReturn(null);

        assertThat(supplier.checkDevice(requestEvent)).isFalse();
    }
}
