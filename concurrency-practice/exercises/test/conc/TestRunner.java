package conc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;
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
        check("FooBar", TestRunner::testFooBar);
        check("ZeroEvenOdd", TestRunner::testZeroEvenOdd);
        check("H2O", TestRunner::testH2O);
        check("FizzBuzzMT", TestRunner::testFizzBuzz);
        check("TrafficLight", TestRunner::testTrafficLight);
        check("WebCrawler", TestRunner::testWebCrawler);
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

    private static void testFooBar() throws Exception {
        FooBar fb = new FooBar(4);
        StringBuffer out = new StringBuffer();
        AtomicReference<Throwable> err = new AtomicReference<>();
        Thread t1 = new Thread(() -> runCatch(err, () -> fb.foo(() -> out.append("foo"))));
        Thread t2 = new Thread(() -> runCatch(err, () -> fb.bar(() -> out.append("bar"))));
        t2.start();
        t1.start();
        t1.join(8000);
        t2.join(8000);
        rethrow(err);
        eq("foobarfoobarfoobarfoobar", out.toString());
    }

    private static void testZeroEvenOdd() throws Exception {
        int n = 5;
        ZeroEvenOdd z = new ZeroEvenOdd(n);
        StringBuffer out = new StringBuffer();
        IntConsumer print = out::append;
        AtomicReference<Throwable> err = new AtomicReference<>();
        Thread a = new Thread(() -> runCatch(err, () -> z.zero(print)));
        Thread b = new Thread(() -> runCatch(err, () -> z.even(print)));
        Thread c = new Thread(() -> runCatch(err, () -> z.odd(print)));
        b.start();
        c.start();
        a.start();
        a.join(8000);
        b.join(8000);
        c.join(8000);
        rethrow(err);
        eq("0102030405", out.toString());
    }

    private static void testH2O() throws Exception {
        H2O h2o = new H2O();
        StringBuffer out = new StringBuffer();
        int molecules = 6;
        AtomicReference<Throwable> err = new AtomicReference<>();
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < molecules * 2; i++) {
            ts.add(new Thread(() -> runCatch(err, () -> h2o.hydrogen(() -> out.append('H')))));
        }
        for (int i = 0; i < molecules; i++) {
            ts.add(new Thread(() -> runCatch(err, () -> h2o.oxygen(() -> out.append('O')))));
        }
        Collections.shuffle(ts);
        ts.forEach(Thread::start);
        for (Thread t : ts) {
            t.join(8000);
        }
        rethrow(err);
        String s = out.toString();
        if (s.length() != molecules * 3) {
            throw new AssertionError("length " + s.length() + " " + s);
        }
        for (int i = 0; i < s.length(); i += 3) {
            String m = s.substring(i, i + 3);
            long h = m.chars().filter(ch -> ch == 'H').count();
            long o = m.chars().filter(ch -> ch == 'O').count();
            if (h != 2 || o != 1) {
                throw new AssertionError("bad molecule " + m + " in " + s);
            }
        }
    }

    private static void testFizzBuzz() throws Exception {
        int n = 15;
        FizzBuzzMT fb = new FizzBuzzMT(n);
        StringBuffer out = new StringBuffer();
        AtomicReference<Throwable> err = new AtomicReference<>();
        Thread t1 = new Thread(() -> runCatch(err, () -> fb.fizz(() -> out.append("fizz,"))));
        Thread t2 = new Thread(() -> runCatch(err, () -> fb.buzz(() -> out.append("buzz,"))));
        Thread t3 = new Thread(() -> runCatch(err, () -> fb.fizzbuzz(() -> out.append("fizzbuzz,"))));
        Thread t4 = new Thread(() -> runCatch(err, () -> fb.number(x -> out.append(x).append(','))));
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t1.join(8000);
        t2.join(8000);
        t3.join(8000);
        t4.join(8000);
        rethrow(err);
        eq("1,2,fizz,4,buzz,fizz,7,8,fizz,buzz,11,fizz,13,14,fizzbuzz,", out.toString());
    }

    private static void testTrafficLight() throws Exception {
        TrafficLight light = new TrafficLight();
        light.carArrived(1, 1, 1, () -> {}, () -> {});
        AtomicInteger road1 = new AtomicInteger();
        AtomicInteger road2 = new AtomicInteger();
        AtomicInteger overlap = new AtomicInteger();
        AtomicReference<Throwable> err = new AtomicReference<>();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch done = new CountDownLatch(40);
        for (int i = 0; i < 40; i++) {
            int car = i;
            int road = (i % 2) + 1;
            pool.execute(() -> {
                try {
                    light.carArrived(car, road, road, () -> {}, () -> {
                        AtomicInteger mine = road == 1 ? road1 : road2;
                        AtomicInteger other = road == 1 ? road2 : road1;
                        mine.incrementAndGet();
                        if (other.get() > 0) {
                            overlap.incrementAndGet();
                        }
                        sleep(2);
                        mine.decrementAndGet();
                    });
                } catch (UnsupportedOperationException e) {
                    err.compareAndSet(null, e);
                } finally {
                    done.countDown();
                }
            });
        }
        if (!done.await(10, TimeUnit.SECONDS)) {
            pool.shutdownNow();
            throw new AssertionError("traffic timeout");
        }
        pool.shutdownNow();
        rethrow(err);
        if (overlap.get() > 0) {
            throw new AssertionError("two roads crossed at once");
        }
    }

    private static void testWebCrawler() throws Exception {
        Map<String, List<String>> graph = Map.of(
                "http://a.com/", List.of("http://a.com/x", "http://b.com/y"),
                "http://a.com/x", List.of("http://a.com/", "http://a.com/z"),
                "http://a.com/z", List.of(),
                "http://b.com/y", List.of("http://a.com/z"));
        WebCrawler.HtmlParser parser = url -> graph.getOrDefault(url, List.of());
        List<String> got = new WebCrawler().crawl("http://a.com/", parser);
        Set<String> set = new HashSet<>(got);
        eq(Set.of("http://a.com/", "http://a.com/x", "http://a.com/z"), set);
        eq(3, set.size());
    }

    private static void runCatch(AtomicReference<Throwable> err, ThrowingRunnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            err.compareAndSet(null, t);
        }
    }

    private static void rethrow(AtomicReference<Throwable> err) throws Exception {
        Throwable t = err.get();
        if (t == null) {
            return;
        }
        if (t instanceof UnsupportedOperationException u) {
            throw u;
        }
        if (t instanceof RuntimeException r) {
            throw r;
        }
        if (t instanceof Error e) {
            throw e;
        }
        if (t instanceof Exception e) {
            throw e;
        }
        throw new RuntimeException(t);
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
