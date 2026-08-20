# Tình huống khó — đọc code, đoán hành vi

Không mở đáp án trước. Nhiều câu **không có một output duy nhất**; phải nói *tập output hợp lệ* và *bug*.

---

## S1. Visibility không volatile

```java
class Loop {
    static boolean run = true;
    public static void main(String[] args) {
        new Thread(() -> { while (run) {} System.out.println("done"); }).start();
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        run = false;
    }
}
```

Hỏi: Thread worker có **chắc** in `done` không? Vì sao? Sửa tối thiểu?

<details><summary>Đáp án S1</summary>

Không chắc. `run` không `volatile`, không lock: JIT có thể hoist `while (run)` thành `if (run) while (true)`. Main sleep **không** tạo happens-before tới worker. Sửa: `volatile boolean run`, hoặc `AtomicBoolean`, hoặc cả hai phía `synchronized` cùng lock.

</details>

---

## S2. Compound trên Atomic

```java
AtomicInteger n = new AtomicInteger(0);

// 100 threads, mỗi thread:
if (n.get() < 10) {
    n.incrementAndGet();
}
```

Hỏi: `n` cuối ≤ 10 luôn? Có thể > 10?

<details><summary>Đáp án S2</summary>

Có thể > 10. Nhiều thread cùng thấy `get() == 9`, rồi tất cả increment. Sửa: `n.updateAndGet(v -> v < 10 ? v + 1 : v)` hoặc CAS loop, hoặc lock.

</details>

---

## S3. Lock lẻ

```java
class Account {
    int bal;
    synchronized void deposit(int x) { bal += x; }
    int get() { return bal; }          // không synchronized
    synchronized void transfer(Account to, int x) {
        bal -= x;
        to.deposit(x);
    }
}
```

Hỏi hết bug: visibility, deadlock, atomicity.

<details><summary>Đáp án S3</summary>

1. `get()` không synchronized / không volatile → đọc `bal` stale.
2. `transfer` lock `this` rồi gọi `to.deposit` lock `to` → hai account A.transfer(B) và B.transfer(A) **deadlock**.
3. `bal += x` trong deposit OK vì synchronized, nhưng transfer không atomic với `get` của thread khác trên cả hai account (inconsistent snapshot).
Sửa deadlock: lock order theo `identityHashCode`, hoặc một lock toàn cục, hoặc `tryLock` timeout.

</details>

---

## S4. Wait sai object

```java
final Object a = new Object();
final Object b = new Object();
synchronized (a) {
    b.wait();
}
```

Hỏi exception / hành vi.

<details><summary>Đáp án S4</summary>

`IllegalMonitorStateException`: `wait` phải trên monitor đang hold — đang hold `a` nhưng `wait` trên `b`.

</details>

---

## S5. Notify trước wait (lost signal)

```java
boolean ready = false;
synchronized (lock) {
    // T1:
    if (!ready) lock.wait();
}
synchronized (lock) {
    // T2:
    ready = true;
    lock.notify();
}
```

T2 có thể chạy **trước** T1. Kết quả?

<details><summary>Đáp án S5</summary>

Nếu T2 notify trước khi T1 wait, notify **mất**. T1 vào wait mãi (nếu không timeout). Sửa: T1 `while (!ready) wait();` — nếu T2 đã set ready, T1 không wait. Predicate + lock là bắt buộc, không wait “trần”.

</details>

---

## S6. start trong constructor + subclass

```java
class Base {
    Base() {
        new Thread(this::hello).start();
    }
    void hello() { System.out.println("base"); }
}
class Sub extends Base {
    int k = 1;
    void hello() { System.out.println(k); }
}
new Sub();
```

Output có thể?

<details><summary>Đáp án S6</summary>

Worker có thể in `0` (default `int`) vì `hello` override chạy **trước** khi `Sub` gán `k = 1`. Đây là this-escape (TSM01). Có thể in `1` nếu constructor Sub kịp xong. Không được start thread / publish `this` trong constructor.

</details>

---

## S7. ConcurrentHashMap compute

```java
ConcurrentHashMap<String, Integer> m = new ConcurrentHashMap<>();
m.put("a", 1);
// thread 1
m.compute("a", (k, v) -> { sleep(1000); return v + 1; });
// thread 2 ngay lập tức
m.compute("a", (k, v) -> v + 1);
```

`m.get("a")` sau cả hai xong? Có deadlock với compute lồng nhau không?

<details><summary>Đáp án S7</summary>

Hai `compute` cùng key **serialize** trên bin/node: kết quả `3` (1+1+1), không mất update. **Không** gọi `compute`/`put` lồng trên cùng map từ trong lambda `compute` — có thể deadlock (Javadoc CHM). Side effect lâu trong `compute` chặn updater khác cùng key.

</details>

---

## S8. HashMap iterate + put

```java
Map<Integer, Integer> map = new HashMap<>();
map.put(1, 1);
Thread t = new Thread(() -> {
    for (int i = 2; i < 100_000; i++) map.put(i, i);
});
t.start();
for (Integer k : map.keySet()) { /* ... */ }
t.join();
```

