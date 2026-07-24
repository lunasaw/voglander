package io.github.lunasaw.voglander.web.api.image;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import io.github.lunasaw.voglander.web.api.image.req.ImageCollectionCreateReq;
import io.github.lunasaw.voglander.web.api.image.req.ImageCollectionRescheduleReq;
import io.github.lunasaw.voglander.web.api.image.vo.ImageCollectionVO;
import io.github.lunasaw.voglander.web.api.sse.controller.SseController;
import io.github.lunasaw.voglander.web.api.task.req.BusinessTaskControlReq;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskDetailVO;
import io.github.lunasaw.voglander.web.api.task.vo.BusinessTaskVO;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

class ImageOpenApiContractTest {

    @Test
    void thumbnailDocumentsFixedProfilesBinaryCachingAndJsonErrors() throws Exception {
        Method method = ImageAssetController.class.getDeclaredMethod("thumbnail", String.class, String.class,
            String.class, String.class);
        Parameter profile = method.getParameters()[3].getAnnotation(Parameter.class);
        assertNotNull(profile);
        assertArrayEquals(new String[] {"table", "gallery"}, profile.schema().allowableValues());

        ApiResponses documented = method.getAnnotation(ApiResponses.class);
        assertNotNull(documented);
        Map<String, ApiResponse> responses = Arrays.stream(documented.value())
            .collect(Collectors.toMap(ApiResponse::responseCode, Function.identity()));
        for (String status : new String[] {"200", "304", "400", "401", "403", "404", "409", "410", "503"}) {
            assertTrue(responses.containsKey(status), "thumbnail 缺少 HTTP " + status + " OpenAPI 响应");
        }
        Schema binary = responses.get("200").content()[0].schema();
        assertEquals("string", binary.type());
        assertEquals("binary", binary.format());
        assertEquals(0, responses.get("304").content().length);
        for (String status : new String[] {"400", "401", "403", "404", "409", "410", "503"}) {
            assertEquals("application/json", responses.get(status).content()[0].mediaType());
        }
    }

    @Test
    void idempotencyExpectedVersionAndSseSubscriptionContractAreExplicit() throws Exception {
        Method upload = ImageAssetController.class.getDeclaredMethod("upload", String.class, String.class,
            MultipartFile.class, String.class);
        assertIdempotencyHeader(upload.getParameters()[1]);
        Method create = ImageCollectionController.class.getDeclaredMethod("create", String.class, String.class,
            ImageCollectionCreateReq.class);
        assertIdempotencyHeader(create.getParameters()[1]);

        assertRequired(field(BusinessTaskControlReq.class, "expectedVersion"));
        assertRequired(field(ImageCollectionRescheduleReq.class, "expectedVersion"));

        Method subscribe = SseController.class.getDeclaredMethod("subscribe", String.class, String.class);
        Parameter topics = subscribe.getParameters()[0].getAnnotation(Parameter.class);
        assertNotNull(topics);
        for (String required : new String[] {"主题前缀", "登录用户", "任意", "非空主题"}) {
            assertTrue(topics.description().contains(required), "SSE OpenAPI 缺少 " + required);
        }
    }

    @Test
    void taskAndCollectionNewFieldsArePublishedAsSchemas() {
        for (String name : new String[] {"version", "lastExecutionId", "resultRefType", "resultRefId",
            "resultSummary"}) {
            assertSchema(field(BusinessTaskVO.class, name));
        }
        for (String name : new String[] {"activeExecution", "capabilities"}) {
            assertSchema(field(BusinessTaskDetailVO.class, name));
        }
        for (String name : new String[] {"version", "scheduleVersion", "capabilities", "lastExecutionId",
            "resultRefType", "resultRefId", "resultSummary"}) {
            assertSchema(field(ImageCollectionVO.class, name));
        }
    }

    private void assertIdempotencyHeader(java.lang.reflect.Parameter parameter) {
        Parameter annotation = parameter.getAnnotation(Parameter.class);
        assertNotNull(annotation);
        assertFalse(annotation.required(), "兼容调用方仍可省略 Idempotency-Key");
        assertTrue(annotation.description().contains("1-128"));
        assertTrue(annotation.description().contains("UI"));
    }

    private void assertRequired(Field field) {
        Schema schema = field.getAnnotation(Schema.class);
        assertNotNull(schema);
        assertEquals(Schema.RequiredMode.REQUIRED, schema.requiredMode());
    }

    private void assertSchema(Field field) {
        assertNotNull(field.getAnnotation(Schema.class), field.getDeclaringClass().getSimpleName() + "."
            + field.getName() + " 未发布 OpenAPI schema 语义");
    }

    private Field field(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name);
        } catch (NoSuchFieldException exception) {
            throw new AssertionError(type.getSimpleName() + " 缺少字段 " + name, exception);
        }
    }
}
