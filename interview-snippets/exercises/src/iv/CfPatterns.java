package iv;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Công thức CompletableFuture hay bị hỏi (Java 8 — không có {@code orTimeout}).
 * <ul>
 *   <li>{@code thenApply} = map (sync trên kết quả)</li>
 *   <li>{@code thenCompose} = flatMap (trả về Future lồng)</li>
 *   <li>{@code thenCombine} = zip 2 future</li>
 *   <li>{@code allOf} / {@code anyOf} = fan-in</li>
 * </ul>
 */
public final class CfPatterns {
    private CfPatterns() {
    }

    public static <T, R> CompletableFuture<R> map(CompletableFuture<T> future, Function<T, R> fn) {
        return future.thenApply(fn);
    }

    public static <T, R> CompletableFuture<R> flatMap(
            CompletableFuture<T> future,
            Function<T, CompletableFuture<R>> fn) {
        return future.thenCompose(fn);
    }

    public static <A, B, R> CompletableFuture<R> zip(
            CompletableFuture<A> a,
            CompletableFuture<B> b,
            BiFunction<A, B, R> fn) {
        return a.thenCombine(b, fn);
    }

    public static <T> CompletableFuture<List<T>> allOf(List<CompletableFuture<T>> futures) {
        @SuppressWarnings("unchecked")
        CompletableFuture<T>[] arr = futures.toArray(new CompletableFuture[0]);
        return CompletableFuture.allOf(arr).thenApply(new Function<Void, List<T>>() {
            @Override
            public List<T> apply(Void ignored) {
                List<T> out = new ArrayList<T>(futures.size());
                for (CompletableFuture<T> f : futures) {
                    out.add(f.join());
                }
                return out;
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> anyOf(List<CompletableFuture<T>> futures) {
        CompletableFuture<?>[] arr = futures.toArray(new CompletableFuture[0]);
        return CompletableFuture.anyOf(arr).thenApply(new Function<Object, T>() {
            @Override
            public T apply(Object o) {
                return (T) o;
            }
        });
    }

    public static <T> CompletableFuture<T> recover(
            CompletableFuture<T> future,
            Function<Throwable, T> fallback) {
        return future.exceptionally(fallback);
    }

    /**
     * Timeout kiểu Java 8: race với future fail sau N ms.
     * Java 9+: {@code future.orTimeout(n, unit)}.
     */
    public static <T> CompletableFuture<T> withTimeout(
            final CompletableFuture<T> future,
            long timeout,
            TimeUnit unit,
            ScheduledExecutorService scheduler) {
        final CompletableFuture<T> timeoutFuture = new CompletableFuture<T>();
        final ScheduledFuture<?> scheduled = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                timeoutFuture.completeExceptionally(new TimeoutException("timeout"));
            }
        }, timeout, unit);
        future.whenComplete(new java.util.function.BiConsumer<T, Throwable>() {
            @Override
            public void accept(T v, Throwable ex) {
                scheduled.cancel(false);
            }
        });
        return future.applyToEither(timeoutFuture, Function.<T>identity());
    }

    public static <T> CompletableFuture<T> retryAsync(
            final Supplier<CompletableFuture<T>> action,
            final int maxAttempts) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts");
        }
        final CompletableFuture<T> result = new CompletableFuture<T>();
        attempt(action, maxAttempts, result);
        return result;
    }

    private static <T> void attempt(
            final Supplier<CompletableFuture<T>> action,
            final int left,
            final CompletableFuture<T> result) {
        try {
            action.get().whenComplete(new java.util.function.BiConsumer<T, Throwable>() {
                @Override
                public void accept(T value, Throwable ex) {
                    if (ex == null) {
                        result.complete(value);
                    } else if (left <= 1) {
                        result.completeExceptionally(unwrap(ex));
                    } else {
                        attempt(action, left - 1, result);
                    }
                }
            });
        } catch (RuntimeException e) {
            if (left <= 1) {
                result.completeExceptionally(e);
            } else {
                attempt(action, left - 1, result);
            }
        }
    }

    public static <T, R> List<R> parallelMap(List<T> input, final Function<T, R> fn, Executor executor) {
        List<CompletableFuture<R>> futures = new ArrayList<CompletableFuture<R>>(input.size());
        for (final T item : input) {
            futures.add(CompletableFuture.supplyAsync(new Supplier<R>() {
                @Override
                public R get() {
                    return fn.apply(item);
                }
            }, executor));
        }
        return allOf(futures).join();
    }

    static Throwable unwrap(Throwable ex) {
        if (ex instanceof CompletionException && ex.getCause() != null) {
            return ex.getCause();
        }
        return ex;
    }
}
