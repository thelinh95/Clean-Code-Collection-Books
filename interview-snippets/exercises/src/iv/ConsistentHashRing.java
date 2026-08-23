package iv;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Consistent hashing + virtual nodes. Thêm/bớt node chỉ ảnh hưởng ~1/N key.
 * Hash đơn giản (hashCode) đủ viết tay; production dùng MD5/Murmur.
 */
public class ConsistentHashRing {
    private final int virtualNodes;
    private final TreeMap<Integer, String> ring = new TreeMap<Integer, String>();

    public ConsistentHashRing(int virtualNodes, Collection<String> nodes) {
        if (virtualNodes <= 0) {
            throw new IllegalArgumentException("virtualNodes");
        }
        this.virtualNodes = virtualNodes;
        for (String node : nodes) {
            add(node);
        }
    }

    public synchronized void add(String node) {
        for (int i = 0; i < virtualNodes; i++) {
            ring.put(hash(node + "#" + i), node);
        }
    }

    public synchronized void remove(String node) {
        for (int i = 0; i < virtualNodes; i++) {
            ring.remove(hash(node + "#" + i));
        }
    }

    public synchronized String get(String key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("empty ring");
        }
        int h = hash(key);
        SortedMap<Integer, String> tail = ring.tailMap(h);
        Integer point = tail.isEmpty() ? ring.firstKey() : tail.firstKey();
        return ring.get(point);
    }

    public synchronized int size() {
        return ring.size();
    }

    static int hash(String s) {
        int h = 0x811c9dc5;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x01000193;
        }
        return h;
    }
}
