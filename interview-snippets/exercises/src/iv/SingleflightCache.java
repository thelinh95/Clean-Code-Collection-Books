package iv;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Chống cache stampede / thundering herd: nhiều caller miss cùng key thì
 * chỉ <em>một</em> load chạy, còn lại share cùng {@link CompletableFuture}.
 * <p>
 * Pattern production: singleflight (Go) / request coalescing.
 */
public class SingleflightCache<K, V> {
    private final ConcurrentHashMap<K, V> values = new ConcurrentHashMap<K, V>();
    private final ConcurrentHashMap<K, CompletableFuture<V>> inflight =
            new ConcurrentHashMap<K, CompletableFuture<V>>();
    private final Function<K, V> loader;
    private final Executor executor;

    public SingleflightCache(Function<K, V> loader, Executor executor) {
        this.loader = loader;
        this.executor = executor;
    }

    public V get(K key) {
        return getAsync(key).join();
    }

    public CompletableFuture<V> getAsync(K key) {
        V hit = values.get(key);
        if (hit != null) {
            return CompletableFuture.completedFuture(hit);
        }
        CompletableFuture<V> created = new CompletableFuture<V>();
        CompletableFuture<V> existing = inflight.putIfAbsent(key, created);
        if (existing != null) {
            return existing;
        }
        CompletableFuture.supplyAsync(new java.util.function.Supplier<V>() {
            @Override
            public V get() {
                return loader.apply(key);
            }
        }, executor).whenComplete(new java.util.function.BiConsumer<V, Throwable>() {
            @Override
            public void accept(V value, Throwable ex) {
                try {
                    if (ex == null) {
                        values.put(key, value);
                        created.complete(value);
                    } else {
                        created.completeExceptionally(unwrap(ex));
                    }
                } finally {
                    inflight.remove(key, created);
                }
            }
        });
        return created;
    }

    public int size() {
        return values.size();
    }

    static Throwable unwrap(Throwable ex) {
        if (ex instanceof java.util.concurrent.CompletionException && ex.getCause() != null) {
            return ex.getCause();
        }
        return ex;
    }
}
