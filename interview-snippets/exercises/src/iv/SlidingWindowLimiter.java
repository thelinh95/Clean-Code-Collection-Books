package iv;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Sliding-window log: mỗi key giữ timestamp trong cửa sổ.
 * O(n) theo số request trong window — đủ để viết tay. Production: Redis ZSET / counter.
 */
public class SlidingWindowLimiter {
    public interface Clock {
        long nowMs();
    }

    private final int limit;
    private final long windowMs;
    private final Clock clock;
    private final Map<String, Deque<Long>> hits = new HashMap<String, Deque<Long>>();

    public SlidingWindowLimiter(int limit, long windowMs) {
        this(limit, windowMs, new Clock() {
            @Override
            public long nowMs() {
                return System.currentTimeMillis();
            }
        });
    }

    public SlidingWindowLimiter(int limit, long windowMs, Clock clock) {
        if (limit <= 0 || windowMs <= 0) {
            throw new IllegalArgumentException("limit/window");
        }
        this.limit = limit;
        this.windowMs = windowMs;
        this.clock = clock;
    }

    public synchronized boolean allow(String key) {
        long now = clock.nowMs();
        Deque<Long> q = hits.get(key);
        if (q == null) {
            q = new ArrayDeque<Long>();
            hits.put(key, q);
        }
        while (!q.isEmpty() && now - q.peekFirst() >= windowMs) {
            q.removeFirst();
        }
        if (q.size() >= limit) {
            return false;
        }
        q.addLast(now);
        return true;
    }
}
