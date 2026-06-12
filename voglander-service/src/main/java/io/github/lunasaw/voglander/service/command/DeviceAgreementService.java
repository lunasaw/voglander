package io.github.lunasaw.voglander.service.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.luna.common.check.AssertUtil;

import io.github.lunasaw.voglander.client.service.device.DeviceCommandService;
import io.github.lunasaw.voglander.common.enums.DeviceProtocolEnum;
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
 * PROTOCOL-S4：路由键与对外入参均为<strong>纯协议</strong>（{@link DeviceProtocolEnum} 的 type）。
 * 「协议×型态」复合枚举 {@code DeviceAgreementEnum} → 纯协议的折算由调用方完成
 * （{@code DeviceAgreementEnum.getProtocol()}），本服务职责单一、只认纯协议维度。
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
     * 按纯协议（{@link DeviceProtocolEnum} 的 type）路由到对应命令服务。
     *
     * @param protocolType {@code DeviceProtocolEnum.getType()}（如 GB28181=1 / ONVIF=2）；
     *                     调用方若持有 {@code DeviceAgreementEnum} 须先经 {@code getProtocol()} 折算
     * @return 对应协议的命令服务实现
     * @throws ServiceException 入参为空、或无对应协议实现
     */
    public DeviceCommandService getCommandService(Integer protocolType) {
        Assert.notNull(protocolType, "协议类型不能为空");

        DeviceCommandService svc = routing.get(protocolType);
        AssertUtil.notNull(svc, ServiceException.PARAMETER_ERROR, "该协议没有对应的实现方法");
        return svc;
    }
}
