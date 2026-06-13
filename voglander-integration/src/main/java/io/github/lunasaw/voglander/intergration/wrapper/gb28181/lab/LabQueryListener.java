package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import java.util.HashMap;
import java.util.Map;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gb28181.common.entity.query.DeviceAlarmQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceConfigDownload;
import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceRecordQuery;
import io.github.lunasaw.gb28181.common.entity.query.MobilePositionQuery;
import io.github.lunasaw.gb28181.common.entity.query.PresetQuery;
import io.github.lunasaw.gb28181.common.entity.response.DeviceConfigResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceStatus;
import io.github.lunasaw.gb28181.common.entity.response.PresetQueryResponse;
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
            it.setDeviceId(labChannelHolder.channelIdOf(clientProps.getClientId(), i));
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

    /**
     * 录像查询应答：模拟一条最近录像（时间范围回显平台请求的 start/end）。
     */
    @Override
    public DeviceRecord onRecordInfoQuery(String platformId, DeviceRecordQuery query) {
        publish("clientcmd.query.recordinfo", platformId, query.getSn());
        String clientId = clientProps.getClientId();
        DeviceRecord record = new DeviceRecord(CmdTypeEnum.RECORD_INFO.getType(), query.getSn(), clientId);
        DeviceRecord.RecordItem item = new DeviceRecord.RecordItem();
        item.setDeviceId(labChannelHolder.channelIdOf(clientId, 1));
        item.setName("Lab-Record-1");
        item.setStartTime(query.getStartTime());
        item.setEndTime(query.getEndTime());
        item.setType("time");
        item.setSecrecy("0");
        item.setFileSize("1024");
        record.setRecordList(java.util.List.of(item));
        record.setSumNum(1);
        return record;
    }

    /**
     * 配置下载应答：回显 OK（模拟设备始终接受配置查询）。
     */
    @Override
    public DeviceConfigResponse onConfigDownloadQuery(String platformId, DeviceConfigDownload query) {
        publish("clientcmd.query.configdownload", platformId, query.getSn());
        DeviceConfigResponse resp =
            new DeviceConfigResponse(CmdTypeEnum.CONFIG_DOWNLOAD.getType(), query.getSn(), clientProps.getClientId());
        resp.setResult("OK");
        return resp;
    }

    /**
     * 预置位查询应答：模拟两个预置位。
     */
    @Override
    public PresetQueryResponse onPresetQuery(String platformId, PresetQuery query) {
        publish("clientcmd.query.preset", platformId, query.getSn());
        PresetQueryResponse resp = new PresetQueryResponse();
        resp.setDeviceId(clientProps.getClientId());
        resp.setSn(query.getSn());
        java.util.List<PresetQueryResponse.PresetItem> items = java.util.List.of(
            new PresetQueryResponse.PresetItem("1", "Lab-Preset-1"),
            new PresetQueryResponse.PresetItem("2", "Lab-Preset-2"));
        resp.setPresetList(new PresetQueryResponse.PresetList(items.size(), items));
        return resp;
    }

    /**
     * 移动位置查询应答：模拟一个固定 GPS 坐标点。
     */
    @Override
    public MobilePositionNotify onMobilePositionQuery(String platformId, MobilePositionQuery query) {
        publish("clientcmd.query.mobileposition", platformId, query.getSn());
        // 回包必须带 CmdType（Notify 缺 CmdType 会被平台 doMessageHandForEvt 丢弃）+ 回显 SN
        MobilePositionNotify notify = new MobilePositionNotify(
            CmdTypeEnum.MOBILE_POSITION.getType(), query.getSn(), clientProps.getClientId());
        // GB28181 时间格式 yyyy-MM-ddTHH:mm:ss
        notify.setTime(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new java.util.Date()));
        notify.setLongitude(116.397_128);
        notify.setLatitude(39.916_527);
        notify.setSpeed(0.0);
        notify.setDirection(0.0);
        notify.setAltitude(50.0);
        return notify;
    }

    /**
     * 报警查询应答：模拟一条告警记录。
     */
    @Override
    public DeviceAlarmNotify onAlarmQuery(String platformId, DeviceAlarmQuery query) {
        publish("clientcmd.query.alarm", platformId, query.getSn());
        DeviceAlarmNotify notify =
            new DeviceAlarmNotify(CmdTypeEnum.ALARM.getType(), query.getSn(), clientProps.getClientId());
        notify.setAlarmPriority("1");
        notify.setAlarmMethod("1");
        notify.setAlarmTime(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new java.util.Date()));
        return notify;
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
