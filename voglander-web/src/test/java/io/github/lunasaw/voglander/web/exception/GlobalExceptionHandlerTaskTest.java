package io.github.lunasaw.voglander.web.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerTaskTest {

    @Test
    void taskErrorCodesUseTheReservedContiguousRange() {
        List<Integer> taskCodes = Arrays.stream(ServiceExceptionEnum.values())
            .map(ServiceExceptionEnum::getCode)
            .filter(code -> code >= 720000 && code < 730000)
            .sorted()
            .toList();

        assertIterableEquals(List.of(720000, 720001, 720002, 720003, 720004, 720005, 720006,
            720007, 720008, 720009, 720010, 720011, 720012), taskCodes);
    }

    @Test
    void taskCodesUseStableHttpMappings() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = mock(HttpServletRequest.class);
        Map<ServiceExceptionEnum, HttpStatus> expected = Map.ofEntries(
            Map.entry(ServiceExceptionEnum.TASK_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ServiceExceptionEnum.TASK_EXECUTION_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ServiceExceptionEnum.TASK_TYPE_UNREGISTERED, HttpStatus.NOT_FOUND),
            Map.entry(ServiceExceptionEnum.TASK_STATE_CONFLICT, HttpStatus.CONFLICT),
            Map.entry(ServiceExceptionEnum.TASK_SCHEDULE_INVALID, HttpStatus.BAD_REQUEST),
            Map.entry(ServiceExceptionEnum.TASK_LIMIT_EXCEEDED, HttpStatus.BAD_REQUEST),
            Map.entry(ServiceExceptionEnum.TASK_PERMISSION_DENIED, HttpStatus.FORBIDDEN),
            Map.entry(ServiceExceptionEnum.TASK_PAYLOAD_INVALID, HttpStatus.BAD_REQUEST),
            Map.entry(ServiceExceptionEnum.TASK_CLAIM_CONFLICT, HttpStatus.CONFLICT),
            Map.entry(ServiceExceptionEnum.TASK_LEASE_EXPIRED, HttpStatus.SERVICE_UNAVAILABLE),
            Map.entry(ServiceExceptionEnum.TASK_PROGRESS_INVALID, HttpStatus.BAD_REQUEST),
            Map.entry(ServiceExceptionEnum.TASK_RETRY_NOT_ALLOWED, HttpStatus.CONFLICT),
            Map.entry(ServiceExceptionEnum.TASK_HANDLER_FAILED, HttpStatus.SERVICE_UNAVAILABLE));
        expected.forEach((error, status) -> assertEquals(status,
            handler.handleServiceException(new ServiceException(error), request).getStatusCode()));
    }

    @Test
    void existingErrorHttpBehaviorRemainsUnchanged() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = mock(HttpServletRequest.class);

        List.of(ServiceExceptionEnum.UNKNOWN, ServiceExceptionEnum.PARAM_ERROR,
            ServiceExceptionEnum.DEVICE_NOT_FOUND, ServiceExceptionEnum.LIVE_INVITE_TIMEOUT)
            .forEach(error -> assertEquals(HttpStatus.OK,
                handler.handleServiceException(new ServiceException(error), request).getStatusCode()));
        List.of(ServiceExceptionEnum.TOKEN_INVALID, ServiceExceptionEnum.TOKEN_EXPIRED,
            ServiceExceptionEnum.LOGIN_REQUIRED)
            .forEach(error -> assertEquals(HttpStatus.UNAUTHORIZED,
                handler.handleServiceException(new ServiceException(error), request).getStatusCode()));
    }
}
