package conc;

/**
 * Ba thread gọi first/second/third theo thứ tự bất kỳ;
 * runnable phải chạy theo đúng thứ tự first → second → third.
 */
public class OrderedPrinter {
    public void first(Runnable printFirst) throws InterruptedException {
        throw new UnsupportedOperationException("TODO OrderedPrinter.first");
    }

    public void second(Runnable printSecond) throws InterruptedException {
        throw new UnsupportedOperationException("TODO OrderedPrinter.second");
    }

    public void third(Runnable printThird) throws InterruptedException {
        throw new UnsupportedOperationException("TODO OrderedPrinter.third");
    }
}
