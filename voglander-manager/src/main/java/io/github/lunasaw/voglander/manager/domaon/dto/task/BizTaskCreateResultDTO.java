package io.github.lunasaw.voglander.manager.domaon.dto.task;

/** Database-authoritative result of a durable task insert attempt. */
public final class BizTaskCreateResultDTO {

    private final boolean created;
    private final BizTaskDTO acceptedTask;
    private final BizTaskExecutionDTO acceptedFirstExecution;

    public BizTaskCreateResultDTO(boolean created, BizTaskDTO acceptedTask,
        BizTaskExecutionDTO acceptedFirstExecution) {
        if (acceptedTask == null) {
            throw new IllegalArgumentException("acceptedTask must not be null");
        }
        this.created = created;
        this.acceptedTask = acceptedTask;
        this.acceptedFirstExecution = acceptedFirstExecution;
    }

    public boolean isCreated() {
        return created;
    }

    public BizTaskDTO getAcceptedTask() {
        return acceptedTask;
    }

    public BizTaskExecutionDTO getAcceptedFirstExecution() {
        return acceptedFirstExecution;
    }
}
