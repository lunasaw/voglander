package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ImageUploadSnapshotTest {

    private final ImageValidationService validation = new ImageValidationService();

    @Test
    void namesArePathAndControlSafeWithStableUnnamedFallback() {
        ImageUploadSnapshot snapshot = ImageUploadSnapshot.from(
            new ImageIngestCommand("USER", "7", "org", "key", " C:\\tmp\\bad\u0000.jpg ",
                "image/jpeg", null),
            "abc123", validation);

        assertEquals("bad.jpg", snapshot.getOriginalFilename());
        assertEquals("bad.jpg", snapshot.getAssetName());

        ImageUploadSnapshot unnamed = ImageUploadSnapshot.from(
            new ImageIngestCommand("USER", "7", null, "key", null, "image/jpeg", null),
            "abc123", validation);
        assertNull(unnamed.getOriginalFilename());
        assertEquals("unnamed", unnamed.getAssetName());
    }

    @Test
    void fingerprintChangesForContentEffectiveNameOrOriginalFilename() {
        ImageIngestCommand base = new ImageIngestCommand("USER", "7", null, "key", "one.jpg",
            "image/jpeg", "asset");
        String fingerprint = ImageUploadSnapshot.from(base, "aaa", validation).fingerprint();

        assertEquals(fingerprint, ImageUploadSnapshot.from(base, "aaa", validation).fingerprint());
        org.junit.jupiter.api.Assertions.assertNotEquals(fingerprint,
            ImageUploadSnapshot.from(base, "bbb", validation).fingerprint());
        org.junit.jupiter.api.Assertions.assertNotEquals(fingerprint,
            ImageUploadSnapshot.from(new ImageIngestCommand("USER", "7", null, "key", "two.jpg",
                "image/jpeg", "asset"), "aaa", validation).fingerprint());
        org.junit.jupiter.api.Assertions.assertNotEquals(fingerprint,
            ImageUploadSnapshot.from(new ImageIngestCommand("USER", "7", null, "key", "one.jpg",
                "image/jpeg", "other"), "aaa", validation).fingerprint());
    }
}
