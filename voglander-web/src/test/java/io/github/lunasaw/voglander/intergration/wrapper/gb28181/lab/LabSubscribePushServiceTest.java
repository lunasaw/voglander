package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * LabSubscribePushService 单元测试。
 *
 * <p>核心覆盖：退订语义（{@code expires<=0}）——平台在同一 dialog 发 Expires:0 的 SUBSCRIBE 表示终止订阅
 * （RFC 6665 / GB28181-2022 §9.11），lab 设备须停止推送、不得重排周期任务。这是"关了订阅还在推"的根因修复。</p>
 *
 * <p><strong>非 GB28181 标准实现，仅 lab 自环联调便利</strong>（CLAUDE.md 协议合规铁律）。</p>
 */
@ExtendWith(MockitoExtension.class)
class LabSubscribePushServiceTest {

    @Mock
    LabSipClient              labSipClient;

    @InjectMocks
    LabSubscribePushService   service;

    // ── 退订语义（核心修复） ──────────────────────────────────────────────

    @Test
    @DisplayName("位置退订 expires=0：仅停推，不触发任何 GPS 推送")
    void positionUnsubscribe_zeroExpires_noPush() throws Exception {
        // interval=1s，若按订阅处理会在 1s 内至少推一次；退订则永不��
        service.startPositionPush("platform-1", 0, 1);

        Thread.sleep(1500);
        verify(labSipClient, never()).pushMobilePosition();
    }

    @Test
    @DisplayName("位置退订 expires=null：等同退订，不推送")
    void positionUnsubscribe_nullExpires_noPush() throws Exception {
        service.startPositionPush("platform-1", null, 1);

        Thread.sleep(1500);
        verify(labSipClient, never()).pushMobilePosition();
    }

    @Test
    @DisplayName("先订阅后退订：退订后停止后续推送")
    void positionSubscribeThenUnsubscribe_stopsPush() throws Exception {
        // 订阅：interval=1s，长 expires，应周期推送
        service.startPositionPush("platform-1", 3600, 1);
        verify(labSipClient, timeout(1500).atLeastOnce()).pushMobilePosition();

        // 退订：停推
        service.startPositionPush("platform-1", 0, 1);
        // 等待并记录退订后的推送次数应不再增长
        int after = mockingDetails(labSipClient).getInvocations().size();
        Thread.sleep(1500);
        int later = mockingDetails(labSipClient).getInvocations().size();
        org.assertj.core.api.Assertions.assertThat(later).isEqualTo(after);
    }

    @Test
    @DisplayName("目录退订 expires=0：不推送目录变更")
    void catalogUnsubscribe_zeroExpires_noPush() {
        service.startCatalogPush("platform-1", 0);
        verify(labSipClient, never()).pushCatalogChange(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("目录订阅 expires>0：立即推一条 UPDATE")
    void catalogSubscribe_positiveExpires_pushesOnce() {
        service.startCatalogPush("platform-1", 3600);
        verify(labSipClient, times(1)).pushCatalogChange("UPDATE");
    }

    private static org.mockito.MockingDetails mockingDetails(Object mock) {
        return org.mockito.Mockito.mockingDetails(mock);
    }
}
