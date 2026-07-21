package io.github.lunasaw.voglander.web.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerImageTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void imageDomainCodesMapToRealHttpStatuses() {
        Map<ServiceExceptionEnum, HttpStatus> expected = Map.ofEntries(
            Map.entry(ServiceExceptionEnum.IMAGE_FILE_TYPE_UNSUPPORTED, HttpStatus.BAD_REQUEST),
            Map.entry(ServiceExceptionEnum.IMAGE_FILE_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE),
            Map.entry(ServiceExceptionEnum.IMAGE_DECODE_FAILED, HttpStatus.BAD_REQUEST),
            Map.entry(ServiceExceptionEnum.IMAGE_PIXEL_LIMIT_EXCEEDED, HttpStatus.BAD_REQUEST),
            Map.entry(ServiceExceptionEnum.IMAGE_ASSET_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ServiceExceptionEnum.IMAGE_ASSET_STATE_CONFLICT, HttpStatus.CONFLICT),
            Map.entry(ServiceExceptionEnum.IMAGE_STORAGE_WRITE_FAILED, HttpStatus.SERVICE_UNAVAILABLE),
            Map.entry(ServiceExceptionEnum.IMAGE_STORAGE_READ_FAILED, HttpStatus.SERVICE_UNAVAILABLE),
            Map.entry(ServiceExceptionEnum.IMAGE_STORAGE_DELETE_FAILED, HttpStatus.SERVICE_UNAVAILABLE),
            Map.entry(ServiceExceptionEnum.IMAGE_COLLECTION_SCHEDULE_INVALID, HttpStatus.BAD_REQUEST),
            Map.entry(ServiceExceptionEnum.IMAGE_COLLECTION_LIMIT_EXCEEDED, HttpStatus.BAD_REQUEST),
            Map.entry(ServiceExceptionEnum.IMAGE_CAMERA_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ServiceExceptionEnum.IMAGE_CAMERA_OFFLINE, HttpStatus.CONFLICT),
            Map.entry(ServiceExceptionEnum.IMAGE_STREAM_ESTABLISH_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT),
            Map.entry(ServiceExceptionEnum.IMAGE_SNAPSHOT_FAILED, HttpStatus.BAD_GATEWAY),
            Map.entry(ServiceExceptionEnum.IMAGE_PERMISSION_DENIED, HttpStatus.FORBIDDEN));

        expected.forEach((error, status) -> assertEquals(status,
            handler.handleServiceException(new ServiceException(error), request).getStatusCode(), error.name()));
    }

    @Test
    void existingBusinessCodeKeepsHttp200Behavior() {
        assertEquals(HttpStatus.OK,
            handler.handleServiceException(new ServiceException(ServiceExceptionEnum.DEVICE_NOT_FOUND), request)
                .getStatusCode());
    }
}
