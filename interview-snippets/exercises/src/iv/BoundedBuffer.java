package iv;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Producer–Consumer / blocking queue tự viết (wait/notify).
 * Luôn {@code while} (không {@code if}) vì spurious wakeup + nhiều waiter.
 */
public class BoundedBuffer<T> {
    private final int capacity;
    private final Deque<T> q = new ArrayDeque<T>();

    public BoundedBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity");
        }
        this.capacity = capacity;
    }

    public synchronized void put(T item) throws InterruptedException {
        while (q.size() == capacity) {
            wait();
        }
        q.addLast(item);
        notifyAll();
    }

    public synchronized T take() throws InterruptedException {
        while (q.isEmpty()) {
            wait();
        }
        T v = q.removeFirst();
        notifyAll();
        return v;
    }

    public synchronized int size() {
        return q.size();
    }
}
