package iv;

/**
 * Bloom filter: có thể false positive, không false negative.
 * 2 hash đơn giản đủ để giải thích bitset.
 */
public class BloomFilter {
    private final int[] bits;
    private final int m;

    public BloomFilter(int bitSize) {
        if (bitSize <= 0) {
            throw new IllegalArgumentException("bitSize");
        }
        this.m = bitSize;
        this.bits = new int[(bitSize + 31) >>> 5];
    }

    public void add(String key) {
        set(h1(key));
        set(h2(key));
    }

    public boolean mightContain(String key) {
        return get(h1(key)) && get(h2(key));
    }

    private void set(int bit) {
        bits[bit >>> 5] |= (1 << (bit & 31));
    }

    private boolean get(int bit) {
        return (bits[bit >>> 5] & (1 << (bit & 31))) != 0;
    }

    private int h1(String s) {
        return floorMod(s.hashCode(), m);
    }

    private int h2(String s) {
        int h = 0x811c9dc5;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x01000193;
        }
        return floorMod(h, m);
    }

    static int floorMod(int a, int m) {
        int r = a % m;
        return r < 0 ? r + m : r;
    }
}
