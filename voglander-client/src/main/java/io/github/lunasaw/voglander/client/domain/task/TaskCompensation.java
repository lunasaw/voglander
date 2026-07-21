package io.github.lunasaw.voglander.client.domain.task;

/** Idempotent external-resource compensation invoked after a completion transaction fails. */
@FunctionalInterface
public interface TaskCompensation {
    void compensate();

    static TaskCompensation noop() {
        return new TaskCompensation() {
            @Override
            public void compensate() {
            }
        };
    }
}
