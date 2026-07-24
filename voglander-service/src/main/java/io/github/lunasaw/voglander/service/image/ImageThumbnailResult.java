package io.github.lunasaw.voglander.service.image;

import io.github.lunasaw.voglander.common.enums.image.ThumbnailProfile;

/** Immutable result of one private thumbnail request. */
public final class ImageThumbnailResult {

    private final byte[] content;
    private final String etag;
    private final ThumbnailProfile profile;
    private final boolean notModified;

    private ImageThumbnailResult(byte[] content, String etag, ThumbnailProfile profile, boolean notModified) {
        this.content = content == null ? null : content.clone();
        this.etag = etag;
        this.profile = profile;
        this.notModified = notModified;
    }

    public static ImageThumbnailResult content(byte[] content, String etag, ThumbnailProfile profile) {
        return new ImageThumbnailResult(content, etag, profile, false);
    }

    public static ImageThumbnailResult notModified(String etag, ThumbnailProfile profile) {
        return new ImageThumbnailResult(null, etag, profile, true);
    }

    public byte[] getContent() { return content == null ? null : content.clone(); }
    public String getEtag() { return etag; }
    public ThumbnailProfile getProfile() { return profile; }
    public boolean isNotModified() { return notModified; }
}
