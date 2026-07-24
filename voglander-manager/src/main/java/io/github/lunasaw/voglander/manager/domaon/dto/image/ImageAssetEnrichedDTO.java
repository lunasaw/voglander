package io.github.lunasaw.voglander.manager.domaon.dto.image;

import java.io.Serializable;

import lombok.Data;

/** Manager projection carrying an asset and its optional immutable source. */
@Data
public class ImageAssetEnrichedDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private ImageAssetDTO asset;
    private ImageAssetSourceDTO source;
}
