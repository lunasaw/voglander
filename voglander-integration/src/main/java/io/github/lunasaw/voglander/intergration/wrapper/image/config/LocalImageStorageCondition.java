package io.github.lunasaw.voglander.intergration.wrapper.image.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** Enables the local provider only when image support is enabled and provider is LOCAL. */
public final class LocalImageStorageCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String enabled = context.getEnvironment().getProperty("voglander.image.enabled", "true");
        String provider = context.getEnvironment().getProperty("voglander.image.storage.provider", "LOCAL");
        return Boolean.parseBoolean(enabled) && "LOCAL".equalsIgnoreCase(provider);
    }
}
