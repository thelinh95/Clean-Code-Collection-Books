package conc;

/**
 * Buffer giới hạn: put block khi đầy, take block khi rỗng.
 */
public class BoundedBuffer<T> {
    public BoundedBuffer(int capacity) {
        throw new UnsupportedOperationException("TODO BoundedBuffer ctor");
    }

    public void put(T item) throws InterruptedException {
        throw new UnsupportedOperationException("TODO BoundedBuffer.put");
    }

    public T take() throws InterruptedException {
        throw new UnsupportedOperationException("TODO BoundedBuffer.take");
    }
}
