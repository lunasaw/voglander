package io.github.lunasaw.voglander.service.image;

import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.client.domain.task.TaskCompletionContext;
import io.github.lunasaw.voglander.client.service.task.TaskCompletionParticipant;
import io.github.lunasaw.voglander.manager.assembler.ImageAssetAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetSourceDTO;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;

/** Same-datasource idempotent asset/source registration for a successful capture. */
@Component
@ConditionalOnProperty(prefix = "voglander.image", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ImageCollectionCompletionParticipant implements TaskCompletionParticipant {
    private final ImageAssetManager assetManager;

    public ImageCollectionCompletionParticipant(ImageAssetManager assetManager) {
        this.assetManager = Objects.requireNonNull(assetManager, "assetManager");
    }

    @Override
    public void complete(TaskCompletionContext context, JSONObject completionData) {
        if (context == null || completionData == null) throw new IllegalArgumentException("completion facts are required");
        JSONObject assetJson = completionData.getJSONObject("asset");
        JSONObject sourceJson = completionData.getJSONObject("source");
        ImageAssetDTO asset = assetJson == null ? null : assetJson.toJavaObject(ImageAssetDTO.class);
        ImageAssetSourceDTO source = sourceJson == null ? null : sourceJson.toJavaObject(ImageAssetSourceDTO.class);
        if (asset == null || source == null) throw new IllegalArgumentException("asset/source completion facts are required");
        source.setSourceTaskId(context.taskId());
        source.setSourceExecutionId(context.executionId());
        assetManager.createWithSource(asset, source);
    }
}
