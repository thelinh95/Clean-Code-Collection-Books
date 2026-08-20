package conc;

import java.util.concurrent.Semaphore;

public class RateLimiter {
    private final Semaphore sem;

    public RateLimiter(int permits) {
        if (permits <= 0) {
            throw new IllegalArgumentException("permits");
        }
        this.sem = new Semaphore(permits);
    }

    public void run(Runnable task) throws InterruptedException {
        sem.acquire();
        try {
            task.run();
        } finally {
            sem.release();
        }
    }
}
