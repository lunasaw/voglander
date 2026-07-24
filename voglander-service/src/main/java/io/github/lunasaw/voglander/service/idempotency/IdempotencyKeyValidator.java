package io.github.lunasaw.voglander.service.idempotency;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;

/** Validates opaque idempotency keys without normalizing or logging their value. */
public final class IdempotencyKeyValidator {

    private static final int MAX_LENGTH = 128;

    private IdempotencyKeyValidator() {
    }

    public static String validateOptional(String key) {
        if (key == null) {
            return null;
        }
        if (key.isEmpty() || key.length() > MAX_LENGTH) {
            throw invalid();
        }
        for (int index = 0; index < key.length(); index++) {
            char value = key.charAt(index);
            if (value < 0x21 || value > 0x7e) {
                throw invalid();
            }
        }
        return key;
    }

    public static String validateRequired(String key) {
        if (key == null) {
            throw invalid();
        }
        return validateOptional(key);
    }

    private static ServiceException invalid() {
        return new ServiceException(ServiceExceptionEnum.PARAM_ERROR)
            .setDetailMessage("Idempotency-Key must contain 1-128 visible ASCII characters");
    }
}
