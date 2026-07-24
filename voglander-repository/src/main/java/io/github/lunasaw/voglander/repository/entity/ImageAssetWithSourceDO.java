package io.github.lunasaw.voglander.repository.entity;

import java.io.Serializable;

import lombok.Data;

/** Read projection for one image asset and its optional immutable source. */
@Data
public class ImageAssetWithSourceDO implements Serializable {
    private static final long serialVersionUID = 1L;
    private ImageAssetDO asset;
    private ImageAssetSourceDO source;
}
