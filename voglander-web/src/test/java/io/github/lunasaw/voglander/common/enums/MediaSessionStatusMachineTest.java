package io.github.lunasaw.voglander.common.enums;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.common.constant.media.MediaSessionConstant.Status;
import io.github.lunasaw.voglander.common.enums.MediaSessionStatusMachine;

/**
 * S6.2：媒体会话状态机合法性单测。
 * <p>
 * 合法转移：INVITING→{ACTIVE,FAILED,CLOSED}；ACTIVE→{ACTIVE,CLOSED,FAILED}；任意→CLOSED（关流幂等）。
 * 非法转移：终态（CLOSED/FAILED）→ACTIVE（晚到 InviteOk 复活已结束会话）、→INVITING（重回占位）。
 * </p>
 *
 * @author luna
 */
@DisplayName("S6.2 — 媒体会话状态机合法性")
class MediaSessionStatusMachineTest {

    @Test
    @DisplayName("INVITING 可转 ACTIVE/FAILED/CLOSED")
    void invitingTransitions() {
        assertTrue(MediaSessionStatusMachine.isLegal(Status.INVITING, Status.ACTIVE));
        assertTrue(MediaSessionStatusMachine.isLegal(Status.INVITING, Status.FAILED));
        assertTrue(MediaSessionStatusMachine.isLegal(Status.INVITING, Status.CLOSED));
    }

    @Test
    @DisplayName("ACTIVE 可转 ACTIVE(幂等)/CLOSED/FAILED")
    void activeTransitions() {
        assertTrue(MediaSessionStatusMachine.isLegal(Status.ACTIVE, Status.ACTIVE));
        assertTrue(MediaSessionStatusMachine.isLegal(Status.ACTIVE, Status.CLOSED));
        assertTrue(MediaSessionStatusMachine.isLegal(Status.ACTIVE, Status.FAILED));
    }

    @Test
    @DisplayName("任意状态 → CLOSED 合法（关流幂等，含 CLOSED→CLOSED）")
    void anyToClosedLegal() {
        assertTrue(MediaSessionStatusMachine.isLegal(Status.CLOSED, Status.CLOSED));
        assertTrue(MediaSessionStatusMachine.isLegal(Status.FAILED, Status.CLOSED));
    }

    @Test
    @DisplayName("终态 CLOSED → ACTIVE 非法（拒绝晚到 InviteOk 复活）")
    void closedToActiveIllegal() {
        assertFalse(MediaSessionStatusMachine.isLegal(Status.CLOSED, Status.ACTIVE));
    }

    @Test
    @DisplayName("终态 FAILED → ACTIVE 非法")
    void failedToActiveIllegal() {
        assertFalse(MediaSessionStatusMachine.isLegal(Status.FAILED, Status.ACTIVE));
    }

    @Test
    @DisplayName("回退到 INVITING 非法（占位仅由新建产生）")
    void backToInvitingIllegal() {
        assertFalse(MediaSessionStatusMachine.isLegal(Status.ACTIVE, Status.INVITING));
        assertFalse(MediaSessionStatusMachine.isLegal(Status.CLOSED, Status.INVITING));
    }
}
