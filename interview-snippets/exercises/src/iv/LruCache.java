package iv;

import java.util.HashMap;
import java.util.Map;

/**
 * LRU — bản HashMap + doubly linked list (LeetCode 146).
 * get/put O(1): map tìm node, list chuyển node lên head (MRU), đuổi tail (LRU).
 */
public class LruCache<K, V> {
    private static final class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private final int capacity;
    private final Map<K, Node<K, V>> map = new HashMap<K, Node<K, V>>();
    private final Node<K, V> head = new Node<K, V>(null, null);
    private final Node<K, V> tail = new Node<K, V>(null, null);

    public LruCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity");
        }
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    public V get(K key) {
        Node<K, V> n = map.get(key);
        if (n == null) {
            return null;
        }
        moveToHead(n);
        return n.value;
    }

    public void put(K key, V value) {
        Node<K, V> n = map.get(key);
        if (n != null) {
            n.value = value;
            moveToHead(n);
            return;
        }
        Node<K, V> created = new Node<K, V>(key, value);
        map.put(key, created);
        addAfterHead(created);
        if (map.size() > capacity) {
            Node<K, V> lru = tail.prev;
            remove(lru);
            map.remove(lru.key);
        }
    }

    public int size() {
        return map.size();
    }

    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    private void moveToHead(Node<K, V> n) {
        remove(n);
        addAfterHead(n);
    }

    private void addAfterHead(Node<K, V> n) {
        n.next = head.next;
        n.prev = head;
        head.next.prev = n;
        head.next = n;
    }

    private void remove(Node<K, V> n) {
        n.prev.next = n.next;
        n.next.prev = n.prev;
        n.prev = null;
        n.next = null;
    }
}
