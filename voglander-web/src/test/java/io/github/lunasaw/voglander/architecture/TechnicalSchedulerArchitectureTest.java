package io.github.lunasaw.voglander.architecture;

import io.github.lunasaw.voglander.client.service.task.LongTaskHandler;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TechnicalSchedulerArchitectureTest {

    private static final String TECHNICAL_SCHEDULER_ANNOTATION =
        "io.github.lunasaw.voglander.common.anno.TechnicalScheduler";

    private static final Map<String, String> EXPECTED_SCHEDULERS = expectedSchedulers();

    @Test
    void scheduledComponentsMustBeExplicitlyClassifiedOutsideBusinessTaskScope() throws Exception {
        Set<Class<?>> scheduledTypes = findScheduledTypes();
        Set<String> actualNames = new TreeSet<>();
        for (Class<?> type : scheduledTypes) {
            actualNames.add(type.getName());
        }

        assertEquals(EXPECTED_SCHEDULERS.keySet(), actualNames,
            "新增或删除 @Scheduled 组件时必须同步确认其技术调度分类");

        for (Class<?> type : scheduledTypes) {
            Annotation classification = findTechnicalSchedulerAnnotation(type);
            assertNotNull(classification, () -> type.getName() + " 必须显式标注 @TechnicalScheduler");
            assertEquals(EXPECTED_SCHEDULERS.get(type.getName()), categoryOf(classification),
                () -> type.getName() + " 的技术调度分类不正确");
            assertFalse(LongTaskHandler.class.isAssignableFrom(type),
                () -> type.getName() + " 不得作为业务长任务 Handler");
            assertNoBusinessTaskDependency(type);
        }
    }

    @Test
    void technicalSchedulersRemainEnabledIndependentlyOfBusinessTaskEngine() throws Exception {
        Class<?> applicationType = Class.forName("io.github.lunasaw.voglander.web.ApplicationWeb");
        assertNotNull(AnnotationUtils.findAnnotation(applicationType, EnableScheduling.class),
            "技术调度器必须由应用级 @EnableScheduling 启用，不能依赖业务任务开关");
    }

    private Set<Class<?>> findScheduledTypes() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        CachingMetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resolver);
        Set<Class<?>> result = new LinkedHashSet<>();
        for (Resource resource : resolver.getResources("classpath*:io/github/lunasaw/voglander/**/*.class")) {
            String className = metadataReaderFactory.getMetadataReader(resource).getClassMetadata().getClassName();
            if (className.contains("$$") || className.endsWith("package-info")) {
                continue;
            }
            Class<?> type = Class.forName(className, false, getClass().getClassLoader());
            if (Arrays.stream(type.getDeclaredMethods()).anyMatch(method -> method.isAnnotationPresent(Scheduled.class))) {
                result.add(type);
            }
        }
        return result;
    }

    private Annotation findTechnicalSchedulerAnnotation(Class<?> type) {
        return Arrays.stream(type.getAnnotations())
            .filter(annotation -> annotation.annotationType().getName().equals(TECHNICAL_SCHEDULER_ANNOTATION))
            .findFirst()
            .orElse(null);
    }

    private String categoryOf(Annotation annotation) throws Exception {
        Object category = annotation.annotationType().getMethod("category").invoke(annotation);
        return ((Enum<?>) category).name();
    }

    private void assertNoBusinessTaskDependency(Class<?> type) {
        Set<Class<?>> referencedTypes = new LinkedHashSet<>();
        referencedTypes.addAll(Arrays.asList(type.getInterfaces()));
        if (type.getSuperclass() != null) {
            referencedTypes.add(type.getSuperclass());
        }
        for (Field field : type.getDeclaredFields()) {
            referencedTypes.add(field.getType());
        }
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            referencedTypes.addAll(Arrays.asList(constructor.getParameterTypes()));
        }
        for (Method method : type.getDeclaredMethods()) {
            referencedTypes.add(method.getReturnType());
            referencedTypes.addAll(Arrays.asList(method.getParameterTypes()));
            referencedTypes.addAll(Arrays.asList(method.getExceptionTypes()));
        }

        assertTrue(referencedTypes.stream().map(Class::getName).noneMatch(this::isBusinessTaskType),
            () -> type.getName() + " 不得依赖业务任务 SPI、Service 或 Handler");
    }

    private boolean isBusinessTaskType(String typeName) {
        // Retention cleanup is deliberately a maintenance scheduler.  It may read the
        // validated task configuration (for the retention knobs) but must not depend on
        // the task SPI, persistence managers, or worker implementation.
        if ("io.github.lunasaw.voglander.service.task.BusinessTaskProperties".equals(typeName)) {
            return false;
        }
        return typeName.startsWith("io.github.lunasaw.voglander.client.domain.task.")
            || typeName.startsWith("io.github.lunasaw.voglander.client.service.task.")
            || typeName.startsWith("io.github.lunasaw.voglander.service.task.");
    }

    private static Map<String, String> expectedSchedulers() {
        Map<String, String> schedulers = new LinkedHashMap<>();
        schedulers.put("io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeRecordRequestScheduler",
            "PROTOCOL");
        schedulers.put("io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeSubscribeCleanScheduler",
            "PROTOCOL");
        schedulers.put("io.github.lunasaw.voglander.manager.manager.DeviceManager", "MAINTENANCE");
        schedulers.put("io.github.lunasaw.voglander.manager.routing.NodeAliveService", "MAINTENANCE");
        schedulers.put("io.github.lunasaw.voglander.manager.spring.SpringDynamicTask", "MAINTENANCE");
        schedulers.put("io.github.lunasaw.voglander.service.image.ImageStorageReconciliationService", "MAINTENANCE");
        schedulers.put("io.github.lunasaw.voglander.service.live.LiveSessionGcService", "MAINTENANCE");
        schedulers.put("io.github.lunasaw.voglander.service.sse.LocalSseEventBus", "MAINTENANCE");
        schedulers.put("io.github.lunasaw.voglander.service.sse.RedisBackedSseEventBus", "MAINTENANCE");
        schedulers.put("io.github.lunasaw.voglander.service.stream.impl.StreamProxyBizServiceImpl", "MAINTENANCE");
        schedulers.put("io.github.lunasaw.voglander.service.subscription.SubscriptionRefreshScheduler", "PROTOCOL");
        schedulers.put("io.github.lunasaw.voglander.service.task.BusinessTaskEventRetentionScheduler", "MAINTENANCE");
        return schedulers;
    }
}
