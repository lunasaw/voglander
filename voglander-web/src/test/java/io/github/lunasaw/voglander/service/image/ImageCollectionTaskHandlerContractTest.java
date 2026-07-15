package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.client.domain.task.TaskCreateContext;
import io.github.lunasaw.voglander.client.domain.task.TaskAttemptContext;
import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;

class ImageCollectionTaskHandlerContractTest {
    @Test
    void handlerExposesStableTypeVersionAndCapabilities() {
        ImageCollectionTaskHandler handler = new ImageCollectionTaskHandler();
        assertEquals(ImageConstant.TASK_TYPE_IMAGE_COLLECTION, handler.taskType());
        assertEquals(ImageConstant.TASK_PAYLOAD_VERSION, handler.payloadVersion());
        org.junit.jupiter.api.Assertions.assertTrue(handler.capabilities().supportsPause());
        org.junit.jupiter.api.Assertions.assertTrue(handler.capabilities().supportsCancel());
        org.junit.jupiter.api.Assertions.assertTrue(handler.capabilities().supportsManualRetry());
    }

    @Test
    void validationRequiresCameraIdentityAndRejectsSecrets() {
        ImageCollectionTaskHandler handler = new ImageCollectionTaskHandler();
        JSONObject valid = new JSONObject(); valid.put("deviceId", "d1"); valid.put("channelId", "c1"); valid.put("payloadVersion", 1);
        handler.validate(new TaskCreateContext("USER", "7", null, "CAMERA", "d1"), valid);
        valid.put("url", "rtsp://secret");
        assertThrows(ServiceException.class, () -> handler.validate(new TaskCreateContext("USER", "7", null, "CAMERA", "d1"), valid));
    }

    @Test
    void classifySeparatesTransientMediaStorageFromPermanentConfigurationAndPermissionFailures() {
        ImageCollectionTaskHandler handler = new ImageCollectionTaskHandler();
        TaskAttemptContext attempt = new TaskAttemptContext("task", "execution", 1, 3, null);
        assertEquals(true, handler.classify(new java.io.IOException("timeout"), attempt).isRetryable());
        assertEquals(true, handler.classify(new ServiceException(ServiceExceptionEnum.IMAGE_SNAPSHOT_FAILED), attempt).isRetryable());
        assertEquals(false, handler.classify(new ServiceException(ServiceExceptionEnum.IMAGE_CAMERA_OFFLINE), attempt).isRetryable());
        assertEquals(false, handler.classify(new ServiceException(ServiceExceptionEnum.IMAGE_FILE_TYPE_UNSUPPORTED), attempt).isRetryable());
        assertEquals(false, handler.classify(new ServiceException(ServiceExceptionEnum.IMAGE_PERMISSION_DENIED), attempt).isRetryable());
    }
}
