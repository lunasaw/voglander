package io.github.lunasaw.voglander.common.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a protocol or maintenance scheduler that is intentionally outside the durable business-task engine.
 *
 * <p>These schedulers are enabled by the application's scheduling infrastructure and must remain independent of
 * {@code voglander.task.*} feature flags, task handlers, task persistence and worker dispatch.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TechnicalScheduler {

    /** Scheduler responsibility used by architecture tests and documentation. */
    Category category();

    enum Category {
        /** Protocol keepalive, subscription or protocol-state maintenance. */
        PROTOCOL,

        /** Cache, session, node, stream or other system maintenance. */
        MAINTENANCE
    }
}
