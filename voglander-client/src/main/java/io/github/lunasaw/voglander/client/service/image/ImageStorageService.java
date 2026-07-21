package io.github.lunasaw.voglander.client.service.image;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import io.github.lunasaw.voglander.client.domain.image.ImageContent;
import io.github.lunasaw.voglander.client.domain.image.ImagePromoteCommand;
import io.github.lunasaw.voglander.client.domain.image.ImageStageCommand;
import io.github.lunasaw.voglander.client.domain.image.StagedImage;
import io.github.lunasaw.voglander.client.domain.image.StoredImage;

/** Stable storage port used by image-domain business services. */
public interface ImageStorageService {

    /** Lightweight readiness probe; providers may override with a filesystem/object-store check. */
    default boolean isHealthy() { return true; }

    StagedImage stage(ImageStageCommand command, InputStream content) throws IOException;

    InputStream openStaged(String stagingKey) throws IOException;

    StoredImage promote(ImagePromoteCommand command) throws IOException;

    ImageContent open(String storageKey) throws IOException;

    boolean delete(String storageKey) throws IOException;

    boolean exists(String storageKey) throws IOException;

    void discardStaged(String stagingKey) throws IOException;

    /** Optional provider inventory used by report-only reconciliation. */
    default Set<String> listFinalKeys() throws IOException { return Collections.emptySet(); }

    /** Optional provider staging sweep; providers without a local staging area may no-op. */
    default int sweepStaging(Duration ttl) throws IOException { return 0; }
}
