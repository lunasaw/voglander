package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceRecordQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceConfigDownload;
import io.github.lunasaw.gb28181.common.entity.query.MobilePositionQuery;
import io.github.lunasaw.gb28181.common.entity.query.PresetQuery;
import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gb28181.common.entity.response.*;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gbproxy.client.api.QueryListener;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.record.VoglanderServerRecordCommand;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.manager.CascadeChannelManager;
import io.github.lunasaw.voglander.manager.manager.CascadePlatformManager;
import io.github.lunasaw.voglander.manager.manager.CascadeRecordRequestManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Stage 3 — 响应上级平台的目录/信息/状态查询。
 * <p>
 * 框架要求此 Bean 唯一（{@code ObjectProvider#getIfUnique()}）。
 * Lab 模式（{@code voglander.protocol-lab.enabled=true}）时由 LabQueryListener 替代，
 * 本 Bean 不注册，避免唯一约束冲突。
 * </p>
 */
@Slf4j
@Component
@ConditionalOnExpression(
    "${sip.client.enabled:false} and not ${voglander.protocol-lab.enabled:false}"
)
@RequiredArgsConstructor
public class CascadeQueryHandler implements QueryListener {

    private final CascadeChannelManager  cascadeChannelManager;
    private final CascadePlatformManager cascadePlatformManager;
    private final CascadeRecordRequestManager cascadeRecordRequestManager;
    private final VoglanderServerRecordCommand serverRecordCommand;

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

    /**
     * 录像查询（C6）：上级查录像 → 登记请求到 tb_cascade_record_request →
     * 转查真实下级设备 → 真实设备响应到达后由 {@link CascadeRecordService} 主动回包上级。
     */
    @Override
    public DeviceRecord onRecordInfoQuery(String platformId, DeviceRecordQuery query) {
        String cascadeChannelId = query.getDeviceId();
        CascadeChannelDTO channel = cascadeChannelManager.getByPlatformAndCascadeChannelId(platformId, cascadeChannelId);
        if (channel == null) {
            log.warn("录像查询找不到级联通道: platformId={}, cascadeChannelId={}", platformId, cascadeChannelId);
            return null; // 框架会自动回 404
        }
        // 登记上级查询请求（携带上级 sn，供响应时重映射）
        cascadeRecordRequestManager.create(platformId, query.getSn(), channel,
            query.getStartTime(), query.getEndTime());
        // 转查真实下级设备（异步，响应到达后 CascadeRecordService 监听事件并主动回包上级）
        serverRecordCommand.queryDeviceRecord(channel.getLocalDeviceId(), query.getStartTime(), query.getEndTime());
        log.info("录像查询已转发: platformId={}, cascadeChannelId={} → localDeviceId={}",
            platformId, cascadeChannelId, channel.getLocalDeviceId());
        // 返回 null 表示异步响应，框架不立即回包（等待 CascadeRecordService 主动推送）
        return null;
    }

    /**
     * 配置下载查询（C11）：上级查本平台配置参数 → 返回基础 OK 应答（平台自身无下级设备配置）。
     */
    @Override
    public DeviceConfigResponse onConfigDownloadQuery(String platformId, DeviceConfigDownload query) {
        log.info("响应配置下载查询: platformId={}, configType={}", platformId, query.getConfigType());
        return new DeviceConfigResponse(query.getSn(), query.getDeviceId(), "OK");
    }

    /**
     * 移动位置查询（C11）：级联平台自身非移动设备，返回 null（框架回 404/不回包），
     * 由上级订阅真实移动设备位置走 {@link CascadeNotifyPublisher} 主动推送。
     */
    @Override
    public MobilePositionNotify onMobilePositionQuery(String platformId, MobilePositionQuery query) {
        log.info("收到移动位置查询(平台自身无位置, 不应答): platformId={}", platformId);
        return null;
    }

    /**
     * 预置位查询（C11）：上级查通道预置位 → 首版返回空列表（预置位由真实设备维护，后续可转查下级）。
     */
    @Override
    public PresetQueryResponse onPresetQuery(String platformId, PresetQuery query) {
        log.info("响应预置位查询(首版空列表): platformId={}, channelId={}", platformId, query.getDeviceId());
        PresetQueryResponse.PresetList presetList =
            new PresetQueryResponse.PresetList(0, java.util.Collections.emptyList());
        return new PresetQueryResponse(query.getSn(), query.getDeviceId(), presetList);
    }
}
