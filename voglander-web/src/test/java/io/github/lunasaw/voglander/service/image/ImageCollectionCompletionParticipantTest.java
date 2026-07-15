package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.client.domain.task.TaskCompletionContext;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetSourceDTO;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;

class ImageCollectionCompletionParticipantTest {
    @Test
    void complete_shouldBindStableTaskAndExecutionIdentityBeforeIdempotentDomainInsert() {
        ImageAssetManager assetManager = mock(ImageAssetManager.class);
        ImageCollectionCompletionParticipant participant = new ImageCollectionCompletionParticipant(assetManager);
        ImageAssetDTO asset = new ImageAssetDTO(); asset.setAssetId("img_1"); asset.setAssetName("img_1");
        asset.setOwnerType("SYSTEM"); asset.setOwnerId("IMAGE_COLLECTION"); asset.setStorageKey("images/x.jpg");
        ImageAssetSourceDTO source = new ImageAssetSourceDTO(); source.setAssetId("img_1");
        JSONObject completion = new JSONObject(); completion.put("asset", JSONObject.parseObject(com.alibaba.fastjson2.JSON.toJSONString(asset)));
        completion.put("source", JSONObject.parseObject(com.alibaba.fastjson2.JSON.toJSONString(source)));

        participant.complete(new TaskCompletionContext("btask_1", "bexec_1", 1), completion);

        verify(assetManager).createWithSource(argThat(value -> "img_1".equals(value.getAssetId())),
            argThat(value -> "btask_1".equals(value.getSourceTaskId())
                && "bexec_1".equals(value.getSourceExecutionId())));
    }

    @Test
    void complete_shouldRejectMissingCompletionFacts() {
        ImageCollectionCompletionParticipant participant = new ImageCollectionCompletionParticipant(mock(ImageAssetManager.class));
        assertThrows(IllegalArgumentException.class,
            () -> participant.complete(new TaskCompletionContext("task", "execution", 1), new JSONObject()));
    }
}