Hỏi exception / hang.

<details><summary>Đáp án S8</summary>

Iterator fail-fast: `ConcurrentModificationException` **có thể**. Trước Java 8, resize đa luồng có thể **infinite loop**. Không bao giờ share `HashMap` mutable không lock.

</details>

---

## S9. Pool + interrupt

```java
ExecutorService ex = Executors.newSingleThreadExecutor();
Future<?> f = ex.submit(() -> {
    try { Thread.sleep(10_000); }
    catch (InterruptedException e) { /* nuốt, không restore */ }
});
f.cancel(true);
ex.shutdown();
```

Task có dừng không? Pool tái sử dụng thread sau đó thế nào?

<details><summary>Đáp án S9</summary>

`cancel(true)` interrupt worker. `sleep` ném `InterruptedException` và **clear flag**. Nuốt exception → task kết thúc “bình thường”, cờ interrupt mất. Task sau trên cùng worker **không** thấy interrupt (tốt nếu clear; xấu nếu bạn tưởng thread vẫn interrupted). Best practice: restore `Thread.currentThread().interrupt()` hoặc return sớm có chủ đích. `shutdown()` không interrupt; cần `shutdownNow` nếu muốn dừng sleep đang chạy **sau** khi không cancel.

</details>

---

## S10. Double lock order cố định?

```java
void transfer(Object from, Object to) {
    synchronized (from) {
        synchronized (to) {
            /* move */
        }
    }
}
```

`transfer(a,b)` song song `transfer(a,b)` — deadlock? `transfer(a,b)` vs `transfer(b,a)`?

<details><summary>Đáp án S10</summary>

Cùng thứ tự `a` rồi `b`: không circular wait. Thứ tự ngược: có thể deadlock. Sửa: luôn lock pointer nhỏ hơn `identityHashCode` trước; nếu hash bằng nhau, tie-break lock thứ 3.

</details>

---

## S11. Fairness vs barge

```java
ReentrantLock lock = new ReentrantLock(); // unfair
```

T1 giữ lock lâu. T2, T3 chờ. T1 unlock, T4 vừa `lock()`:

Hỏi T4 có thể nhảy trước T2 không?

<details><summary>Đáp án S11</summary>

Có. Unfair cho phép barging → throughput cao, starvation có thể. `new ReentrantLock(true)` giảm barge.

</details>

---

## S12. Parallel reduce sai

```java
int[] acc = {0};
IntStream.range(0, 1000).parallel().forEach(i -> acc[0] += i);
```

Kết quả vs `IntStream.range(0,1000).sum()`?

<details><summary>Đáp án S12</summary>

`+=` trên `acc[0]` race; thiếu visibility. Phải `sum()`, `reduce`, `AtomicInteger`, hoặc collect. Parallel + shared mutable là anti-pattern OCP.

</details>

---

## S13. CompletableFuture thread

```java
CompletableFuture<Void> f = CompletableFuture
    .runAsync(() -> sleep(50))
    .thenRun(() -> System.out.println(Thread.currentThread().getName()));
f.join();
```

Thread in ra có thể là gì?

<details><summary>Đáp án S13</summary>

Thường worker của `ForkJoinPool.commonPool()` (`ForkJoinPool.commonPool-worker-N`). Nếu stage trước đã complete lúc `thenRun` đăng ký, callback có thể chạy trên **thread gọi `thenRun`**. `thenRunAsync` đẩy sang executor rõ ràng.

</details>

---

## S14. CopyOnWrite iterator snapshot

```java
CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<>(List.of(1, 2));
Iterator<Integer> it = list.iterator();
list.add(3);
it.forEachRemaining(System.out::print);
```

In gì?

<details><summary>Đáp án S14</summary>

`12` — iterator là snapshot lúc tạo, không thấy `add` sau.

</details>

---

## S15. SynchronizedList addAll vs iterate

Thread 1 iterate (không lock ngoài). Thread 2 `addAll` lớn.

Hỏi CME? Data race?

<details><summary>Đáp án S15</summary>

`synchronizedList` synchronize **từng** method; iterator vẫn fail-fast. Iterate không lock → `ConcurrentModificationException` hoặc traverse không nhất quán. Phải `synchronized (list) { iterate }`.

</details>

---

## S16. Daemon vs shutdown hook

Main start daemon worker loop vô hạn, main return.

Hỏi worker có chạy nốt không? So với non-daemon?

<details><summary>Đáp án S16</summary>

Chỉ còn daemon → JVM thoát, worker **bị cắt**, có thể giữa chừng ghi file. Non-daemon giữ JVM sống. Shutdown hook chạy khi còn non-daemon kết thúc / `System.exit`, **không** phải lúc chỉ còn daemon (JVM đã quyết định exit). Đừng dựa daemon để flush quan trọng.

</details>

---

## S17. Happens-before qua volatile khác biến

```java
int a = 0;
volatile boolean g = false;
// T1
a = 1;
g = true;
// T2
if (g) System.out.print(a);
```

