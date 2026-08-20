package conc;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Memoizer<K, V> {
    private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();
    private final Function<K, V> fn;

    public Memoizer(Function<K, V> fn) {
        this.fn = fn;
    }

    public V get(K key) {
        return cache.computeIfAbsent(key, fn);
    }
}
