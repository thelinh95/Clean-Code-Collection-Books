package iv;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread pool tối giản: N worker + hàng đợi unbounded.
 * Follow-up: bounded queue + CallerRuns / Abort; {@code shutdown} vs {@code shutdownNow}.
 */
public class SimpleThreadPool {
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
    private final List<Thread> workers = new ArrayList<Thread>();
    private final AtomicBoolean running = new AtomicBoolean(true);

    public SimpleThreadPool(int nThreads) {
        if (nThreads <= 0) {
            throw new IllegalArgumentException("nThreads");
        }
        for (int i = 0; i < nThreads; i++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    loop();
                }
            }, "pool-" + i);
            t.start();
            workers.add(t);
        }
    }

    public void execute(Runnable task) {
        if (!running.get()) {
            throw new IllegalStateException("shutdown");
        }
        queue.add(task);
    }

    public void shutdown() {
        running.set(false);
        for (Thread t : workers) {
            t.interrupt();
        }
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        for (Thread t : workers) {
            long left = deadline - System.nanoTime();
            if (left <= 0) {
                return false;
            }
            t.join(TimeUnit.NANOSECONDS.toMillis(left), (int) (left % 1_000_000L));
        }
        return true;
    }

    private void loop() {
        while (running.get() || !queue.isEmpty()) {
            try {
                Runnable task = queue.poll(50, TimeUnit.MILLISECONDS);
                if (task != null) {
                    task.run();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (!running.get()) {
                    break;
                }
            }
        }
    }
}
