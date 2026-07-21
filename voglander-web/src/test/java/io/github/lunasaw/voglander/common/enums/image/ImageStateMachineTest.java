package io.github.lunasaw.voglander.common.enums.image;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ImageStateMachineTest {

    @Test
    void assetTransitionsAreExplicitAndIdempotent() {
        assertTrue(ImageAssetStatusEnum.AVAILABLE.canTransitionTo(ImageAssetStatusEnum.DELETING));
        assertTrue(ImageAssetStatusEnum.DELETING.canTransitionTo(ImageAssetStatusEnum.DELETED));
        assertTrue(ImageAssetStatusEnum.DELETING.canTransitionTo(ImageAssetStatusEnum.DELETE_FAILED));
        assertTrue(ImageAssetStatusEnum.DELETE_FAILED.canTransitionTo(ImageAssetStatusEnum.DELETING));
        assertTrue(ImageAssetStatusEnum.DELETED.canTransitionTo(ImageAssetStatusEnum.DELETED));
        assertFalse(ImageAssetStatusEnum.DELETED.canTransitionTo(ImageAssetStatusEnum.AVAILABLE));
    }

}