T2 in `1` luôn nếu vào nhánh `if`? In `0` được không?

<details><summary>Đáp án S17</summary>

Write `g` HB read `g`. Mọi action T1 **trước** write `g` (gồm `a=1`) HB với T2 sau read `g`. Nếu T2 thấy `g==true` thì thấy `a==1`. Nếu `g==false` thì không print. Không print `0` khi đã vào `if`. (Đây là “volatile piggyback”.)

</details>

---

## S18. StampedLock optimistic đọc

```java
long stamp = sl.tryOptimisticRead();
int x = data.x, y = data.y;
if (!sl.validate(stamp)) {
    stamp = sl.readLock();
    try { x = data.x; y = data.y; }
    finally { sl.unlockRead(stamp); }
}
```

Hỏi: trong đoạn optimistic, có được ghi `data` không? Nếu writer xen, `validate` làm gì?

<details><summary>Đáp án S18</summary>

Optimistic **không** chặn writer. Có thể đọc `x`,`y` torn (x mới y cũ). `validate == false` → phải đọc lại dưới read lock. Không gọi method có side effect / không assume invariant trong cửa sổ optimistic.

</details>

---

## S19. ForkJoin blocking

```java
ForkJoinPool.commonPool().submit(() -> {
    CompletableFuture<String> other = CompletableFuture.supplyAsync(() -> "z");
    return other.join(); // block
}).join();
```

Hỏi: có thể hang trên máy ít core không?

<details><summary>Đáp án S19</summary>

Có rủi ro **pool exhaustion**: worker commonPool block `join`, task `supplyAsync` cũng cần commonPool. Parallelism nhỏ (1–2) dễ deadlock-like stall. Sửa: `ManagedBlocker`, executor riêng cho I/O, virtual thread, `get()` trên pool khác. Java có thể compensate một phần, **đừng** dựa vào đó.

</details>

---

## S20. Livelock retry

Hai thread:

```
while (true) {
  if (lockA.tryLock()) {
    try {
      if (lockB.tryLock()) { work(); return; }
    } finally { lockA.unlock(); }
  }
}
```

Đối xứng thread kia `lockB` rồi `lockA`. Hỏi deadlock hay livelock?

<details><summary>Đáp án S20</summary>

Không circular wait bền: cả hai liên tục acquire/release → **livelock** có thể (đặc biệt cùng backoff 0). Sửa: lock order cố định, hoặc random backoff, hoặc `tryLock(timeout)`.

</details>

---

## S21. `join` vs interrupt main

Main `t.join()`. Thread khác interrupt main. `t` vẫn chạy. Main sau catch?

<details><summary>Đáp án S21</summary>

`join` ném `InterruptedException`, **không** giết `t`. Main phải quyết định: restore interrupt, `join` lại, hoặc `t.interrupt()`.

</details>

---

## S22. Publication không an toàn

```java
class Holder { int n; Holder(int n) { this.n = n; } }
Holder h; // shared, không volatile
// T1
h = new Holder(42);
// T2
if (h != null) System.out.print(h.n);
```

T2 có thể in `0`?

<details><summary>Đáp án S22</summary>

Có (JMM): T2 thấy `h != null` nhưng `n` chưa flush (data race). Sửa: `volatile Holder h`, hoặc `synchronized` cả hai phía, hoặc `final int n` + không leak this (final field freeze) **và** an toàn publish reference (`volatile`/lock/static init). Chỉ `final` mà publish data race reference vẫn là territory phức tạp — safest: `volatile` reference hoặc immutable + safe publication.

</details>

---

## S23. Thread.holdsLock

```java
synchronized void a() {
    new Thread(() -> {
        synchronized (Outer.this) { }
    }).start();
}
```

Thread con có inherit lock không? Có vào `synchronized (Outer.this)` khi cha chưa thoát `a` không?

<details><summary>Đáp án S23</summary>

Lock **không** inherit. Thread con block cho đến khi cha nhả `this` (nếu con lock cùng object). Deadlock nếu cha `join` con trong `a()` khi con cần cùng lock.

</details>

---

## S24. Nested deadlock join

```java
synchronized void a() {
    Thread t = new Thread(() -> b());
    t.start();
    t.join();
}
synchronized void b() { }
```

Cùng instance, gọi `a()` từ main.

<details><summary>Đáp án S24</summary>

Deadlock: `a` giữ `this`, `join` chờ `t`; `t` gọi `b()` cần `this`. Cổ điển.

</details>

---

## S25. `BlockingQueue.offer` vs `put`

Producer không block được: queue đầy. Dùng `put` hay `offer`? Consumer `take` vs `poll`?

<details><summary>Đáp án S25</summary>

`put`/`take`: block. `offer`/`poll`: không block, `false`/`null`. `offer(e, time, unit)` timeout. Exam hay tráo. Bounded + `add` ném `IllegalStateException` khi đầy.

</details>

---

Tiếp theo: [bai-tap.md](bai-tap.md) — **viết code**, không chỉ đoán.
