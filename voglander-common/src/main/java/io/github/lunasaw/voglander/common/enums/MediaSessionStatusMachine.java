package io.github.lunasaw.voglander.common.enums;

import java.util.Map;
import java.util.Set;

import io.github.lunasaw.voglander.common.constant.media.MediaSessionConstant.Status;

/**
 * 媒体会话状态机（PROTOCOL-S6.2）。
 * <p>
 * 集中定义合法状态转移，消除"任意状态可互转"导致的隐患（典型：晚到的 {@code Session.InviteOk}
 * 把已 CLOSED/FAILED 的会话复活为 ACTIVE）。由 {@code MediaSessionManager} 在状态变更出口统一校验。
 * </p>
 * <ul>
 * <li>{@code INVITING → {ACTIVE, FAILED, CLOSED}}：占位会话可被建立、失败或回收</li>
 * <li>{@code ACTIVE → {ACTIVE, CLOSED, FAILED}}：ACTIVE 幂等；可正常关流；异常可置失败</li>
 * <li>任意状态 {@code → CLOSED}：关流幂等，允许从任何状态收口</li>
 * <li>终态 {@code CLOSED/FAILED → ACTIVE/INVITING}：<strong>非法</strong>，不可复活/回退</li>
 * </ul>
 *
 * @author luna
 */
public final class MediaSessionStatusMachine {

    /**
     * from 状态 → 允许转入的 to 状态集合（CLOSED 作为通用收口出口，单独放行，见 {@link #isLegal}）。
     */
    private static final Map<Integer, Set<Integer>> ALLOWED = Map.of(
        Status.INVITING, Set.of(Status.ACTIVE, Status.FAILED, Status.CLOSED),
        Status.ACTIVE, Set.of(Status.ACTIVE, Status.CLOSED, Status.FAILED),
        Status.FAILED, Set.of(Status.CLOSED),
        Status.CLOSED, Set.of(Status.CLOSED));

    private MediaSessionStatusMachine() {}

    /**
     * 校验状态转移是否合法。
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return 合法返回 true
     */
    public static boolean isLegal(Integer from, Integer to) {
        if (to == null) {
            return false;
        }
        // 关流是通用收口：任意状态 → CLOSED 始终合法（幂等）
        if (Status.CLOSED == to) {
            return true;
        }
        if (from == null) {
            return false;
        }
        Set<Integer> allowed = ALLOWED.get(from);
        return allowed != null && allowed.contains(to);
    }
}
