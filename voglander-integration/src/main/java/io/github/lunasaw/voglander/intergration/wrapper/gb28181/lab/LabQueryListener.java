package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import java.util.HashMap;
import java.util.Map;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceStatus;
import io.github.lunasaw.gbproxy.client.api.QueryListener;
import io.github.lunasaw.voglander.common.event.SseRelayEvent;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Lab 查询监听器（QueryListener 全局唯一）。
 * <p>
 * {@code voglander.protocol-lab.enabled=true} 时激活，同时 {@code CascadeQueryHandler}
 * 被 {@code @ConditionalOnExpression} 排除，确保运行时只有一个 QueryListener Bean。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabQueryListener implements QueryListener {

    private final ApplicationEventPublisher       eventPublisher;
    private final VoglanderSipClientProperties    clientProps;
    private final LabChannelHolder                labChannelHolder;

    @Override
    public DeviceResponse onCatalogQuery(String platformId, DeviceQuery query) {
        publish("clientcmd.query.catalog", platformId, query.getSn());
        // 回包：按 LabChannelHolder 配置生成模拟通道目录
        LabChannelHolder.Config cfg = labChannelHolder.current();
        int count = cfg.getCount();
        DeviceResponse resp = new DeviceResponse(CmdTypeEnum.CATALOG.getType(), query.getSn(), clientProps.getClientId());
        resp.setSumNum(count);
        java.util.List<io.github.lunasaw.gb28181.common.entity.response.DeviceItem> items = new java.util.ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            io.github.lunasaw.gb28181.common.entity.response.DeviceItem it =
                new io.github.lunasaw.gb28181.common.entity.response.DeviceItem();
            it.setDeviceId(clientProps.getClientId() + String.format("%02d", i));
            it.setName(cfg.getNamePrefix() + i);
            it.setStatus("ON");
            it.setParental(0);
            it.setRegisterWay(1);
            it.setSafetyWay(0);
            items.add(it);
        }
        resp.setDeviceItemList(items);
        return resp;
    }

    @Override
    public DeviceInfo onDeviceInfoQuery(String platformId, DeviceQuery query) {
        publish("clientcmd.query.deviceinfo", platformId, query.getSn());
        DeviceInfo info = new DeviceInfo(CmdTypeEnum.DEVICE_INFO.getType(), query.getSn(), clientProps.getClientId());
        info.setManufacturer("Voglander");
        info.setModel("LabDevice");
        info.setFirmware("v1.0.6");
        return info;
    }

    @Override
    public DeviceStatus onDeviceStatusQuery(String platformId, DeviceQuery query) {
        publish("clientcmd.query.devicestatus", platformId, query.getSn());
        DeviceStatus status = new DeviceStatus(CmdTypeEnum.DEVICE_STATUS.getType(), query.getSn(), clientProps.getClientId());
        status.setOnline("ONLINE");
        return status;
    }

    private void publish(String topic, String platformId, String sn) {
        Map<String, Object> d = new HashMap<>();
        d.put("platformId", platformId != null ? platformId : "");
        d.put("sn", sn != null ? sn : "");
        d.put("ts", System.currentTimeMillis());
        log.debug("Lab 收到查询, topic={}", topic);
        eventPublisher.publishEvent(new SseRelayEvent(topic, d));
    }
}
