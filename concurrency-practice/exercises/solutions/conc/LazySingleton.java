package conc;

import java.util.concurrent.atomic.AtomicInteger;

public final class LazySingleton {
    private LazySingleton() {}

    private static class Holder {
        static final Expensive INSTANCE = new Expensive();
    }

    public static Expensive get() {
        return Holder.INSTANCE;
    }

    public static final class Expensive {
        static final AtomicInteger CONSTRUCTED = new AtomicInteger();

        Expensive() {
            CONSTRUCTED.incrementAndGet();
        }
    }
}
