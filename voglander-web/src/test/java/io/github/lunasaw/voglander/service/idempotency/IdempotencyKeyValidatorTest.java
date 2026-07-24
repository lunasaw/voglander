package io.github.lunasaw.voglander.service.idempotency;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;

class IdempotencyKeyValidatorTest {

    @Test
    void optionalKeyAcceptsNullAndVisibleAsciiUpTo128Characters() {
        assertNull(IdempotencyKeyValidator.validateOptional(null));
        assertEquals("upload-01_ABC~", IdempotencyKeyValidator.validateOptional("upload-01_ABC~"));
        assertDoesNotThrow(() -> IdempotencyKeyValidator.validateOptional("x".repeat(128)));
    }

    @Test
    void providedKeyRejectsEmptyWhitespaceControlNonAsciiAndOverlongValues() {
        assertInvalid("");
        assertInvalid(" ");
        assertInvalid("contains space");
        assertInvalid("line\nbreak");
        assertInvalid("中文");
        assertInvalid("x".repeat(129));
    }

    @Test
    void requiredKeyRejectsNull() {
        ServiceException error = assertThrows(ServiceException.class,
            () -> IdempotencyKeyValidator.validateRequired(null));
        assertEquals(ServiceExceptionEnum.PARAM_ERROR.getCode(), error.getCode());
    }

    private static void assertInvalid(String key) {
        ServiceException error = assertThrows(ServiceException.class,
            () -> IdempotencyKeyValidator.validateOptional(key));
        assertEquals(ServiceExceptionEnum.PARAM_ERROR.getCode(), error.getCode());
    }
}
