package iv;

import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Retry + exponential backoff + full jitter.
 * delay = random(0, min(cap, base * 2^attempt))
 */
public final class Retry {
    private Retry() {
    }

    public interface Sleeper {
        void sleep(long ms) throws InterruptedException;
    }

    public static <T> T call(
            Callable<T> action,
            int maxAttempts,
            long baseBackoffMs,
            long capMs) throws Exception {
        return call(action, maxAttempts, baseBackoffMs, capMs, new Sleeper() {
            @Override
            public void sleep(long ms) throws InterruptedException {
                Thread.sleep(ms);
            }
        }, new Random());
    }

    public static <T> T call(
            Callable<T> action,
            int maxAttempts,
            long baseBackoffMs,
            long capMs,
            Sleeper sleeper,
            Random random) throws Exception {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts");
        }
        Exception last = null;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                return action.call();
            } catch (Exception e) {
                last = e;
                if (attempt == maxAttempts - 1) {
                    break;
                }
                long exp = baseBackoffMs * (1L << Math.min(attempt, 16));
                long window = Math.min(capMs, exp);
                long sleepMs = window <= 0 ? 0 : (long) (random.nextDouble() * window);
                sleeper.sleep(sleepMs);
            }
        }
        throw last;
    }
}
