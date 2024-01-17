package io.github.lunasaw.voglander.repository.tair;

import io.github.lunasaw.voglander.common.constant.CacheConstants;
import lombok.Data;

@Data
public class TairContext {

    private boolean       localCacheQuery   = true;

    private boolean       redisCacheQuery   = true;

    private int           redisCacheSeconds = CacheConstants.DEFAULT_REDIS_CACHE_SECONDS;

    private KeyPrefixEnum keyPrefixEnum;

    public static TairContext defaultContext(KeyPrefixEnum keyPrefixEnum) {
        TairContext context = new TairContext();
        context.setKeyPrefixEnum(keyPrefixEnum);
        return context;
    }
}
