package io.github.lunasaw.voglander.service.live.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.lunasaw.zlm.config.ZlmNode;

/**
 * S6.1：媒体节点故障转移选择逻辑单测。
 * <p>
 * 主选（负载均衡）命中即用；主选为空时按 weight 降序从候选中取首个 enabled 节点兜底；
 * 全不可用才返回 null（由编排层抛 LIVE_NODE_UNAVAILABLE）。消除"主选 null 即直接失败"的单点脆弱。
 * </p>
 *
 * @author luna
 */
@DisplayName("S6.1 — 节点故障转移选择")
class MediaPlayServiceNodeFailoverTest {

    private ZlmNode node(String id, int weight, boolean enabled) {
        ZlmNode n = new ZlmNode();
        n.setServerId(id);
        n.setWeight(weight);
        n.setEnabled(enabled);
        return n;
    }

    @Test
    @DisplayName("主选命中 → 直接用主选，不看候选")
    void primaryPresent_usesPrimary() {
        ZlmNode primary = node("p", 1, true);
        ZlmNode chosen = MediaPlayServiceImpl.chooseNode(primary,
            List.of(node("a", 99, true)));
        assertSame(primary, chosen);
    }

    @Test
    @DisplayName("主选为空 → 候选按 weight 降序取首个 enabled")
    void primaryNull_fallsBackToHighestWeightEnabled() {
        ZlmNode high = node("high", 50, true);
        ZlmNode chosen = MediaPlayServiceImpl.chooseNode(null,
            List.of(node("low", 10, true), high, node("mid", 30, true)));
        assertSame(high, chosen);
    }

    @Test
    @DisplayName("主选为空 → 跳过 disabled，取权重最高的 enabled")
    void primaryNull_skipsDisabled() {
        ZlmNode enabledMid = node("mid", 30, true);
        ZlmNode chosen = MediaPlayServiceImpl.chooseNode(null,
            List.of(node("disabledHigh", 99, false), enabledMid));
        assertSame(enabledMid, chosen);
    }

    @Test
    @DisplayName("主选为空且候选全不可用 → 返回 null")
    void primaryNull_allUnavailable_returnsNull() {
        assertNull(MediaPlayServiceImpl.chooseNode(null, List.of(node("d", 99, false))));
        assertNull(MediaPlayServiceImpl.chooseNode(null, List.of()));
        assertNull(MediaPlayServiceImpl.chooseNode(null, null));
    }
}
