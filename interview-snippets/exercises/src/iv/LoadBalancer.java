package iv;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LB cổ điển: Round-robin và Least-connections.
 */
public final class LoadBalancer {
    private LoadBalancer() {
    }

    public static final class RoundRobin {
        private final List<String> backends;
        private final AtomicInteger idx = new AtomicInteger();

        public RoundRobin(List<String> backends) {
            if (backends == null || backends.isEmpty()) {
                throw new IllegalArgumentException("backends");
            }
            this.backends = new ArrayList<String>(backends);
        }

        public String next() {
            int n = backends.size();
            int r = idx.getAndIncrement() % n;
            return backends.get(r < 0 ? r + n : r);
        }
    }

    public static final class LeastConnections {
        private static final class Node {
            final String name;
            int active;

            Node(String name) {
                this.name = name;
            }
        }

        private final List<Node> nodes = new ArrayList<Node>();

        public LeastConnections(List<String> backends) {
            if (backends == null || backends.isEmpty()) {
                throw new IllegalArgumentException("backends");
            }
            for (String b : backends) {
                nodes.add(new Node(b));
            }
        }

        public synchronized String acquire() {
            Node best = nodes.get(0);
            for (int i = 1; i < nodes.size(); i++) {
                if (nodes.get(i).active < best.active) {
                    best = nodes.get(i);
                }
            }
            best.active++;
            return best.name;
        }

        public synchronized void release(String name) {
            for (Node n : nodes) {
                if (n.name.equals(name) && n.active > 0) {
                    n.active--;
                    return;
                }
            }
        }

        public synchronized int active(String name) {
            for (Node n : nodes) {
                if (n.name.equals(name)) {
                    return n.active;
                }
            }
            return -1;
        }
    }
}
