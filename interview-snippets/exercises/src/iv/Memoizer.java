package iv;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Cache-aside tối giản: {@code computeIfAbsent} atomic theo key.
 * Loader phải thuần (không side-effect nặng / không gọi lại memoizer cùng key).
 * Hạn chế: loader chậm + nhiều thread miss cùng lúc vẫn chỉ chạy 1 lần
 * (CHM compute lock per bin) — nhưng giữ lock trong lúc load. Xem {@link SingleflightCache}.
 */
public class Memoizer<K, V> {
    private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<K, V>();
    private final Function<K, V> loader;

    public Memoizer(Function<K, V> loader) {
        this.loader = loader;
    }

    public V get(K key) {
        return cache.computeIfAbsent(key, loader);
    }

    public int size() {
        return cache.size();
    }
}
