package io.github.lunasaw.voglander.service.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.luna.common.check.AssertUtil;

import io.github.lunasaw.voglander.client.service.device.DeviceCommandService;
import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;

/**
 * 出站命令服务路由（PROTOCOL-S1：SPI 表驱动，对称入站 {@code InboundEventDispatcher}）。
 * <p>
 * 构造期注入所有 {@link DeviceCommandService} 实现，按各自声明的 {@code supportProtocols()}
 * 折叠成「纯协议 type → 命令服务」路由表。新增协议只需新增一个 {@code @Service} 实现并声明
 * 支持协议，本类<strong>零改动</strong>，彻底消除原硬编码 {@code if (协议 ==)} 分支。
 * </p>
 * <p>
 * 路由键为<strong>纯协议</strong>（{@code DeviceProtocolEnum}）。对外入参仍为
 * {@code DeviceAgreementEnum} 的 type（协议×型态），内部经 {@link DeviceAgreementEnum#getProtocol()}
 * 折算到纯协议维度——GB28181_IPC / GB28181_NVR 都路由到同一 GB28181 服务，调用方签名不变。
 * </p>
 *
 * @author luna
 * @date 2023/12/31
 */
@Slf4j
@Service
public class DeviceAgreementService {

    /**
     * 纯协议 type（{@code DeviceProtocolEnum.getType()}）→ 命令服务实现。
     */
    private final Map<Integer, DeviceCommandService> routing;

    public DeviceAgreementService(List<DeviceCommandService> services) {
        Map<Integer, DeviceCommandService> map = new HashMap<>();
        for (DeviceCommandService svc : services) {
            for (Integer protocol : svc.supportProtocols()) {
                DeviceCommandService prev = map.put(protocol, svc);
                if (prev != null) {
                    throw new IllegalStateException("协议 " + protocol + " 存在多个 DeviceCommandService 实现: "
                        + prev.getClass().getName() + " / " + svc.getClass().getName());
                }
            }
        }
        this.routing = Collections.unmodifiableMap(map);
        log.info("DeviceAgreementService 就绪，已注册协议: {}", routing.keySet());
    }

    /**
     * 按设备协议（{@link DeviceAgreementEnum} 的 type）路由到对应命令服务。
     *
     * @param type {@code DeviceAgreementEnum.getType()}（协议×型态复合值）
     * @return 对应纯协议的命令服务实现
     * @throws ServiceException 入参为空、未知 agreement、或无对应协议实现
     */
    public DeviceCommandService getCommandService(Integer type) {
        Assert.notNull(type, "协议类型不能为空");

        DeviceAgreementEnum agreement = DeviceAgreementEnum.getByType(type);
        AssertUtil.notNull(agreement, ServiceException.PARAMETER_ERROR, "未知设备协议类型: " + type);

        DeviceCommandService svc = routing.get(agreement.getProtocol());
        AssertUtil.notNull(svc, ServiceException.PARAMETER_ERROR, "该协议没有对应的实现方法");
        return svc;
    }
}
