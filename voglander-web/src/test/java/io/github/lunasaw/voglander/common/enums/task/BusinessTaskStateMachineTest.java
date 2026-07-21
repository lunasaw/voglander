package io.github.lunasaw.voglander.common.enums.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessTaskStateMachineTest {

    @ParameterizedTest(name = "task transition {0} -> {1} allowed={2}")
    @MethodSource("taskTransitions")
    void taskTransitionsAreExplicit(TaskStateEnum source, TaskStateEnum target, boolean allowed) {
        assertEquals(allowed, source.canTransitionTo(target));
    }

    @ParameterizedTest(name = "execution transition {0} -> {1} allowed={2}")
    @MethodSource("executionTransitions")
    void executionTransitionsAreExplicit(TaskExecutionStateEnum source, TaskExecutionStateEnum target,
        boolean allowed) {
        assertEquals(allowed, source.canTransitionTo(target));
    }

    @Test
    void terminalStatesAndNullTargetsNeverReopen() {
        EnumSet.of(TaskStateEnum.COMPLETED, TaskStateEnum.PARTIAL_COMPLETED, TaskStateEnum.FAILED,
            TaskStateEnum.CANCELLED).forEach(state -> {
                assertTrue(state.isTerminal());
                assertFalse(state.canTransitionTo(null));
            });
        EnumSet.of(TaskExecutionStateEnum.SUCCEEDED, TaskExecutionStateEnum.FAILED,
            TaskExecutionStateEnum.MISSED, TaskExecutionStateEnum.CANCELLED).forEach(state -> {
                assertTrue(state.isTerminal());
                assertFalse(state.canTransitionTo(null));
            });
    }

    @ParameterizedTest(name = "stable enum {0}")
    @MethodSource("stableEnumCodes")
    void stableMachineCodesAreLocked(Class<? extends Enum<?>> enumType, List<String> expectedCodes) {
        List<String> actualCodes = Arrays.stream(enumType.getEnumConstants()).map(Enum::name).toList();
        assertEquals(expectedCodes, actualCodes);
    }

    private static Stream<Arguments> taskTransitions() {
        Set<Transition<TaskStateEnum>> allowed = Set.of(
            task(TaskStateEnum.SCHEDULED, TaskStateEnum.RUNNING),
            task(TaskStateEnum.SCHEDULED, TaskStateEnum.PAUSED),
            task(TaskStateEnum.SCHEDULED, TaskStateEnum.CANCELLING),
            task(TaskStateEnum.RUNNING, TaskStateEnum.PAUSED),
            task(TaskStateEnum.RUNNING, TaskStateEnum.CANCELLING),
            task(TaskStateEnum.RUNNING, TaskStateEnum.COMPLETED),
            task(TaskStateEnum.RUNNING, TaskStateEnum.PARTIAL_COMPLETED),
            task(TaskStateEnum.RUNNING, TaskStateEnum.FAILED),
            task(TaskStateEnum.PAUSED, TaskStateEnum.SCHEDULED),
            task(TaskStateEnum.PAUSED, TaskStateEnum.RUNNING),
            task(TaskStateEnum.PAUSED, TaskStateEnum.CANCELLING),
            task(TaskStateEnum.CANCELLING, TaskStateEnum.CANCELLED));
        return Arrays.stream(TaskStateEnum.values())
            .flatMap(source -> Arrays.stream(TaskStateEnum.values())
                .map(target -> Arguments.of(source, target, source == target || allowed.contains(task(source, target)))));
    }

    private static Stream<Arguments> executionTransitions() {
        Set<Transition<TaskExecutionStateEnum>> allowed = Set.of(
            execution(TaskExecutionStateEnum.PENDING, TaskExecutionStateEnum.RUNNING),
            execution(TaskExecutionStateEnum.PENDING, TaskExecutionStateEnum.MISSED),
            execution(TaskExecutionStateEnum.PENDING, TaskExecutionStateEnum.CANCELLED),
            execution(TaskExecutionStateEnum.RUNNING, TaskExecutionStateEnum.SUCCEEDED),
            execution(TaskExecutionStateEnum.RUNNING, TaskExecutionStateEnum.FAILED),
            execution(TaskExecutionStateEnum.RUNNING, TaskExecutionStateEnum.RETRY_WAIT),
            execution(TaskExecutionStateEnum.RUNNING, TaskExecutionStateEnum.CANCELLED),
            execution(TaskExecutionStateEnum.RETRY_WAIT, TaskExecutionStateEnum.RUNNING),
            execution(TaskExecutionStateEnum.RETRY_WAIT, TaskExecutionStateEnum.FAILED),
            execution(TaskExecutionStateEnum.RETRY_WAIT, TaskExecutionStateEnum.CANCELLED));
        return Arrays.stream(TaskExecutionStateEnum.values())
            .flatMap(source -> Arrays.stream(TaskExecutionStateEnum.values())
                .map(target -> Arguments.of(source, target,
                    source == target || allowed.contains(execution(source, target)))));
    }

    private static Stream<Arguments> stableEnumCodes() {
        return Stream.of(
            Arguments.of(BusinessTaskTypeEnum.class,
                List.of("IMAGE_COLLECTION", "DATA_EXPORT", "DATA_IMPORT", "AI_ANALYSIS", "TEST")),
            Arguments.of(TaskModeEnum.class, List.of("ONCE", "AT_TIME", "FIXED_RATE")),
            Arguments.of(TaskStateEnum.class,
                List.of("SCHEDULED", "RUNNING", "PAUSED", "CANCELLING", "COMPLETED", "PARTIAL_COMPLETED",
                    "FAILED", "CANCELLED")),
            Arguments.of(TaskExecutionStateEnum.class,
                List.of("PENDING", "RUNNING", "RETRY_WAIT", "SUCCEEDED", "FAILED", "MISSED", "CANCELLED")),
            Arguments.of(TaskEventTypeEnum.class,
                List.of("CREATED", "SCHEDULED", "CLAIMED", "STARTED", "PROGRESS", "RETRY_SCHEDULED",
                    "SUCCEEDED", "FAILED", "MISSED", "PAUSED", "RESUMED", "CANCELLING", "CANCELLED",
                    "LEASE_EXPIRED", "MANUAL_RETRY")),
            Arguments.of(TaskCapabilityEnum.class,
                List.of("PAUSE", "CANCEL", "MANUAL_RETRY", "PROGRESS", "RESCHEDULE")),
            Arguments.of(TaskFailureCodeEnum.class,
                List.of("HANDLER_NOT_FOUND", "PAYLOAD_INVALID", "CLAIM_CONFLICT", "LEASE_EXPIRED",
                    "EXECUTION_TIMEOUT", "CANCELLED", "QUEUE_SATURATED", "RETRY_EXHAUSTED", "COMPLETION_FAILED",
                    "SYSTEM_ERROR")));
    }

    private static Transition<TaskStateEnum> task(TaskStateEnum source, TaskStateEnum target) {
        return new Transition<>(source, target);
    }

    private static Transition<TaskExecutionStateEnum> execution(TaskExecutionStateEnum source,
        TaskExecutionStateEnum target) {
        return new Transition<>(source, target);
    }

    private record Transition<T extends Enum<T>>(T source, T target) {
    }
}
