package iv;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Idempotency key: request trùng key trả cùng kết quả, không chạy lại side-effect.
 * Payment / webhook hay hỏi.
 */
public class IdempotencyStore<V> {
    private final ConcurrentHashMap<String, V> done = new ConcurrentHashMap<String, V>();

    public V executeOnce(String key, Callable<V> action) throws Exception {
        V cached = done.get(key);
        if (cached != null) {
            return cached;
        }
        V computed = action.call();
        V raced = done.putIfAbsent(key, computed);
        return raced != null ? raced : computed;
    }

    public V get(String key) {
        return done.get(key);
    }
}
