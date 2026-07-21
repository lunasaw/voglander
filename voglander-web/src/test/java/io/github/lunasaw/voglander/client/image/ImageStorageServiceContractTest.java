package io.github.lunasaw.voglander.client.image;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.client.domain.image.ImagePromoteCommand;
import io.github.lunasaw.voglander.client.domain.image.ImageStageCommand;
import io.github.lunasaw.voglander.client.domain.image.StagedImage;
import io.github.lunasaw.voglander.client.service.image.ImageStorageService;

/** Reusable provider contract. Concrete adapters supply an isolated provider instance. */
public abstract class ImageStorageServiceContractTest {

    protected abstract ImageStorageService createProvider() throws Exception;

    @Test
    void stagePromoteOpenAndIdempotentDelete() throws Exception {
        ImageStorageService provider = createProvider();
        byte[] expected = "provider-contract".getBytes(StandardCharsets.UTF_8);
        StagedImage staged = provider.stage(new ImageStageCommand("img_contract", "test", 1024),
            new ByteArrayInputStream(expected));
        String finalKey = "images/2026/07/14/img_contract.jpg";
        provider.promote(new ImagePromoteCommand(staged.stagingKey(), finalKey));

        assertTrue(provider.exists(finalKey));
        try (var content = provider.open(finalKey)) {
            assertArrayEquals(expected, content.inputStream().readAllBytes());
        }
        assertTrue(provider.delete(finalKey));
        assertTrue(provider.delete(finalKey));
        assertFalse(provider.exists(finalKey));
    }
}
