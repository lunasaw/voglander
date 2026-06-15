package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.common.constant.cascade.CascadeConstant;
import io.github.lunasaw.voglander.common.event.LocalRecordInfoEvent;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeRecordRequestDTO;
import io.github.lunasaw.voglander.manager.manager.CascadeChannelManager;
import io.github.lunasaw.voglander.manager.manager.CascadePlatformManager;
import io.github.lunasaw.voglander.manager.manager.CascadeRecordRequestManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 级联录像查询转查聚合服务（C6）。
 *
 * <p>上级查录像 → 本平台转查真实设备 → 真实设备 RecordInfo 响应到达后，
 * 监听 {@link LocalRecordInfoEvent} → 按 (localDeviceId + 时间窗) 找回上级请求 →
 * 重映射 deviceId/sn → 主动回包上级（sendDeviceRecordCommand）。
 *
 * <p>关联逻辑在 {@link CascadeRecordRequestManager#findPending}（按设备+时间窗匹配 PENDING 请求）。
 *
 * @author luna
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sip.client.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CascadeRecordService {

    private final CascadeRecordRequestManager cascadeRecordRequestManager;
    private final CascadePlatformManager      cascadePlatformManager;
    private final CascadeChannelManager       cascadeChannelManager;
    private final CascadeDeviceSupplier       cascadeDeviceSupplier;

    /** 真实设备录像响应到达 → 找回上级请求 → 重映射回包 */
    @EventListener
    @Async("sipNotifierExecutor")
    public void onRecordInfo(LocalRecordInfoEvent e) {
        DeviceRecord record = JSON.parseObject(e.getRecordJson(), DeviceRecord.class);
        if (record == null) {
            log.warn("录像响应 JSON 解析失败, deviceId={}", e.getDeviceId());
            return;
        }
        // 按 (localDeviceId + 时间窗) 找回上级查询请求；时间窗取请求登记时的 startTime/endTime
        // （DeviceRecord 本身不含查询时间窗，只有 recordList 的具体片段）
        CascadeRecordRequestDTO req = cascadeRecordRequestManager.findPending(e.getDeviceId(), null, null);
        if (req == null) {
            log.debug("录像响应未匹配到上级查询请求, deviceId={}", e.getDeviceId());
            return;
        }
        CascadeChannelDTO ch = cascadeChannelManager.getByPlatformAndChannel(
            req.getPlatformId(), req.getLocalChannelId());
        if (ch == null) {
            log.warn("录像响应找到请求但通道映射缺失, reqId={}, localChannelId={}",
                req.getId(), req.getLocalChannelId());
            cascadeRecordRequestManager.markResponded(req.getId());
            return;
        }
        CascadePlatformDTO platform = cascadePlatformManager.getByPlatformId(req.getPlatformId());
        if (platform == null) {
            log.warn("录像响应找到请求但上级平台配置缺失, platformId={}", req.getPlatformId());
            cascadeRecordRequestManager.markResponded(req.getId());
            return;
        }
        // 重映射 deviceId 为上级编码，sn 为上级原始 sn
        record.setDeviceId(ch.getCascadeChannelId());
        record.setSn(req.getSuperiorSn());

        FromDevice from = cascadeDeviceSupplier.buildFromDevice(platform);
        ToDevice   to   = cascadeDeviceSupplier.buildToDevice(platform);
        ClientCommandSender.sendDeviceRecordCommand(from, to, record);
        cascadeRecordRequestManager.markResponded(req.getId());
        log.info("级联录像响应已回包: platformId={}, superiorSn={}, sumNum={}",
            req.getPlatformId(), req.getSuperiorSn(), record.getSumNum());
    }

    /**
     * 定时清理：将 create_time 早于 (now - TIMEOUT) 的 PENDING 请求标为 TIMEOUT，避免僵尸记录堆积。
     * 由外部定时任务调用（如 {@code @Scheduled} 或独立 Spring Bean）。
     */
    public int cleanTimeoutRequests() {
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now()
            .minusSeconds(CascadeConstant.RECORD_REQUEST_TIMEOUT_SEC);
        return cascadeRecordRequestManager.cleanTimeout(cutoff);
    }
}
