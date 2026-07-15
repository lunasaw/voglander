package io.github.lunasaw.voglander.client.domain.task;

/** Sanitized Handler failure classification used by the task engine. */
public final class RetryDecision {
    private final boolean retryable;
    private final String failureCode;
    private final String failureMessage;

    private RetryDecision(boolean retryable, String failureCode, String failureMessage) {
        if (failureCode == null || failureCode.trim().isEmpty()) {
            throw new IllegalArgumentException("failureCode must not be blank");
        }
        this.retryable = retryable;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
    }

    public static RetryDecision retryable(String code, String message) {
        return new RetryDecision(true, code, message);
    }

    public static RetryDecision permanent(String code, String message) {
        return new RetryDecision(false, code, message);
    }

    public boolean isRetryable() { return retryable; }
    public String failureCode() { return failureCode; }
    public String failureMessage() { return failureMessage; }
}
