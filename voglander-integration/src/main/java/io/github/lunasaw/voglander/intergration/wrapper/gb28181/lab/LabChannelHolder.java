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
}
