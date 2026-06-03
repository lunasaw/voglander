package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gb28181.common.entity.response.*;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gbproxy.client.api.QueryListener;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.manager.CascadeChannelManager;
import io.github.lunasaw.voglander.manager.manager.CascadePlatformManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Stage 3 — 响应上级平台的目录/信息/状态查询。
 * <p>
 * 框架要求此 Bean 唯一（{@code ObjectProvider#getIfUnique()}），
 * 所有 default 方法已返回 null 表示不回包，此处只 override 需要的三个方法。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sip.client.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CascadeQueryHandler implements QueryListener {

    private final CascadeChannelManager  cascadeChannelManager;
    private final CascadePlatformManager cascadePlatformManager;

    /** 目录查询：返回本平台对上级暴露的通道列表 */
    @Override
    public DeviceResponse onCatalogQuery(String platformId, DeviceQuery query) {
        List<CascadeChannelDTO> channels = cascadeChannelManager.listByPlatformId(platformId);
        DeviceResponse resp = new DeviceResponse(CmdTypeEnum.CATALOG.getType(), query.getSn(), query.getDeviceId());
        resp.setSumNum(channels.size());
        List<DeviceItem> items = channels.stream()
            .filter(c -> c.getEnabled() != null && c.getEnabled() == 1)
            .map(this::toDeviceItem)
            .collect(Collectors.toList());
        resp.setDeviceItemList(items);
        log.info("响应目录查询: platformId={}, channelCount={}", platformId, items.size());
        return resp;
    }

    /** 设备信息查询：返回本平台基础信息 */
    @Override
    public DeviceInfo onDeviceInfoQuery(String platformId, DeviceQuery query) {
        DeviceInfo info = new DeviceInfo(CmdTypeEnum.DEVICE_INFO.getType(), query.getSn(), query.getDeviceId());
        info.setManufacturer("Voglander");
        info.setModel("CascadePlatform");
        info.setFirmware("v1.0.4");
        return info;
    }

    /** 设备状态查询：平台自身始终在线 */
    @Override
    public DeviceStatus onDeviceStatusQuery(String platformId, DeviceQuery query) {
        DeviceStatus status = new DeviceStatus(CmdTypeEnum.DEVICE_STATUS.getType(), query.getSn(), query.getDeviceId());
        status.setOnline("ONLINE");
        return status;
    }

    private DeviceItem toDeviceItem(CascadeChannelDTO channel) {
        DeviceItem item = new DeviceItem();
        item.setDeviceId(channel.getCascadeChannelId());
        item.setName(channel.getCascadeName() != null ? channel.getCascadeName() : channel.getLocalChannelId());
        item.setStatus("ON"); // 通道具体在线状态可按需从 tb_device_channel 扩展
        item.setParental(0);
        item.setRegisterWay(1);
        item.setSafetyWay(0);
        return item;
    }
}
