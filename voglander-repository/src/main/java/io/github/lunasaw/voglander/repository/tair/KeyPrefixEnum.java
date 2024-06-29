package io.github.lunasaw.voglander.repository.tair;

import lombok.Getter;

/**
 * 缓存key前缀枚举
 * 注意！！！ maxSize大小不能无限扩大 太多会导致内存不足而OOM
 */
public enum KeyPrefixEnum {
    ITEM_POST_FREE("item_postage_free", 3000),
    SUB_ORDER_DETAIL("sub_order_detail", 3000),
    SUB_ORDER_RANGE("sub_order_range", 3000),
    ACTIVITY_RESOURCE("activity_resource", 3000),
    ORDER_DETAIL("order_detail", 3000),
    ORDER_RANGE("order_range", 3000),
    FREE_POSTAGE_DETAIL("free_postage_detail", 3000),
    ;

    @Getter
    private final String prefix;
    @Getter
    private final int    maxSize;

    KeyPrefixEnum(String prefix, int maxSize) {
        this.prefix = prefix;
        this.maxSize = maxSize;
    }
}
