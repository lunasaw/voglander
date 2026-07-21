package io.github.lunasaw.voglander.client.service.image;

import java.io.IOException;

import io.github.lunasaw.voglander.client.domain.image.MediaSnapshotCommand;
import io.github.lunasaw.voglander.client.domain.image.SnapshotContent;

/** Stable media snapshot port independent of ZLM request and response types. */
public interface MediaSnapshotAdapter {

    SnapshotContent capture(MediaSnapshotCommand command) throws IOException;
}
