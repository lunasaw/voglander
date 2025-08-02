package io.github.lunasaw.voglander.intergration.wrapper.gb28181.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.supplier.VoglanderServerDeviceSupplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Voglander GB28181服务端配置
 * 
 * @author luna
 * @since 2025/8/2
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
public class VoglanderGb28181ServerConfig {

    private final VoglanderServerDeviceSupplier voglanderServerDeviceSupplier;

    /**
     * 配置GB28181服务端设备供应器
     * 
     * @return ServerDeviceSupplier Bean
     */
    @Bean
    @ConditionalOnProperty(name = "sip.server.enabled", havingValue = "true")
    @ConditionalOnMissingBean(ServerDeviceSupplier.class)
    public ServerDeviceSupplier serverDeviceSupplier() {
        log.info("创建 Voglander ServerDeviceSupplier Bean");
        return voglanderServerDeviceSupplier;
    }
}