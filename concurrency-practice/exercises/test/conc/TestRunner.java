package conc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class TestRunner {
    private static int failed;

    public static void main(String[] args) throws Exception {
        check("SafeCounter", TestRunner::testSafeCounter);
        check("BoundedBuffer", TestRunner::testBoundedBuffer);
        check("OrderedPrinter", TestRunner::testOrderedPrinter);
        check("Bank", TestRunner::testBank);
        check("Memoizer", TestRunner::testMemoizer);
        check("DiningPhilosophers", TestRunner::testDining);
        check("RateLimiter", TestRunner::testRateLimiter);
        check("LazySingleton", TestRunner::testSingleton);
        if (failed > 0) {
            System.out.println("\nFAILED: " + failed + " test(s)");
            System.exit(1);
        }
        System.out.println("\nAll tests passed.");
    }

    private static void check(String name, ThrowingRunnable r) {
        try {
            r.run();
            System.out.println("OK  " + name);
        } catch (UnsupportedOperationException e) {
            failed++;
            System.out.println("TODO " + name + " — " + e.getMessage());
        } catch (Throwable e) {
            failed++;
            System.out.println("FAIL " + name + " — " + e);
            e.printStackTrace(System.out);
        }
    }

    private static void testSafeCounter() throws Exception {
        SafeCounter smoke = new SafeCounter();
        smoke.inc();
        eq(1, smoke.get());
        SafeCounter c = new SafeCounter();
        int threads = 8;
        int per = 20_000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.execute(() -> {
                for (int j = 0; j < per; j++) {
                    c.inc();
                }
                done.countDown();
            });
        }
        if (!done.await(15, TimeUnit.SECONDS)) {
            throw new AssertionError("timeout");
        }
        pool.shutdownNow();
        eq(threads * per, c.get());
    }

    private static void testBoundedBuffer() throws Exception {
        int cap = 3;
        BoundedBuffer<Integer> buf = new BoundedBuffer<>(cap);
        int producers = 4;
        int per = 500;
        ConcurrentLinkedQueue<Integer> got = new ConcurrentLinkedQueue<>();
        ExecutorService pool = Executors.newCachedThreadPool();
        for (int p = 0; p < producers; p++) {
            int id = p;
            pool.execute(() -> {
                try {
                    for (int i = 0; i < per; i++) {
                        buf.put(id * per + i);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        int total = producers * per;
        CountDownLatch consumed = new CountDownLatch(total);
        for (int c = 0; c < 3; c++) {
            pool.execute(() -> {
                try {
                    while (consumed.getCount() > 0) {
                        got.add(buf.take());
                        consumed.countDown();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        if (!consumed.await(20, TimeUnit.SECONDS)) {
            throw new AssertionError("buffer timeout, got=" + got.size());
        }
        pool.shutdownNow();
        eq(total, got.size());
        eq(total, got.stream().distinct().count());
    }

    private static void testOrderedPrinter() throws Exception {
        OrderedPrinter probe = new OrderedPrinter();
        probe.first(() -> {});
        probe.second(() -> {});
        probe.third(() -> {});
        for (int round = 0; round < 40; round++) {
            OrderedPrinter p = new OrderedPrinter();
            StringBuilder sb = new StringBuilder();
            List<ThrowingRunnable> jobs = new ArrayList<>();
            jobs.add(() -> p.first(() -> sb.append("first")));
            jobs.add(() -> p.second(() -> sb.append("second")));
            jobs.add(() -> p.third(() -> sb.append("third")));
            Collections.shuffle(jobs);
            ExecutorService pool = Executors.newFixedThreadPool(3);
            for (ThrowingRunnable job : jobs) {
                pool.execute(() -> {
                    try {
                        job.run();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            pool.shutdown();
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                throw new AssertionError("printer timeout");
            }
            eq("firstsecondthird", sb.toString());
        }
    }

    private static void testBank() throws Exception {
        Bank bank = new Bank();
        String[] ids = {"a", "b", "c", "d", "e"};
        long each = 1_000;
        for (String id : ids) {
            bank.open(id, each);
        }
        long expected = each * ids.length;
        int workers = 8;
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(workers);
        for (int w = 0; w < workers; w++) {
            pool.execute(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 2_000; i++) {
                        String from = ids[i % ids.length];
                        String to = ids[(i * 3 + 1) % ids.length];
                        bank.transfer(from, to, 1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        if (!done.await(20, TimeUnit.SECONDS)) {
            pool.shutdownNow();
            throw new AssertionError("deadlock or hang in Bank.transfer");
        }
        pool.shutdownNow();
        eq(expected, bank.total());
    }

    private static void testMemoizer() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Memoizer<Integer, Integer> m = new Memoizer<>(k -> {
            calls.incrementAndGet();
            sleep(80);
            return k * 10;
        });
        int n = 12;
        CyclicBarrier bar = new CyclicBarrier(n);
        ExecutorService pool = Executors.newFixedThreadPool(n);
        AtomicReference<Throwable> err = new AtomicReference<>();
        for (int i = 0; i < n; i++) {
            pool.execute(() -> {
                try {
                    bar.await();
                    eq(Integer.valueOf(70), m.get(7));
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                } catch (Throwable t) {
                    err.set(t);
                }
            });
        }
        pool.shutdown();
        if (!pool.awaitTermination(15, TimeUnit.SECONDS)) {
            throw new AssertionError("memoizer timeout");
        }
        if (err.get() != null) {
            throw new AssertionError(err.get());
        }
        eq(1, calls.get());
        eq(Integer.valueOf(70), m.get(7));
        eq(1, calls.get());
        eq(Integer.valueOf(20), m.get(2));
        eq(2, calls.get());
    }

    private static void testDining() throws Exception {
        int n = 5;
        int meals = 30;
        DiningPhilosophers table = new DiningPhilosophers(n);
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch done = new CountDownLatch(n);
        AtomicInteger bites = new AtomicInteger();
        for (int i = 0; i < n; i++) {
            int id = i;
            pool.execute(() -> {
                try {
                    for (int m = 0; m < meals; m++) {
                        table.eat(id, bites::incrementAndGet);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        if (!done.await(20, TimeUnit.SECONDS)) {
            pool.shutdownNow();
            throw new AssertionError("dining deadlock, bites=" + bites.get());
        }
        pool.shutdownNow();
        eq(n * meals, bites.get());
    }

    private static void testRateLimiter() throws Exception {
        int permits = 3;
        RateLimiter lim = new RateLimiter(permits);
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger max = new AtomicInteger();
        int tasks = 20;
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch done = new CountDownLatch(tasks);
        for (int i = 0; i < tasks; i++) {
            pool.execute(() -> {
                try {
                    lim.run(() -> {
                        int now = inFlight.incrementAndGet();
                        max.accumulateAndGet(now, Math::max);
                        sleep(30);
                        inFlight.decrementAndGet();
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        if (!done.await(15, TimeUnit.SECONDS)) {
            throw new AssertionError("rate limiter timeout");
        }
        pool.shutdownNow();
        if (max.get() > permits) {
            throw new AssertionError("max concurrent " + max.get() + " > " + permits);
        }
        if (max.get() < 1) {
            throw new AssertionError("nothing ran");
        }
    }

    private static void testSingleton() throws Exception {
        LazySingleton.Expensive probe = LazySingleton.get();
        if (probe == null) {
            throw new AssertionError("null instance");
        }
        int n = 16;
        CyclicBarrier bar = new CyclicBarrier(n);
        ExecutorService pool = Executors.newFixedThreadPool(n);
        ConcurrentLinkedQueue<LazySingleton.Expensive> seen = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            pool.execute(() -> {
                try {
                    bar.await();
                    seen.add(LazySingleton.get());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        pool.shutdown();
        if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
            throw new AssertionError("singleton timeout");
        }
        eq(1, LazySingleton.Expensive.CONSTRUCTED.get());
        for (LazySingleton.Expensive e : seen) {
            if (e != probe) {
                throw new AssertionError("multiple instances");
            }
        }
        eq(n, seen.size());
    }

    private static void eq(long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError("expected " + expected + " but was " + actual);
        }
    }

    private static void eq(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but was " + actual);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
