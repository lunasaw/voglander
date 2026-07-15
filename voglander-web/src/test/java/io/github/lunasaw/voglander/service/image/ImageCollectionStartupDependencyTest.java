package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.service.task.BizTaskCreateService;
import io.github.lunasaw.voglander.service.task.LongTaskHandlerRegistry;

class ImageCollectionStartupDependencyTest {
    @Test
    void imageCollectionRequiresGenericTaskCreateServiceAndRegistersExactlyOnce() {
        assertTrue(BizTaskCreateService.class.isAssignableFrom(BizTaskCreateService.class));
        ImageCollectionTaskHandler handler = new ImageCollectionTaskHandler();
        LongTaskHandlerRegistry registry = new LongTaskHandlerRegistry(Collections.singletonList(handler));
        assertTrue(registry.contains("IMAGE_COLLECTION"));
        assertTrue(registry.require("IMAGE_COLLECTION", 1) == handler);
    }
}
