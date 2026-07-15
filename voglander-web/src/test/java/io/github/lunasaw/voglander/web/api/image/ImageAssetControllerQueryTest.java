package io.github.lunasaw.voglander.web.api.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetSourceDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetStatisticsDTO;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;
import io.github.lunasaw.voglander.service.image.ImageAssetLifecycleService;
import io.github.lunasaw.voglander.service.image.ImageIngestService;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.web.api.image.assembler.ImageAssetWebAssembler;

class ImageAssetControllerQueryTest {
    @Test
    void constraintsStatisticsPageAndDetailUseTheSameModuleScopeAndHideStorageFields() {
        ImageActorResolver resolver = mock(ImageActorResolver.class);
        UserDTO actor = new UserDTO(); actor.setId(7L);
        when(resolver.resolve("Bearer token")).thenReturn(actor);
        doNothing().when(resolver).require(any(), any());
        ImageAssetManager manager = mock(ImageAssetManager.class);
        ImageAssetDTO asset = new ImageAssetDTO(); asset.setAssetId("img_1"); asset.setAssetName("one.jpg");
        asset.setStatus("AVAILABLE"); asset.setContentType("image/jpeg"); asset.setImageFormat("JPEG");
        asset.setFileSize(12L); asset.setWidth(2); asset.setHeight(3); asset.setChecksum("abc");
        asset.setStorageKey("internal/key.jpg"); asset.setCapturedAt(LocalDateTime.of(2026, 7, 15, 1, 2));
        ImageAssetSourceDTO source = new ImageAssetSourceDTO(); source.setAssetId("img_1"); source.setSourceType("USER_UPLOAD");
        when(manager.statistics(null, null)).thenReturn(new ImageAssetStatisticsDTO());
        when(manager.getByAssetId("img_1")).thenReturn(asset);
        Page<ImageAssetDTO> page = new Page<>(1, 20, 1); page.setRecords(List.of(asset));
        when(manager.getPage(any(), org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(20L))).thenReturn(page);
        when(manager.getSourceByAssetId("img_1")).thenReturn(source);
        ImageAssetController controller = new ImageAssetController(resolver, manager, new ImageAssetWebAssembler(),
            mock(ImageIngestService.class), mock(ImageAssetLifecycleService.class), mock(io.github.lunasaw.voglander.client.service.image.ImageStorageService.class), new ImageProperties());

        var constraints = controller.constraints("Bearer token").getData();
        assertEquals(ImageConstant.DEFAULT_MAX_PIXELS, constraints.getMaxPixels());
        controller.statistics("Bearer token");
        var result = controller.page("Bearer token", null, 1, 20).getData();
        assertEquals(1L, result.get("total"));
        @SuppressWarnings("unchecked")
        List<io.github.lunasaw.voglander.web.api.image.vo.ImageAssetVO> items = (List<io.github.lunasaw.voglander.web.api.image.vo.ImageAssetVO>) result.get("items");
        assertEquals("img_1", items.get(0).getAssetId());
        assertEquals(null, readProperty(items.get(0), "storageKey"));
        controller.detail("Bearer token", "img_1");
        verify(manager).statistics(null, null);
        verify(manager).getPage(any(), org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(20L));
    }

    private static Object readProperty(Object value, String property) {
        var field = ReflectionUtils.findField(value.getClass(), property);
        return field == null ? null : ReflectionUtils.getField(field, value);
    }
}
