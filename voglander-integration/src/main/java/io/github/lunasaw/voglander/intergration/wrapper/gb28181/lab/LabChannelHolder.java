package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 协议验证台「模拟通道」配置持有者（Lab 模式专用）。
 * <p>
 * 被动回应（{@code LabQueryListener.onCatalogQuery}）与主动上报（{@code LabSipClient.pushCatalog}）
 * 共享同一配置：通道数量 + 名称前缀。默认 4 通道、前缀 "Lab-ch"，与现状回包完全一致。
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabChannelHolder {

    public static final int    DEFAULT_COUNT       = 4;
    public static final String DEFAULT_NAME_PREFIX = "Lab-ch";

    @Getter
    @AllArgsConstructor
    public static class Config {
        private final int    count;
        private final String namePrefix;
    }

    private final AtomicReference<Config> ref =
        new AtomicReference<>(new Config(DEFAULT_COUNT, DEFAULT_NAME_PREFIX));

    public void update(Integer count, String namePrefix) {
        int c = (count != null && count > 0) ? count : DEFAULT_COUNT;
        String p = StringUtils.isNotBlank(namePrefix) ? namePrefix : DEFAULT_NAME_PREFIX;
        ref.set(new Config(c, p));
        log.info("Lab 模拟通道配置已更新: count={}, namePrefix={}", c, p);
    }

    public Config current() {
        return ref.get();
    }

    /**
     * 生成本设备第 {@code n} 个通道的编码（n 从 1 开始）。
     * <p>
     * Lab 测试台约定的通道编码规则（<b>非 GB28181 标准</b>，仅为测试简化）：
     * 取设备ID的前18位 + 2位通道序号，保证通道ID也是标准的20位编码。
     * 例如：设备 34020000001320000011，通道1 = 340200000013200000 + 01 = 34020000001320000001
     * </p>
     */
    public String channelIdOf(String clientId, int n) {
        if (clientId == null || clientId.length() < 18) {
            // 回退：不足18位时直接拼接（非标，但保持兼容）
            return clientId + String.format("%02d", n);
        }
        // 标准20位：前18位 + 2位序号
        return clientId.substring(0, 18) + String.format("%02d", n);
    }

    /**
     * 判定 {@code channelId} 是否为设备 {@code clientId} 拥有的某个通道。
     * <p>
     * 依据 {@link #channelIdOf} 的编码规则反推：channelId 须等于 {@code clientId + 两位序号}，
     * 且序号 ∈ [1, {@link Config#getCount() count}]。与目录回包枚举的通道集合严格一致。
     * </p>
     */
    public boolean ownsChannel(String clientId, String channelId) {
        if (clientId == null || channelId == null) {
            return false;
        }
        int count = current().getCount();
        for (int n = 1; n <= count; n++) {
            if (channelIdOf(clientId, n).equals(channelId)) {
                return true;
            }
        }
        return false;
    }
}
