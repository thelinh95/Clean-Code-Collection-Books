package conc;

import java.util.function.Function;

/**
 * Cache: get(key) tính function tối đa một lần cho mỗi key, kể cả khi nhiều thread cùng gọi.
 */
public class Memoizer<K, V> {
    public Memoizer(Function<K, V> fn) {
        throw new UnsupportedOperationException("TODO Memoizer ctor");
    }

    public V get(K key) {
        throw new UnsupportedOperationException("TODO Memoizer.get");
    }
}
