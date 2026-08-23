package iv;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache TTL (expire-after-write), lazy eviction lúc get.
 * Production: scheduled cleaner + size cap (LRU+TTL). Clock inject để test.
 */
public class TtlCache<K, V> {
    public interface Clock {
        long nowMs();
    }

    private static final class Entry<V> {
        final V value;
        final long expireAtMs;

        Entry(V value, long expireAtMs) {
            this.value = value;
            this.expireAtMs = expireAtMs;
        }
    }

    private final ConcurrentHashMap<K, Entry<V>> map = new ConcurrentHashMap<K, Entry<V>>();
    private final long ttlMs;
    private final Clock clock;

    public TtlCache(long ttlMs) {
        this(ttlMs, new Clock() {
            @Override
            public long nowMs() {
                return System.currentTimeMillis();
            }
        });
    }

    public TtlCache(long ttlMs, Clock clock) {
        if (ttlMs <= 0) {
            throw new IllegalArgumentException("ttlMs");
        }
        this.ttlMs = ttlMs;
        this.clock = clock;
    }

    public void put(K key, V value) {
        map.put(key, new Entry<V>(value, clock.nowMs() + ttlMs));
    }

    public V get(K key) {
        Entry<V> e = map.get(key);
        if (e == null) {
            return null;
        }
        if (clock.nowMs() >= e.expireAtMs) {
            map.remove(key, e);
            return null;
        }
        return e.value;
    }

    public int size() {
        return map.size();
    }
}
