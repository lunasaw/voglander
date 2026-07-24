package io.github.lunasaw.voglander.service.image;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** JVM-local access-order cache bounded by entries, bytes and TTL. */
public class ThumbnailMemoryCache {

    private final long maxBytes;
    private final int maxEntries;
    private final long ttlMillis;
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<String, Entry>(16, 0.75f, true);
    private long currentBytes;

    public ThumbnailMemoryCache(long maxBytes, int maxEntries, long ttlSeconds) {
        if (maxBytes <= 0 || maxEntries <= 0 || ttlSeconds <= 0) {
            throw new IllegalArgumentException("thumbnail cache limits must be positive");
        }
        this.maxBytes = maxBytes;
        this.maxEntries = maxEntries;
        this.ttlMillis = ttlSeconds * 1000L;
    }

    public synchronized byte[] get(String key) {
        Entry entry = entries.get(key);
        if (entry == null) return null;
        if (entry.expiresAt <= System.currentTimeMillis()) {
            remove(key, entry);
            return null;
        }
        return entry.content.clone();
    }

    public synchronized void put(String key, byte[] content) {
        if (key == null || content == null || content.length > maxBytes) return;
        Entry previous = entries.remove(key);
        if (previous != null) currentBytes -= previous.content.length;
        byte[] copy = content.clone();
        entries.put(key, new Entry(copy, System.currentTimeMillis() + ttlMillis));
        currentBytes += copy.length;
        evict();
    }

    private void evict() {
        Iterator<Map.Entry<String, Entry>> iterator = entries.entrySet().iterator();
        while ((entries.size() > maxEntries || currentBytes > maxBytes) && iterator.hasNext()) {
            Map.Entry<String, Entry> eldest = iterator.next();
            currentBytes -= eldest.getValue().content.length;
            iterator.remove();
        }
    }

    private void remove(String key, Entry entry) {
        entries.remove(key);
        currentBytes -= entry.content.length;
    }

    private static final class Entry {
        private final byte[] content;
        private final long expiresAt;

        private Entry(byte[] content, long expiresAt) {
            this.content = content;
            this.expiresAt = expiresAt;
        }
    }
}
