package io.github.lunasaw.voglander.common.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * S4（PROTOCOL-ARCHITECTURE-GENERICITY）协议枚举折算事实源测试。
 * <p>
 * 路由维度统一为纯协议 {@link DeviceProtocolEnum} 后，{@link DeviceAgreementEnum}（协议×型态复合）
 * 须能正确折算到纯协议——GB28181_IPC / GB28181_NVR 都映射到 GB28181，ONVIF_IPC 映射到 ONVIF。
 * 这是调用方 agreement→protocol 折算的正确性依据。
 * </p>
 *
 * @author luna
 */
@DisplayName("S4 — DeviceAgreementEnum 协议折算")
class DeviceAgreementEnumTest {

    @Test
    @DisplayName("GB28181_IPC 折算到 GB28181 纯协议")
    void gb28181Ipc_mapsToGb28181() {
        assertEquals(DeviceProtocolEnum.GB28181.getType(), DeviceAgreementEnum.GB28181_IPC.getProtocol());
    }

    @Test
    @DisplayName("GB28181_NVR 折算到 GB28181 纯协议（与 IPC 同协议）")
    void gb28181Nvr_mapsToGb28181() {
        assertEquals(DeviceProtocolEnum.GB28181.getType(), DeviceAgreementEnum.GB28181_NVR.getProtocol());
        assertEquals(DeviceAgreementEnum.GB28181_IPC.getProtocol(), DeviceAgreementEnum.GB28181_NVR.getProtocol());
    }

    @Test
    @DisplayName("ONVIF_IPC 折算到 ONVIF 纯协议")
    void onvifIpc_mapsToOnvif() {
        assertEquals(DeviceProtocolEnum.ONVIF.getType(), DeviceAgreementEnum.ONVIF_IPC.getProtocol());
    }

    @Test
    @DisplayName("getByType 已知/未知取值")
    void getByType_knownAndUnknown() {
        assertEquals(DeviceAgreementEnum.GB28181_IPC, DeviceAgreementEnum.getByType(1));
        assertNull(DeviceAgreementEnum.getByType(999));
    }
}
