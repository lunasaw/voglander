package io.github.lunasaw.voglander.web.api.lab.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipServerProperties;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab.LabChannelHolder;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab.LabKeepaliveScheduler;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab.LabSessionHolder;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab.LabSipClient;
import io.github.lunasaw.voglander.web.api.lab.domain.LabCatalogPushReq;
import io.github.lunasaw.voglander.web.api.lab.domain.LabRegisterReq;

/**
 * LabClientController 单元测试（纯 Mockito，业务层禁 @SpringBootTest）：
 * register 带参→holder.apply、不带→reset；unregister→reset；catalog/push→channelHolder.update。
 */
@ExtendWith(MockitoExtension.class)
class LabClientControllerTest {

    @Mock LabSipClient                 labSipClient;
    @Mock LabKeepaliveScheduler        labKeepaliveScheduler;
    @Mock LabSessionHolder             labSessionHolder;
    @Mock LabChannelHolder             labChannelHolder;
    @Mock VoglanderSipClientProperties clientProps;
    @Mock VoglanderSipServerProperties serverProps;

    @InjectMocks LabClientController controller;

    @Test
    @DisplayName("register 带目标覆盖 → holder.apply 携带快照，并 register(expires)")
    void register_withOverride_applies() {
        LabRegisterReq req = new LabRegisterReq();
        req.setExpires(1800);
        req.setServerId("44010000002000000001");
        req.setServerIp("10.0.0.9");
        req.setServerPort(15060);
        req.setTransport("TCP");

        controller.register(req);

        ArgumentCaptor<LabSessionHolder.Snapshot> captor = ArgumentCaptor.forClass(LabSessionHolder.Snapshot.class);
        verify(labSessionHolder).apply(captor.capture());
        verify(labSessionHolder, never()).reset();
        LabSessionHolder.Snapshot s = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(s.getServerId()).isEqualTo("44010000002000000001");
        org.assertj.core.api.Assertions.assertThat(s.getServerPort()).isEqualTo(15060);
        org.assertj.core.api.Assertions.assertThat(s.getTransport()).isEqualTo("TCP");
        verify(labSipClient).register(1800);
    }

    @Test
    @DisplayName("register 不带参数 → holder.reset（自环），register(3600 默认)")
    void register_noOverride_resets() {
        controller.register(new LabRegisterReq());   // 全默认，无覆盖

        verify(labSessionHolder).reset();
        verify(labSessionHolder, never()).apply(any());
        verify(labSipClient).register(3600);
    }

    @Test
    @DisplayName("register(null) → reset + 默认 3600")
    void register_nullReq_resets() {
        controller.register(null);
        verify(labSessionHolder).reset();
        verify(labSipClient).register(3600);
    }

    @Test
    @DisplayName("unregister → register(0) + holder.reset")
    void unregister_resets() {
        controller.unregister();
        verify(labSipClient).register(0);
        verify(labSessionHolder).reset();
    }

    @Test
    @DisplayName("catalog/push 带参 → channelHolder.update + pushCatalog")
    void pushCatalog_withReq() {
        LabCatalogPushReq req = new LabCatalogPushReq();
        req.setChannelCount(8);
        req.setCatalogName("Cam");

        controller.pushCatalog(req);

        verify(labChannelHolder).update(8, "Cam");
        verify(labSipClient).pushCatalog(8, "Cam");
    }

    @Test
    @DisplayName("catalog/push 无 body → 默认 4/Lab-ch")
    void pushCatalog_nullReq_default() {
        controller.pushCatalog(null);
        verify(labChannelHolder).update(LabChannelHolder.DEFAULT_COUNT, LabChannelHolder.DEFAULT_NAME_PREFIX);
        verify(labSipClient).pushCatalog(LabChannelHolder.DEFAULT_COUNT, LabChannelHolder.DEFAULT_NAME_PREFIX);
    }

    @Test
    @DisplayName("catalog/push catalogName 空白 → 回落 Lab-ch 默认前缀")
    void pushCatalog_blankName_default() {
        LabCatalogPushReq req = new LabCatalogPushReq();
        req.setChannelCount(2);
        req.setCatalogName("   ");

        controller.pushCatalog(req);

        verify(labChannelHolder).update(eq(2), eq(LabChannelHolder.DEFAULT_NAME_PREFIX));
    }
}
