package io.github.lunasaw.voglander.common.enums.image;

import java.util.EnumSet;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/** Image asset lifecycle states. */
public enum ImageAssetStatusEnum {
    AVAILABLE,
    DELETING,
    DELETED,
    DELETE_FAILED;

    private static final Map<ImageAssetStatusEnum, Set<ImageAssetStatusEnum>> ALLOWED = new EnumMap<>(ImageAssetStatusEnum.class);

    static {
        ALLOWED.put(AVAILABLE, EnumSet.of(DELETING));
        ALLOWED.put(DELETING, EnumSet.of(DELETED, DELETE_FAILED));
        ALLOWED.put(DELETE_FAILED, EnumSet.of(DELETING));
        ALLOWED.put(DELETED, EnumSet.noneOf(ImageAssetStatusEnum.class));
    }

    public boolean canTransitionTo(ImageAssetStatusEnum target) {
        return target != null && (this == target || ALLOWED.get(this).contains(target));
    }
}
