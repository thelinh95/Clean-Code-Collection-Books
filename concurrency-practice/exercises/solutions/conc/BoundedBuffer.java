package conc;

import java.util.ArrayDeque;
import java.util.Deque;

public class BoundedBuffer<T> {
    private final int capacity;
    private final Deque<T> q = new ArrayDeque<>();

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
}
