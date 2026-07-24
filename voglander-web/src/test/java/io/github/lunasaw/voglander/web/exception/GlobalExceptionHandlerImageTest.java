package io.github.lunasaw.voglander.web.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerImageTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void imageDomainCodesMapToRealHttpStatuses() {
        Map<ServiceExceptionEnum, HttpStatus> expected = Map.ofEntries(
            Map.entry(ServiceExceptionEnum.IDEMPOTENCY_KEY_REUSED, HttpStatus.CONFLICT),
            Map.entry(ServiceExceptionEnum.IMAGE_FILE_TYPE_UNSUPPORTED, HttpStatus.BAD_REQUEST),
            Map.entry(ServiceExceptionEnum.IMAGE_FILE_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE),
            Map.entry(ServiceExceptionEnum.IMAGE_DECODE_FAILED, HttpStatus.BAD_REQUEST),
            Map.entry(ServiceExceptionEnum.IMAGE_PIXEL_LIMIT_EXCEEDED, HttpStatus.BAD_REQUEST),
            Map.entry(ServiceExceptionEnum.IMAGE_ASSET_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ServiceExceptionEnum.IMAGE_ASSET_STATE_CONFLICT, HttpStatus.CONFLICT),
            Map.entry(ServiceExceptionEnum.IMAGE_ASSET_GONE, HttpStatus.GONE),
            Map.entry(ServiceExceptionEnum.IMAGE_THUMBNAIL_PROFILE_INVALID, HttpStatus.BAD_REQUEST),
            Map.entry(ServiceExceptionEnum.IMAGE_THUMBNAIL_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE),
            Map.entry(ServiceExceptionEnum.IMAGE_STORAGE_WRITE_FAILED, HttpStatus.SERVICE_UNAVAILABLE),
            Map.entry(ServiceExceptionEnum.IMAGE_STORAGE_READ_FAILED, HttpStatus.SERVICE_UNAVAILABLE),
            Map.entry(ServiceExceptionEnum.IMAGE_STORAGE_DELETE_FAILED, HttpStatus.SERVICE_UNAVAILABLE),
            Map.entry(ServiceExceptionEnum.IMAGE_COLLECTION_SCHEDULE_INVALID, HttpStatus.BAD_REQUEST),
            Map.entry(ServiceExceptionEnum.IMAGE_COLLECTION_LIMIT_EXCEEDED, HttpStatus.BAD_REQUEST),
            Map.entry(ServiceExceptionEnum.IMAGE_CAMERA_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ServiceExceptionEnum.IMAGE_CAMERA_OFFLINE, HttpStatus.CONFLICT),
            Map.entry(ServiceExceptionEnum.IMAGE_STREAM_ESTABLISH_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT),
            Map.entry(ServiceExceptionEnum.IMAGE_SNAPSHOT_FAILED, HttpStatus.BAD_GATEWAY),
            Map.entry(ServiceExceptionEnum.IMAGE_PERMISSION_DENIED, HttpStatus.FORBIDDEN),
            Map.entry(ServiceExceptionEnum.TASK_STATE_CONFLICT, HttpStatus.CONFLICT));

        expected.forEach((error, status) -> {
            ResponseEntity<AjaxResult> response = handler.handleServiceException(new ServiceException(error), request);
            assertEquals(status, response.getStatusCode(), error.name());
            assertEquals(error.getCode(), response.getBody().getCode(), error.name());
        });
    }

    @Test
    void existingBusinessCodeKeepsHttp200Behavior() {
        ResponseEntity<AjaxResult> response = handler.handleServiceException(
            new ServiceException(ServiceExceptionEnum.DEVICE_NOT_FOUND), request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ServiceExceptionEnum.DEVICE_NOT_FOUND.getCode(), response.getBody().getCode());
    }

    @Test
    void imageDomainErrorsLogOnlyStableCodeAndNeverSensitiveExceptionDetails() {
        Logger logger = (Logger)LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        when(request.getRequestURI()).thenReturn("/api/v1/images/img-safe/thumbnail");
        ServiceException error = new ServiceException(ServiceExceptionEnum.IMAGE_THUMBNAIL_UNAVAILABLE)
            .setDetailMessage("storage/private/customer-secret.jpg");
        error = new ServiceException(error.getCode(), "Bearer raw-token idempotency-key-secret payload-secret")
            .setDetailMessage("storage/private/customer-secret.jpg");

        try {
            handler.handleServiceException(error, request);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        String logged = String.join("\n", appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList());
        assertTrue(logged.contains(String.valueOf(ServiceExceptionEnum.IMAGE_THUMBNAIL_UNAVAILABLE.getCode())));
        for (String secret : List.of("raw-token", "idempotency-key-secret", "payload-secret",
            "customer-secret.jpg")) {
            assertFalse(logged.contains(secret), logged);
        }
        assertTrue(appender.list.stream().noneMatch(event -> event.getThrowableProxy() != null),
            "领域拒绝不应把异常堆栈和 detail 写入普通业务日志");
    }
}
