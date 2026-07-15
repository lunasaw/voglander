package io.github.lunasaw.voglander.client.domain.task;

/** Stable public reference to a domain result. */
public final class TaskResultReference {
    private final String type;
    private final String id;

    public TaskResultReference(String type, String id) {
        if (isBlank(type) || isBlank(id)) {
            throw new IllegalArgumentException("result reference type and id must not be blank");
        }
        this.type = type;
        this.id = id;
    }

    public String type() { return type; }
    public String id() { return id; }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
