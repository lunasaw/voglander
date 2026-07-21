package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;

@DisplayName("Scheduled task natural terminal evaluator")
class TaskTerminalStateEvaluatorTest {

    private final TaskTerminalStateEvaluator evaluator = new TaskTerminalStateEvaluator();

    @ParameterizedTest(name = "success={0}, failed={1}, missed={2}, cancelled={3}, state={4} -> {5}")
    @MethodSource("terminalCases")
    @DisplayName("cursor 耗尽且全部计划点终结时应得到唯一自然终态")
    void resolve_shouldCoverCompletedPartialFailedAndCancelled(int success, int failed, int missed,
        int cancelled, String currentState, String expected) {
        BizTaskDTO task = task(success, failed, missed, cancelled, currentState);

        assertEquals(expected, evaluator.resolve(task));
    }

    @Test
    @DisplayName("cursor 未耗尽或仍有非终态 execution 时不得提前终结")
    void resolve_shouldWaitForCursorAndEveryTerminalFact() {
        BizTaskDTO cursorRemaining = task(4, 0, 0, 0, "RUNNING");
        cursorRemaining.setNextPlanTime(LocalDateTime.of(2026, 7, 15, 10, 1));
        assertNull(evaluator.resolve(cursorRemaining));

        BizTaskDTO executionRemaining = task(3, 0, 0, 0, "RUNNING");
        assertNull(evaluator.resolve(executionRemaining));
    }

    private static Stream<Arguments> terminalCases() {
        return Stream.of(
            Arguments.of(4, 0, 0, 0, "RUNNING", "COMPLETED"),
            Arguments.of(2, 1, 1, 0, "RUNNING", "PARTIAL_COMPLETED"),
            Arguments.of(0, 2, 2, 0, "RUNNING", "FAILED"),
            Arguments.of(0, 0, 0, 4, "CANCELLING", "CANCELLED"));
    }

    private BizTaskDTO task(int success, int failed, int missed, int cancelled, String state) {
        BizTaskDTO task = new BizTaskDTO();
        task.setTaskMode("FIXED_RATE");
        task.setState(state);
        task.setNextPlanTime(null);
        task.setPlannedCount(4);
        task.setSuccessCount(success);
        task.setFailedCount(failed);
        task.setMissedCount(missed);
        task.setCancelledCount(cancelled);
        return task;
    }
}
