package io.github.lunasaw.voglander.manager.domaon.dto.image;

/** Database-authoritative result of an asset/source insert attempt. */
public final class ImageAssetCreateResultDTO {

    private final boolean created;
    private final ImageAssetDTO acceptedAsset;
    private final ImageAssetSourceDTO acceptedSource;

    public ImageAssetCreateResultDTO(boolean created, ImageAssetDTO acceptedAsset,
        ImageAssetSourceDTO acceptedSource) {
        if (acceptedAsset == null) {
            throw new IllegalArgumentException("acceptedAsset must not be null");
        }
        if (acceptedSource == null) {
            throw new IllegalArgumentException("acceptedSource must not be null");
        }
        this.created = created;
        this.acceptedAsset = acceptedAsset;
        this.acceptedSource = acceptedSource;
    }

    public boolean isCreated() {
        return created;
    }

    public ImageAssetDTO getAcceptedAsset() {
        return acceptedAsset;
    }

    public ImageAssetSourceDTO getAcceptedSource() {
        return acceptedSource;
    }
}
