package iv;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Exception trên chuỗi CompletableFuture nhiều stage.
 * <p>
 * Giống đặt đồ ăn: nhận đơn → bếp nấu → shipper lấy → giao cửa → app hiện kết quả.
 * Lỗi ở <em>bất kỳ</em> khâu chưa được cứu thì các {@code thenApply} sau bị bỏ,
 * nhưng vẫn chảy tới {@code exceptionally}/{@code handle} cuối (hoặc {@code join()} ném).
 * Đã cứu giữa đường thì khâu cuối thấy hàng thay thế, không còn exception.
 */
public final class CfExceptionPipeline {
    private CfExceptionPipeline() {
    }

    public static final class Trace {
        private final List<String> steps = new ArrayList<String>();

        public synchronized void add(String step) {
            steps.add(step);
        }

        public synchronized List<String> snapshot() {
            return new ArrayList<String>(steps);
        }

        public synchronized boolean contains(String step) {
            return steps.contains(step);
        }
    }

    /** Bếp cháy món, không ai cứu giữa đường → app cuối hoàn tiền. Shipper/giao bị skip. */
    public static String cookFailsCaughtAtApp(Trace t) {
        return accept(t)
                .thenApply(order -> cook(t, order, false))
                .thenApply(food -> pickup(t, food))
                .thenApply(food -> deliver(t, food))
                .exceptionally(ex -> {
                    t.add("app:hoan-tien");
                    return "Hoan tien: " + unwrap(ex).getMessage();
                })
                .join();
    }

    /** Bếp cháy món, đổi món ngay tại bếp → shipper vẫn lấy, app thấy thành công. exceptionally cuối không chạy. */
    public static String cookFailsRecoveredInKitchen(Trace t) {
        return accept(t)
                .thenApply(order -> cook(t, order, false))
                .exceptionally(ex -> {
                    t.add("bep:doi-mon");
                    return "com-tam (thay the)";
                })
                .thenApply(food -> pickup(t, food))
                .thenApply(food -> deliver(t, food))
                .exceptionally(ex -> {
                    t.add("app:hoan-tien");
                    return "Hoan tien: " + unwrap(ex).getMessage();
                })
                .join();
    }

    /**
     * {@code whenComplete} chỉ ghi sổ (nhìn lỗi), không cứu.
     * Exception vẫn chảy tới app.
     */
    public static String cookFailsManagerOnlyNotes(Trace t) {
        return accept(t)
                .thenApply(order -> cook(t, order, false))
                .whenComplete((food, ex) -> t.add("quan-ly:ghi-so"))
                .thenApply(food -> pickup(t, food))
                .thenApply(food -> deliver(t, food))
                .handle((food, ex) -> {
                    if (ex != null) {
                        t.add("app:handle-loi");
                        return "App hien loi: " + unwrap(ex).getMessage();
                    }
                    t.add("app:handle-ok");
                    return "App: " + food;
                })
                .join();
    }

    /** Step cuối là {@code thenApply} — không bắt lỗi. {@code join()} mới ném. */
    public static CompletableFuture<String> cookFailsThenApplyAtEndDoesNotCatch(Trace t) {
        return accept(t)
                .thenApply(order -> cook(t, order, false))
                .thenApply(food -> pickup(t, food))
                .thenApply(food -> "App nhan: " + food);
    }

    /** Happy path: mọi thenApply chạy, exceptionally cuối không chạy. */
    public static String happy(Trace t) {
        return accept(t)
                .thenApply(order -> cook(t, order, true))
                .thenApply(food -> pickup(t, food))
                .thenApply(food -> deliver(t, food))
                .exceptionally(ex -> {
                    t.add("app:hoan-tien");
                    return "Hoan tien: " + unwrap(ex).getMessage();
                })
                .join();
    }

    static CompletableFuture<String> accept(Trace t) {
        t.add("nhan-don");
        return CompletableFuture.completedFuture("don-pho");
    }

    static String cook(Trace t, String order, boolean ok) {
        t.add("bep:nau");
        if (!ok) {
            t.add("bep:chay-mon");
            throw new IllegalStateException("bep chay pho");
        }
        return "to-pho";
    }

    static String pickup(Trace t, String food) {
        t.add("shipper:lay");
        return food;
    }

    static String deliver(Trace t, String food) {
        t.add("giao-cua");
        return food;
    }

    static Throwable unwrap(Throwable ex) {
        if (ex instanceof CompletionException && ex.getCause() != null) {
            return ex.getCause();
        }
        return ex;
    }
}
