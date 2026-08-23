package iv;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU — bản viết trong ~2 phút. {@code accessOrder=true} đưa entry vừa
 * get/put lên "youngest"; {@code removeEldestEntry} đuổi oldest khi đầy.
 * <p>
 * Không thread-safe. Interview follow-up: bọc {@code Collections.synchronizedMap}
 * hoặc tự lock; ConcurrentHashMap không giữ thứ tự truy cập.
 */
public class LruLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    public LruLinkedHashMap(int capacity) {
        super(capacity, 0.75f, true);
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity");
        }
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
