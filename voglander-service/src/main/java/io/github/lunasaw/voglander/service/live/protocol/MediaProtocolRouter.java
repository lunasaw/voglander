package io.github.lunasaw.voglander.service.live.protocol;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import lombok.extern.slf4j.Slf4j;

/**
 * 媒体协议处理器路由（PROTOCOL-S5：SPI 表驱动，对称出站命令 {@code DeviceAgreementService}）。
 * <p>
 * 构造期注入所有 {@link MediaProtocolHandler}，按各自 {@code supportProtocols()} 折叠成
 * 「纯协议 type → handler」路由表。编排层（{@code MediaPlayServiceImpl}）按设备协议取 handler；
 * 新增协议只需新增一个 handler 实现，本类与编排层<strong>零改动</strong>。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
public class MediaProtocolRouter {

    /**
     * 纯协议 type（{@code DeviceProtocolEnum.getType()}）→ 媒体协议处理器。
     */
    private final Map<Integer, MediaProtocolHandler> routing;

    private final DeviceManager                      deviceManager;

    @Autowired
    public MediaProtocolRouter(List<MediaProtocolHandler> handlers, DeviceManager deviceManager) {
        Map<Integer, MediaProtocolHandler> map = new HashMap<>();
        for (MediaProtocolHandler h : handlers) {
            for (Integer protocol : h.supportProtocols()) {
                MediaProtocolHandler prev = map.put(protocol, h);
                if (prev != null) {
                    throw new IllegalStateException("协议 " + protocol + " 存在多个 MediaProtocolHandler 实现: "
                        + prev.getClass().getName() + " / " + h.getClass().getName());
                }
            }
        }
        this.routing = Collections.unmodifiableMap(map);
        this.deviceManager = deviceManager;
        log.info("MediaProtocolRouter 就绪，已注册媒体协议: {}", routing.keySet());
    }

    /**
     * 按纯协议 type 取 handler。
     *
     * @param protocolType {@code DeviceProtocolEnum.getType()}
     * @return handler，未注册返回 null
     */
    public MediaProtocolHandler getHandler(Integer protocolType) {
        return protocolType == null ? null : routing.get(protocolType);
    }

    /**
     * 按 deviceId 解析设备协议并取对应 handler。
     * <p>
     * 查设备 → {@code DeviceAgreementEnum.getProtocol()} 折算纯协议 → 路由。
     * 设备不存在 / 协议未知 / 无对应 handler 时返回 null（调用方决定 fail-fast 还是 best-effort）。
     * </p>
     *
     * @param deviceId 设备 ID
     * @return handler 或 null
     */
    public MediaProtocolHandler resolveForDevice(String deviceId) {
        if (deviceId == null) {
            return null;
        }
        DeviceDTO device;
        try {
            device = deviceManager.getDtoByDeviceId(deviceId);
        } catch (Exception e) {
            log.warn("[MediaProtocolRouter] 查设备失败, deviceId={}: {}", deviceId, e.getMessage());
            return null;
        }
        if (device == null || device.getType() == null) {
            return null;
        }
        DeviceAgreementEnum agreement = DeviceAgreementEnum.getByType(device.getType());
        if (agreement == null) {
            return null;
        }
        return routing.get(agreement.getProtocol());
    }
}
