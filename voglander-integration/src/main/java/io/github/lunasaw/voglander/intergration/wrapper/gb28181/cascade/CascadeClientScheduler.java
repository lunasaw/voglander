package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import io.github.lunasaw.voglander.manager.manager.CascadePlatformManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 级联上级平台注册与心跳调度器。
 * <p>
 * 对每个 enabled=1 的上级平台：
 * 启动后及每 55 秒（< registerExpires=3600s 的续期间隔，心跳由 REGISTER 刷新）触发注册。
 * 注册成功由 {@link CascadeClientRegisterListener} 更新 registerStatus。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CascadeClientScheduler {

    private final CascadePlatformManager  cascadePlatformManager;
    private final CascadeDeviceSupplier   cascadeDeviceSupplier;

    /** 每 55 秒刷新注册（远小于 3600s 过期时间） */
    @Scheduled(fixedDelay = 55_000, initialDelay = 5_000)
    public void refreshRegistrations() {
        CascadePlatformDTO query = new CascadePlatformDTO();
        query.setEnabled(1);
        List<CascadePlatformDTO> platforms = cascadePlatformManager.getPage(query, 1, 200).getRecords();
        for (CascadePlatformDTO platform : platforms) {
            try {
                register(platform);
            } catch (Exception e) {
                log.error("级联注册失败: platformId={}", platform.getPlatformId(), e);
                cascadePlatformManager.updateRegisterStatus(platform.getId(), 3); // FAILED
            }
        }
    }

    private void register(CascadePlatformDTO platform) {
        FromDevice from = cascadeDeviceSupplier.buildFromDevice(platform);
        ToDevice to = cascadeDeviceSupplier.buildToDevice(platform);
        log.info("发起级联注册: localClientId={} → platformId={}", platform.getLocalClientId(), platform.getPlatformId());
        cascadePlatformManager.updateRegisterStatus(platform.getId(), 2); // REGISTERING
        ClientCommandSender.sendRegisterCommand(from, to, platform.getRegisterExpires());
    }
}
