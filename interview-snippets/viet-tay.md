# Bản viết tay — thuộc lòng

Đóng IDE, viết lại các khối dưới. Đủ để CoderPad chạy.

## 1. LRU LinkedHashMap (2 phút)

```java
class Lru<K,V> extends LinkedHashMap<K,V> {
    final int cap;
    Lru(int cap) { super(cap, 0.75f, true); this.cap = cap; }
    protected boolean removeEldestEntry(Map.Entry<K,V> e) { return size() > cap; }
}
```

## 2. LRU HashMap + DLL (10 phút)

```java
class LRU {
    static class N { int k,v; N p,n; N(int k,int v){this.k=k;this.v=v;} }
    final int cap; Map<Integer,N> m = new HashMap<>();
    N h = new N(0,0), t = new N(0,0);
    LRU(int c){ cap=c; h.n=t; t.p=h; }
    int get(int k){ N x=m.get(k); if(x==null) return -1; move(x); return x.v; }
    void put(int k,int v){
        N x=m.get(k);
        if(x!=null){ x.v=v; move(x); return; }
        x=new N(k,v); m.put(k,x); add(x);
        if(m.size()>cap){ N lru=t.p; del(lru); m.remove(lru.k); }
    }
    void move(N x){ del(x); add(x); }
    void add(N x){ x.n=h.n; x.p=h; h.n.p=x; h.n=x; }
    void del(N x){ x.p.n=x.n; x.n.p=x.p; }
}
```

## 3. Singleflight (chống stampede)

```java
V get(K key) {
    V hit = values.get(key);
    if (hit != null) return hit;
    CompletableFuture<V> mine = new CompletableFuture<V>();
    CompletableFuture<V> you = inflight.putIfAbsent(key, mine);
    if (you != null) return you.join();
    try {
        V v = loader.apply(key);
        values.put(key, v);
        mine.complete(v);
        return v;
    } catch (RuntimeException e) {
        mine.completeExceptionally(e);
        throw e;
    } finally {
        inflight.remove(key, mine);
    }
}
```

## 4. CF — 5 dòng hay nhầm

```java
f.thenApply(x -> x+1);                       // map
f.thenCompose(x -> otherApi(x));             // flatMap
a.thenCombine(b, (x,y) -> x+y);              // zip
CompletableFuture.allOf(f1,f2).thenApply(v -> Arrays.asList(f1.join(), f2.join()));
f.exceptionally(ex -> fallback);
// timeout Java 8:
CompletableFuture<T> to = new CompletableFuture<T>();
sched.schedule(() -> to.completeExceptionally(new TimeoutException()), n, ms);
f.applyToEither(to, x -> x);
```

### Exception nhiều stage

```java
// lỗi bếp chảy tới app (pickup/deliver SKIP)
accept().thenApply(o -> cookBoom())
        .thenApply(f -> pickup(f))
        .exceptionally(ex -> "Hoan tien"); // bắt được

// đã đổi món giữa đường → exceptionally cuối KHÔNG chạy
accept().thenApply(o -> cookBoom())
        .exceptionally(ex -> "com-tam")
        .thenApply(f -> pickup(f))         // chạy
        .exceptionally(ex -> "Hoan tien"); // không chạy

// whenComplete không cứu; thenApply cuối không phải catch
f.whenComplete((v, ex) -> log(ex));
f.thenApply(v -> "ok");  // skip nếu fail
f.join();                // mới ném CompletionException
```

## 5. Bounded buffer

```java
synchronized void put(T x) throws InterruptedException {
    while (q.size()==cap) wait();
    q.addLast(x); notifyAll();
}
synchronized T take() throws InterruptedException {
    while (q.isEmpty()) wait();
    T v=q.removeFirst(); notifyAll(); return v;
}
```

## 6. Token bucket

```java
synchronized boolean allow() {
    long now = System.nanoTime();
    tokens = Math.min(cap, tokens + (now-last)/1e9 * perSec);
    last = now;
    if (tokens < 1) return false;
    tokens--; return true;
}
```

## 7. Circuit breaker

```
CLOSED: gọi bình thường; lỗi++ >= N → OPEN, ghi openedAt
OPEN:   now-openedAt < reset → ném; hết hạn → HALF_OPEN
HALF_OPEN: OK → CLOSED + failures=0; lỗi → OPEN lại
```

## 8. Retry + jitter

```java
for (int i=0; i<max; i++) {
    try { return action.call(); }
    catch (Exception e) {
        last=e;
        if (i==max-1) break;
        long w = Math.min(cap, base * (1L << i));
        Thread.sleep((long)(rnd.nextDouble()*w));
    }
}
throw last;
```

## 9. Consistent hash

```java
TreeMap<Integer,String> ring;
void add(String node){ for(int i=0;i<V;i++) ring.put(hash(node+"#"+i), node); }
String get(String key){
    SortedMap<Integer,String> tail = ring.tailMap(hash(key));
    return ring.get(tail.isEmpty() ? ring.firstKey() : tail.firstKey());
}
```

## 10. Union-Find / Trie / Kahn

```java
int find(int x){ return parent[x]==x ? x : (parent[x]=find(parent[x])); }

// trie: Node[26] + boolean end; walk từng char

// kahn: indegree, queue nút 0, xóa cạnh; size==n ? order : cycle
```

## 11. Singleton DCL

```java
private static volatile S inst;
static S get() {
    S x = inst;
    if (x == null) {
        synchronized (S.class) {
            x = inst;
            if (x == null) inst = x = new S();
        }
    }
    return x;
}
```

## 12. Caching proxy / cache-aside

```java
String find(int id) {
    String v = cache.get(id);
    if (v != null) return v;
    return cache.computeIfAbsent(id, inner::find);
}
```

## Checklist 60 giây trước khi nộp

- [ ] `while` không `if` khi `wait`
- [ ] `unlock` / `release` trong `finally`
- [ ] LRU: dummy head/tail, evict **sau** insert khi `size>cap`
- [ ] CF: `thenCompose` khi hàm trả Future
- [ ] timeout hủy `ScheduledFuture`
- [ ] singleflight `remove` inflight trong `finally`
- [ ] `volatile` trên DCL
- [ ] modulo âm: `r<0 ? r+n : r` (không `Math.abs(MIN_VALUE)`)
