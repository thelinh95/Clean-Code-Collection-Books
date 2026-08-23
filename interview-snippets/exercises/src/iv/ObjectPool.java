package iv;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Object / connection pool: tạo sẵn N instance, borrow/release.
 * Production: validate-on-borrow, idle evict, max-wait.
 */
public class ObjectPool<T> {
    private final BlockingQueue<T> free;

    public ObjectPool(int size, Supplier<T> factory) {
        if (size <= 0) {
            throw new IllegalArgumentException("size");
        }
        this.free = new ArrayBlockingQueue<T>(size);
        for (int i = 0; i < size; i++) {
            free.add(factory.get());
        }
    }

    public T borrow(long timeout, TimeUnit unit) throws InterruptedException {
        T v = free.poll(timeout, unit);
        if (v == null) {
            throw new IllegalStateException("pool exhausted");
        }
        return v;
    }

    public void release(T obj) {
        if (!free.offer(obj)) {
            throw new IllegalStateException("release overflow");
        }
    }

    public int available() {
        return free.size();
    }
}
