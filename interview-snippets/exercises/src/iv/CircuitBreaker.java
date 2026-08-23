package iv;

import java.util.function.Supplier;

/**
 * Circuit breaker 3 trạng thái: CLOSED → OPEN (đủ lỗi) → HALF_OPEN (hết cooldown).
 * HALF_OPEN thành công → CLOSED; thất bại → OPEN lại.
 */
public class CircuitBreaker {
    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    public static final class OpenCircuitException extends RuntimeException {
        public OpenCircuitException() {
            super("circuit open");
        }
    }

    public interface Clock {
        long nowMs();
    }

    private final int failureThreshold;
    private final long resetMs;
    private final Clock clock;
    private State state = State.CLOSED;
    private int failures;
    private long openedAtMs;

    public CircuitBreaker(int failureThreshold, long resetMs) {
        this(failureThreshold, resetMs, new Clock() {
            @Override
            public long nowMs() {
                return System.currentTimeMillis();
            }
        });
    }

    public CircuitBreaker(int failureThreshold, long resetMs, Clock clock) {
        if (failureThreshold <= 0 || resetMs <= 0) {
            throw new IllegalArgumentException("threshold/reset");
        }
        this.failureThreshold = failureThreshold;
        this.resetMs = resetMs;
        this.clock = clock;
    }

    public synchronized <T> T call(Supplier<T> action) {
        if (state == State.OPEN) {
            if (clock.nowMs() - openedAtMs < resetMs) {
                throw new OpenCircuitException();
            }
            state = State.HALF_OPEN;
        }
        try {
            T v = action.get();
            onSuccess();
            return v;
        } catch (RuntimeException e) {
            onFailure();
            throw e;
        }
    }

    public synchronized State state() {
        return state;
    }

    private void onSuccess() {
        failures = 0;
        state = State.CLOSED;
    }

    private void onFailure() {
        failures++;
        if (state == State.HALF_OPEN || failures >= failureThreshold) {
            state = State.OPEN;
            openedAtMs = clock.nowMs();
        }
    }
}
