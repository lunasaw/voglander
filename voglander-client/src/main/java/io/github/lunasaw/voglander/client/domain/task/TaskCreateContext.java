package io.github.lunasaw.voglander.client.domain.task;

/** Trusted actor and subject facts available during Handler validation. */
public final class TaskCreateContext {
    private final String ownerType;
    private final String ownerId;
    private final String organizationId;
    private final String subjectType;
    private final String subjectId;

    public TaskCreateContext(String ownerType, String ownerId, String organizationId, String subjectType,
        String subjectId) {
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.organizationId = organizationId;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
    }

    public String ownerType() { return ownerType; }
    public String ownerId() { return ownerId; }
    public String organizationId() { return organizationId; }
    public String subjectType() { return subjectType; }
    public String subjectId() { return subjectId; }
}
