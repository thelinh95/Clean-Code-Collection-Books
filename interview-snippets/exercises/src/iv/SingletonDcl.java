package iv;

/**
 * Double-checked locking. {@code volatile} bắt buộc (Java 5+) để chặn
 * reorder constructor vs gán reference.
 */
public final class SingletonDcl {
    private static volatile SingletonDcl instance;

    private SingletonDcl() {
    }

    public static SingletonDcl getInstance() {
        SingletonDcl local = instance;
        if (local == null) {
            synchronized (SingletonDcl.class) {
                local = instance;
                if (local == null) {
                    instance = local = new SingletonDcl();
                }
            }
        }
        return local;
    }
}
