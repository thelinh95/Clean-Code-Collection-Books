package conc;

import java.util.concurrent.atomic.AtomicInteger;

public class SafeCounter {
    private final AtomicInteger n = new AtomicInteger();

    public void inc() {
        n.incrementAndGet();
    }

    public int get() {
        return n.get();
    }
}
