package conc;

/**
 * Lazy singleton thread-safe. Dùng Holder / enum, không DCL thiếu volatile.
 */
public final class LazySingleton {
    private LazySingleton() {}

    public static Expensive get() {
        throw new UnsupportedOperationException("TODO LazySingleton.get");
    }

    public static final class Expensive {
        static final java.util.concurrent.atomic.AtomicInteger CONSTRUCTED =
                new java.util.concurrent.atomic.AtomicInteger();

        Expensive() {
            CONSTRUCTED.incrementAndGet();
        }
    }
}
