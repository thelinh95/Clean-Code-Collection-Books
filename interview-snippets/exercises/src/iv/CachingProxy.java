package iv;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Proxy pattern: bọc service chậm, cache kết quả (cache-aside).
 */
public final class CachingProxy {
    private CachingProxy() {
    }

    public interface UserRepo {
        String findName(int id);
    }

    public static final class SlowUserRepo implements UserRepo {
        private int calls;

        @Override
        public synchronized String findName(int id) {
            calls++;
            return "user-" + id;
        }

        public synchronized int calls() {
            return calls;
        }
    }

    public static final class CachedUserRepo implements UserRepo {
        private final UserRepo inner;
        private final ConcurrentHashMap<Integer, String> cache = new ConcurrentHashMap<Integer, String>();

        public CachedUserRepo(UserRepo inner) {
            this.inner = inner;
        }

        @Override
        public String findName(int id) {
            String hit = cache.get(id);
            if (hit != null) {
                return hit;
            }
            return cache.computeIfAbsent(id, new java.util.function.Function<Integer, String>() {
                @Override
                public String apply(Integer k) {
                    return inner.findName(k);
                }
            });
        }
    }
}
