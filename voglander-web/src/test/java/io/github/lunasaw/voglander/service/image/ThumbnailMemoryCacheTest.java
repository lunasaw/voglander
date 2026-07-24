package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ThumbnailMemoryCacheTest {

    @Test
    void cacheEvictsLeastRecentlyUsedEntriesByCountAndTotalBytes() {
        ThumbnailMemoryCache cache = new ThumbnailMemoryCache(5, 2, 300);
        cache.put("a", new byte[] {1, 2});
        cache.put("b", new byte[] {3, 4});
        assertArrayEquals(new byte[] {1, 2}, cache.get("a"));
        cache.put("c", new byte[] {5, 6});

        assertNull(cache.get("b"));
        assertArrayEquals(new byte[] {1, 2}, cache.get("a"));
        assertArrayEquals(new byte[] {5, 6}, cache.get("c"));
    }

    @Test
    void oversizedSingleEntryIsNeverCached() {
        ThumbnailMemoryCache cache = new ThumbnailMemoryCache(2, 2, 300);
        cache.put("large", new byte[] {1, 2, 3});
        assertNull(cache.get("large"));
    }
}
