package iv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class TestRunner {
    private static int failed;

    public static void main(String[] args) throws Exception {
        check("LruLinkedHashMap", TestRunner::testLruLinked);
        check("LruCache", TestRunner::testLruDll);
        check("TtlCache", TestRunner::testTtl);
        check("Memoizer", TestRunner::testMemoizer);
        check("SingleflightCache", TestRunner::testSingleflight);
        check("CfPatterns", TestRunner::testCf);
        check("BoundedBuffer", TestRunner::testBuffer);
        check("TokenBucket", TestRunner::testBucket);
        check("SlidingWindowLimiter", TestRunner::testWindow);
        check("CircuitBreaker", TestRunner::testBreaker);
        check("Retry", TestRunner::testRetry);
        check("SimpleThreadPool", TestRunner::testPool);
        check("ObjectPool", TestRunner::testObjectPool);
        check("EventBus", TestRunner::testBus);
        check("ConsistentHashRing", TestRunner::testHash);
        check("LoadBalancer", TestRunner::testLb);
        check("IdempotencyStore", TestRunner::testIdem);
        check("ReadWriteCache", TestRunner::testRw);
        check("UnionFind", TestRunner::testUf);
        check("Trie", TestRunner::testTrie);
        check("TopoSort", TestRunner::testTopo);
        check("BloomFilter", TestRunner::testBloom);
        check("SingletonDcl", TestRunner::testSingleton);
        check("OrderStateMachine", TestRunner::testFsm);
        check("CachingProxy", TestRunner::testProxy);
        check("Pipeline", TestRunner::testPipeline);
        check("ClassicPatterns", TestRunner::testPatterns);
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
        } catch (Throwable e) {
            failed++;
            System.out.println("FAIL " + name + " — " + e);
            e.printStackTrace(System.out);
        }
    }

    private static void testLruLinked() {
        LruLinkedHashMap<Integer, String> c = new LruLinkedHashMap<Integer, String>(2);
        c.put(1, "a");
        c.put(2, "b");
        eq("a", c.get(1));
        c.put(3, "c");
        isTrue(!c.containsKey(2), "2 should be evicted");
        eq("a", c.get(1));
        eq("c", c.get(3));
    }

    private static void testLruDll() {
        LruCache<Integer, String> c = new LruCache<Integer, String>(2);
        c.put(1, "a");
        c.put(2, "b");
        eq("a", c.get(1));
        c.put(3, "c");
        isTrue(!c.containsKey(2), "2 should be evicted");
        eq("c", c.get(3));
        c.put(3, "c2");
        eq("c2", c.get(3));
        eq(2, c.size());
    }

    private static void testTtl() {
        final AtomicLong now = new AtomicLong(1000);
        TtlCache<String, String> c = new TtlCache<String, String>(100, new TtlCache.Clock() {
            @Override
            public long nowMs() {
                return now.get();
            }
        });
        c.put("k", "v");
        eq("v", c.get("k"));
        now.set(1099);
        eq("v", c.get("k"));
        now.set(1100);
        eq(null, c.get("k"));
    }

    private static void testMemoizer() {
        final AtomicInteger calls = new AtomicInteger();
        Memoizer<Integer, Integer> m = new Memoizer<Integer, Integer>(new java.util.function.Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer k) {
                calls.incrementAndGet();
                return k * 2;
            }
        });
        eq(10, m.get(5));
        eq(10, m.get(5));
        eq(1, calls.get());
    }

    private static void testSingleflight() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        ExecutorService exec = Executors.newFixedThreadPool(8);
        SingleflightCache<String, String> cache = new SingleflightCache<String, String>(
                new java.util.function.Function<String, String>() {
                    @Override
                    public String apply(String k) {
                        calls.incrementAndGet();
                        started.countDown();
                        try {
                            if (!release.await(5, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("stuck");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }
                        return "v-" + k;
                    }
                }, exec);
        List<CompletableFuture<String>> fs = new ArrayList<CompletableFuture<String>>();
        for (int i = 0; i < 12; i++) {
            fs.add(cache.getAsync("a"));
        }
        isTrue(started.await(5, TimeUnit.SECONDS), "loader started");
        release.countDown();
        for (CompletableFuture<String> f : fs) {
            eq("v-a", f.get(5, TimeUnit.SECONDS));
        }
        eq(1, calls.get());
        eq("v-a", cache.get("a"));
        eq(1, calls.get());
        exec.shutdownNow();
    }

    private static void testCf() throws Exception {
        eq("AB", CfPatterns.map(CompletableFuture.completedFuture("A"), new java.util.function.Function<String, String>() {
            @Override
            public String apply(String s) {
                return s + "B";
            }
        }).get());
        eq("xy", CfPatterns.flatMap(CompletableFuture.completedFuture("x"), new java.util.function.Function<String, CompletableFuture<String>>() {
            @Override
            public CompletableFuture<String> apply(String s) {
                return CompletableFuture.completedFuture(s + "y");
            }
        }).get());
        eq("12", CfPatterns.zip(
                CompletableFuture.completedFuture("1"),
                CompletableFuture.completedFuture("2"),
                new java.util.function.BiFunction<String, String, String>() {
                    @Override
                    public String apply(String a, String b) {
                        return a + b;
                    }
                }).get());
        List<CompletableFuture<Integer>> fs = Arrays.asList(
                CompletableFuture.completedFuture(1),
                CompletableFuture.completedFuture(2),
                CompletableFuture.completedFuture(3));
        eq(Arrays.asList(1, 2, 3), CfPatterns.allOf(fs).get());
        eq("fb", CfPatterns.recover(failed("boom"), new java.util.function.Function<Throwable, String>() {
            @Override
            public String apply(Throwable t) {
                return "fb";
            }
        }).get());

        ScheduledExecutorService sched = Executors.newSingleThreadScheduledExecutor();
        try {
            CompletableFuture<String> slow = new CompletableFuture<String>();
            try {
                CfPatterns.withTimeout(slow, 30, TimeUnit.MILLISECONDS, sched).get(1, TimeUnit.SECONDS);
                throw new AssertionError("expected timeout");
            } catch (Exception e) {
                isTrue(root(e) instanceof TimeoutException, "timeout");
            }
            eq("ok", CfPatterns.withTimeout(CompletableFuture.completedFuture("ok"), 1, TimeUnit.SECONDS, sched).get());
        } finally {
            sched.shutdownNow();
        }

        final AtomicInteger tries = new AtomicInteger();
        String retried = CfPatterns.retryAsync(new java.util.function.Supplier<CompletableFuture<String>>() {
            @Override
            public CompletableFuture<String> get() {
                if (tries.incrementAndGet() < 3) {
                    return failed("x");
                }
                return CompletableFuture.completedFuture("done");
            }
        }, 5).get(2, TimeUnit.SECONDS);
        eq("done", retried);
        eq(3, tries.get());

        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Integer> out = CfPatterns.parallelMap(Arrays.asList(1, 2, 3), new java.util.function.Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer x) {
                    return x * 10;
                }
            }, pool);
            eq(Arrays.asList(10, 20, 30), out);
        } finally {
            pool.shutdownNow();
        }
    }

    private static CompletableFuture<String> failed(String msg) {
        CompletableFuture<String> f = new CompletableFuture<String>();
        f.completeExceptionally(new IllegalStateException(msg));
        return f;
    }

    private static void testBuffer() throws Exception {
        final BoundedBuffer<Integer> buf = new BoundedBuffer<Integer>(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        final CountDownLatch done = new CountDownLatch(1);
        pool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    buf.put(1);
                    buf.put(2);
                    buf.put(3);
                    done.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        Thread.sleep(40);
        isTrue(done.getCount() == 1, "put should block when full");
        eq(1, buf.take());
        isTrue(done.await(2, TimeUnit.SECONDS), "unblocked");
        eq(2, buf.take());
        eq(3, buf.take());
        pool.shutdownNow();
    }

    private static void testBucket() {
        final AtomicLong now = new AtomicLong(0);
        TokenBucket b = new TokenBucket(2, 10, new TokenBucket.Clock() {
            @Override
            public long nowNanos() {
                return now.get();
            }
        });
        isTrue(b.tryAcquire(), "1");
        isTrue(b.tryAcquire(), "2");
        isTrue(!b.tryAcquire(), "empty");
        now.set(100_000_000L);
        isTrue(b.tryAcquire(), "refilled 1");
    }

    private static void testWindow() {
        final AtomicLong now = new AtomicLong(0);
        SlidingWindowLimiter lim = new SlidingWindowLimiter(2, 100, new SlidingWindowLimiter.Clock() {
            @Override
            public long nowMs() {
                return now.get();
            }
        });
        isTrue(lim.allow("u"));
        isTrue(lim.allow("u"));
        isTrue(!lim.allow("u"));
        now.set(100);
        isTrue(lim.allow("u"));
    }

    private static void testBreaker() {
        final AtomicLong now = new AtomicLong(0);
        final CircuitBreaker cb = new CircuitBreaker(2, 50, new CircuitBreaker.Clock() {
            @Override
            public long nowMs() {
                return now.get();
            }
        });
        try {
            cb.call(boom());
            throw new AssertionError("expected");
        } catch (IllegalStateException ignored) {
        }
        try {
            cb.call(boom());
        } catch (IllegalStateException ignored) {
        }
        eq(CircuitBreaker.State.OPEN, cb.state());
        try {
            cb.call(ok());
            throw new AssertionError("open");
        } catch (CircuitBreaker.OpenCircuitException ignored) {
        }
        now.set(50);
        eq("ok", cb.call(ok()));
        eq(CircuitBreaker.State.CLOSED, cb.state());
    }

    private static java.util.function.Supplier<String> boom() {
        return new java.util.function.Supplier<String>() {
            @Override
            public String get() {
                throw new IllegalStateException("down");
            }
        };
    }

    private static java.util.function.Supplier<String> ok() {
        return new java.util.function.Supplier<String>() {
            @Override
            public String get() {
                return "ok";
            }
        };
    }

    private static void testRetry() throws Exception {
        final AtomicInteger n = new AtomicInteger();
        final List<Long> sleeps = new ArrayList<Long>();
        String v = Retry.call(new Callable<String>() {
            @Override
            public String call() {
                if (n.incrementAndGet() < 3) {
                    throw new IllegalStateException("no");
                }
                return "yes";
            }
        }, 5, 10, 100, new Retry.Sleeper() {
            @Override
            public void sleep(long ms) {
                sleeps.add(ms);
            }
        }, new Random(1));
        eq("yes", v);
        eq(3, n.get());
        eq(2, sleeps.size());
    }

    private static void testPool() throws Exception {
        SimpleThreadPool pool = new SimpleThreadPool(3);
        final AtomicInteger sum = new AtomicInteger();
        final CountDownLatch done = new CountDownLatch(50);
        for (int i = 0; i < 50; i++) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    sum.incrementAndGet();
                    done.countDown();
                }
            });
        }
        isTrue(done.await(5, TimeUnit.SECONDS), "tasks");
        pool.shutdown();
        isTrue(pool.awaitTermination(5, TimeUnit.SECONDS), "term");
        eq(50, sum.get());
    }

    private static void testObjectPool() throws Exception {
        final AtomicInteger created = new AtomicInteger();
        ObjectPool<Integer> pool = new ObjectPool<Integer>(2, new java.util.function.Supplier<Integer>() {
            @Override
            public Integer get() {
                return created.incrementAndGet();
            }
        });
        eq(2, created.get());
        Integer a = pool.borrow(1, TimeUnit.SECONDS);
        Integer b = pool.borrow(1, TimeUnit.SECONDS);
        eq(0, pool.available());
        pool.release(a);
        eq(1, pool.available());
        pool.release(b);
        eq(2, pool.available());
    }

    private static void testBus() {
        EventBus bus = new EventBus();
        final List<String> got = new ArrayList<String>();
        bus.subscribe(String.class, new java.util.function.Consumer<String>() {
            @Override
            public void accept(String s) {
                got.add(s);
            }
        });
        bus.publish("hi");
        bus.publish(Integer.valueOf(1));
        eq(Collections.singletonList("hi"), got);
    }

    private static void testHash() {
        ConsistentHashRing ring = new ConsistentHashRing(8, Arrays.asList("a", "b", "c"));
        String n = ring.get("user-1");
        isTrue(n.equals("a") || n.equals("b") || n.equals("c"), n);
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < 200; i++) {
            seen.add(ring.get("k" + i));
        }
        isTrue(seen.size() >= 2, "spread");
        ring.remove("a");
        for (int i = 0; i < 30; i++) {
            isTrue(!"a".equals(ring.get("z" + i)), "removed");
        }
    }

    private static void testLb() {
        LoadBalancer.RoundRobin rr = new LoadBalancer.RoundRobin(Arrays.asList("a", "b", "c"));
        eq("a", rr.next());
        eq("b", rr.next());
        eq("c", rr.next());
        eq("a", rr.next());
        LoadBalancer.LeastConnections lc = new LoadBalancer.LeastConnections(Arrays.asList("a", "b"));
        String first = lc.acquire();
        String second = lc.acquire();
        isTrue(!first.equals(second), "spread load");
        lc.release(first);
        eq(first, lc.acquire());
    }

    private static void testIdem() throws Exception {
        IdempotencyStore<String> store = new IdempotencyStore<String>();
        final AtomicInteger calls = new AtomicInteger();
        String a = store.executeOnce("pay-1", new Callable<String>() {
            @Override
            public String call() {
                return "ok-" + calls.incrementAndGet();
            }
        });
        String b = store.executeOnce("pay-1", new Callable<String>() {
            @Override
            public String call() {
                return "ok-" + calls.incrementAndGet();
            }
        });
        eq("ok-1", a);
        eq(a, b);
        eq(1, calls.get());
    }

    private static void testRw() throws Exception {
        final ReadWriteCache<String, Integer> c = new ReadWriteCache<String, Integer>();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        final CountDownLatch done = new CountDownLatch(40);
        for (int i = 0; i < 40; i++) {
            final int id = i;
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    c.put("k" + (id % 5), id);
                    c.get("k" + (id % 5));
                    done.countDown();
                }
            });
        }
        isTrue(done.await(5, TimeUnit.SECONDS), "rw");
        eq(5, c.size());
        pool.shutdownNow();
    }

    private static void testUf() {
        UnionFind uf = new UnionFind(5);
        uf.union(0, 1);
        uf.union(2, 3);
        isTrue(uf.connected(0, 1), "01");
        isTrue(!uf.connected(0, 2), "02");
        uf.union(1, 2);
        isTrue(uf.connected(0, 3), "03");
        eq(2, uf.components());
    }

    private static void testTrie() {
        Trie t = new Trie();
        t.insert("app");
        t.insert("apple");
        isTrue(t.search("app"), "app");
        isTrue(!t.search("ap"), "ap");
        isTrue(t.startsWith("ap"), "prefix");
        isTrue(t.search("apple"), "apple");
    }

    private static void testTopo() {
        List<Integer> order = TopoSort.kahn(4, new int[][] {{0, 1}, {0, 2}, {1, 3}, {2, 3}});
        eq(4, order.size());
        isTrue(order.indexOf(0) < order.indexOf(1), "0 before 1");
        isTrue(order.indexOf(3) == 3 || order.contains(3), "has 3");
        eq(0, TopoSort.kahn(2, new int[][] {{0, 1}, {1, 0}}).size());
    }

    private static void testBloom() {
        BloomFilter f = new BloomFilter(256);
        f.add("alice");
        f.add("bob");
        isTrue(f.mightContain("alice"), "alice");
        isTrue(f.mightContain("bob"), "bob");
    }

    private static void testSingleton() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        final Set<SingletonDcl> seen = Collections.synchronizedSet(new HashSet<SingletonDcl>());
        final CountDownLatch done = new CountDownLatch(40);
        for (int i = 0; i < 40; i++) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    seen.add(SingletonDcl.getInstance());
                    done.countDown();
                }
            });
        }
        isTrue(done.await(5, TimeUnit.SECONDS), "singleton");
        eq(1, seen.size());
        pool.shutdownNow();
    }

    private static void testFsm() {
        OrderStateMachine fsm = new OrderStateMachine();
        eq(OrderStateMachine.State.PAID, fsm.apply(OrderStateMachine.Event.PAY));
        eq(OrderStateMachine.State.SHIPPED, fsm.apply(OrderStateMachine.Event.SHIP));
        eq(OrderStateMachine.State.DELIVERED, fsm.apply(OrderStateMachine.Event.DELIVER));
        OrderStateMachine c = new OrderStateMachine();
        c.apply(OrderStateMachine.Event.CANCEL);
        eq(OrderStateMachine.State.CANCELLED, c.state());
        try {
            c.apply(OrderStateMachine.Event.PAY);
            throw new AssertionError("illegal");
        } catch (IllegalStateException ignored) {
        }
    }

    private static void testProxy() {
        CachingProxy.SlowUserRepo slow = new CachingProxy.SlowUserRepo();
        CachingProxy.UserRepo repo = new CachingProxy.CachedUserRepo(slow);
        eq("user-7", repo.findName(7));
        eq("user-7", repo.findName(7));
        eq(1, slow.calls());
    }

    private static void testPipeline() {
        Pipeline<String> p = new Pipeline<String>()
                .add(new java.util.function.Function<String, String>() {
                    @Override
                    public String apply(String s) {
                        return s.trim();
                    }
                })
                .add(new java.util.function.Function<String, String>() {
                    @Override
                    public String apply(String s) {
                        return s.toUpperCase();
                    }
                });
        eq("HI", p.run("  hi "));
    }

    private static void testPatterns() {
        final AtomicInteger seen = new AtomicInteger();
        ClassicPatterns.Subject s = new ClassicPatterns.Subject();
        s.add(new ClassicPatterns.Observer() {
            @Override
            public void onChange(int value) {
                seen.set(value);
            }
        });
        s.set(9);
        eq(9, seen.get());
        eq(80, ClassicPatterns.checkout(100, new ClassicPatterns.PercentOff(20)));
        eq(70, ClassicPatterns.checkout(100, new ClassicPatterns.CouponOff(30)));
        eq("*hi*", new ClassicPatterns.Bold(new ClassicPatterns.Plain("hi")).render());
        ClassicPatterns.User u = new ClassicPatterns.UserBuilder().name("linh").age(30).build();
        eq("linh", u.name);
        eq(30, u.age);
        eq("woof", ClassicPatterns.create("dog").speak());
    }

    private static void eq(Object exp, Object act) {
        if (exp == null ? act != null : !exp.equals(act)) {
            throw new AssertionError("expected " + exp + " but " + act);
        }
    }

    private static void isTrue(boolean v, String msg) {
        if (!v) {
            throw new AssertionError(msg);
        }
    }

    private static Throwable root(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
