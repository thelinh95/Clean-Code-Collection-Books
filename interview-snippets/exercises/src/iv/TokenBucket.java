package iv;

/**
 * Token bucket: lưu burst {@code capacity}, nạp {@code refillPerSecond} token/s.
 * Interview hay hỏi khác sliding window: bucket cho burst, window cứng hơn.
 */
public class TokenBucket {
    public interface Clock {
        long nowNanos();
    }

    private final double capacity;
    private final double refillPerSecond;
    private final Clock clock;
    private double tokens;
    private long lastNanos;

    public TokenBucket(double capacity, double refillPerSecond) {
        this(capacity, refillPerSecond, new Clock() {
            @Override
            public long nowNanos() {
                return System.nanoTime();
            }
        });
    }

    public TokenBucket(double capacity, double refillPerSecond, Clock clock) {
        if (capacity <= 0 || refillPerSecond <= 0) {
            throw new IllegalArgumentException("capacity/refill");
        }
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.clock = clock;
        this.tokens = capacity;
        this.lastNanos = clock.nowNanos();
    }

    public synchronized boolean tryAcquire() {
        return tryAcquire(1.0);
    }

    public synchronized boolean tryAcquire(double n) {
        refill();
        if (tokens >= n) {
            tokens -= n;
            return true;
        }
        return false;
    }

    public synchronized double tokens() {
        refill();
        return tokens;
    }

    private void refill() {
        long now = clock.nowNanos();
        double elapsedSec = (now - lastNanos) / 1_000_000_000.0;
        tokens = Math.min(capacity, tokens + elapsedSec * refillPerSecond);
        lastNanos = now;
    }
}
