# Câu Oracle-style theo chủ đề (1Z0-829 / 1Z0-830)

Đề **tự soạn** theo objective chính thức *Managing Concurrent Code Execution*. Không phải câu từ kỳ thi Oracle.

Làm xong mới mở `<details>`.

---

## 1. Thread API — Runnable, Callable, start, interrupt

### O1. `(một đáp án)`

```java
ExecutorService es = Executors.newSingleThreadExecutor();
Future<Integer> f = es.submit(() -> 1 / 0);
es.shutdown();
```

Gọi `f.get()`:

- A. Return `0`
- B. `ArithmeticException`
- C. `ExecutionException` (cause `ArithmeticException`)
- D. Hang vì `shutdown`

<details><summary>Đáp án O1</summary>

**C.** `Callable` ném unchecked → bọc `ExecutionException`. `shutdown` không hủy task đang/chờ.

</details>

### O2. `(một đáp án)`

```java
Runnable r = () -> System.out.print("A");
Callable<String> c = () -> { System.out.print("B"); return "x"; };
ExecutorService e = Executors.newCachedThreadPool();
e.execute(r);
e.submit(c);
e.submit(r);
```

- A. Compile error: `submit` không nhận `Runnable`
- B. In gồm `A` và `B` (thứ tự không cố định); `submit(r)` trả `Future<?>`
- C. `execute` trả `Future`
- D. `Callable` không được dùng với cached pool

<details><summary>Đáp án O2</summary>

**B.** `submit(Runnable)` tồn tại, `Future.get()` ra `null`.

</details>

### O3. `(nhiều đáp án)` Thread.State

- A. `NEW` — chưa `start`
- B. `RUNNABLE` — đang chạy hoặc sẵn sàng (OS không tách RUN vs ready)
- C. Chờ vào `synchronized` → `WAITING`
- D. `TERMINATED` — `run` return
- E. `BLOCKED` — chờ monitor

<details><summary>Đáp án O3</summary>

**A, B, D, E.** Chờ `synchronized` là `BLOCKED`, không phải `WAITING`.

</details>

### O4. `(một đáp án)`

```java
Thread t = new Thread(() -> {
    while (!Thread.currentThread().isInterrupted()) { }
});
t.start();
t.interrupt();
t.join();
```

- A. Hang vì loop không check
- B. `join` return được vì loop thấy interrupt flag
- C. `interrupt` giết thread ngay
- D. Compile error

<details><summary>Đáp án O4</summary>

**B.** `isInterrupted()` không xóa flag. So với `Thread.interrupted()` (static, **clear** flag).

</details>

### O5. `(một đáp án)` `Callable` vs `Runnable`

- A. `run()` được `throws Exception`
- B. `call()` trả value và được checked exception
- C. `execute(Callable)` là method `Executor`
- D. `Runnable` dùng với `FutureTask` thì không lấy được result

<details><summary>Đáp án O5</summary>

**B.** `Executor.execute` chỉ `Runnable`. `FutureTask` bọc cả hai.

</details>

---

## 2. Locking — synchronized, Lock, deadlock

### O6. `(một đáp án)`

```java
Lock lock = new ReentrantLock();
lock.lock();
try {
    lock.lock();
    work();
} finally {
    lock.unlock();
}
```

- A. Deadlock với chính nó
- B. Hold count = 2 sau `lock` lần 2; `finally` unlock **một** lần → lock vẫn giữ
- C. `unlock` hết mọi hold
- D. Compile error

<details><summary>Đáp án O6</summary>

**B.** Reentrant: mỗi `lock` cần một `unlock`. Thiếu unlock → thread khác block mãi.

</details>

### O7. `(một đáp án)`

```java
public void m() {
    synchronized (lock) {
        lock.wait();
    }
}
```

`lock` là `ReentrantLock` (không phải Object monitor của cùng instance):

- A. OK
- B. `wait` là `Object.wait` — phải synchronized trên **object** `lock`; `ReentrantLock` không thay `wait` (dùng `Condition.await`)
- C. `ReentrantLock` implement `wait` đúng nghĩa lock
- D. Chỉ sai nếu fair lock

<details><summary>Đáp án O7</summary>

**B.** LCK03: đừng synchronized trên high-level lock object. Dùng `lock.newCondition()`.

</details>

### O8. `(nhiều đáp án)` Đề OCP hay cho đoạn “possible deadlock”

```java
void ab() { synchronized (a) { synchronized (b) {} } }
void ba() { synchronized (b) { synchronized (a) {} } }
```

- A. Gọi `ab` từ 1 thread không deadlock
- B. Hai thread `ab` + `ba` *có thể* deadlock
- C. JVM ném `DeadlockException`
- D. `synchronized` lồng luôn deadlock

<details><summary>Đáp án O8</summary>

**A, B.**

</details>

### O9. `(một đáp án)` `synchronized (this)` vs method synchronized

- A. Khác monitor
- B. Instance method `synchronized` ≡ `synchronized (this)` toàn method
- C. `synchronized` method lock `Class` luôn
- D. Subclass override mất lock cha tự động

<details><summary>Đáp án O9</summary>

**B.** `static synchronized` ≡ `synchronized (Cls.class)`. Override không “kế thừa” synchronized trừ khi subclass khai báo lại.

</details>

---

## 3. Visibility — volatile, atomic, parallel safety

### O10. `(một đáp án)`

```java
AtomicInteger a = new AtomicInteger(0);
IntStream.range(0, 1000).parallel().forEach(i -> a.incrementAndGet());
```

- A. Race, kết quả sai
- B. Luôn 1000 (`incrementAndGet` atomic + parallel độc lập)
- C. Parallel stream cấm `AtomicInteger`
- D. Có thể > 1000

<details><summary>Đáp án O10</summary>

**B.** Khác `ArrayList.add` trong parallel `forEach`.

</details>

### O11. `(một đáp án)`

```java
List<Integer> xs = new CopyOnWriteArrayList<>();
IntStream.range(0, 100).parallel().forEach(xs::add);
```

- A. CME chắc chắn
- B. Thread-safe: size 100; **thứ tự không** như 0..99
- C. Giống `ArrayList`
- D. Parallel cấm COWAL

<details><summary>Đáp án O11</summary>

**B.** Write đắt; không CME.

</details>

### O12. `(một đáp án)` `LongAdder`

- A. Thay `synchronized` mọi kiểu
- B. Cộng phân tán, `sum()` không phải snapshot tức thời khi vẫn đang add
- C. Nhanh hơn `AtomicLong` khi **một** thread
- D. `increment` không thread-safe

<details><summary>Đáp án O12</summary>

**B.** Contention cao mới thắng `AtomicLong`. `sum()` weakly consistent.

</details>

### O13. `(nhiều đáp án)` Parallel stream — OCP bẫy

```java
List<String> out = new ArrayList<>();
List.of("a","b","c").parallelStream().map(String::toUpperCase).forEach(out::add);
```

- A. `out` luôn `[A,B,C]`
- B. Race trên `ArrayList`
- C. Nên `.collect(Collectors.toList())` / `toList()`
- D. `forEachOrdered` + `ArrayList` vẫn không đủ nếu nhiều thread add không lock — `forEachOrdered` tuần tự hơn nhưng vẫn không biến `ArrayList` thành concurrent. An toàn: collect.

<details><summary>Đáp án O13</summary>

**B, C.** `forEachOrdered` giảm song song, vẫn **không** phải cách lấy list kết quả. Exam: parallel + shared mutable = sai.

</details>

---

## 4. Concurrent collections

### O14. `(một đáp án)`

```java
Map<String, Integer> m = new ConcurrentHashMap<>();
m.put(null, 1);
```

- A. OK như `HashMap`
- B. `NullPointerException`
- C. Compile error
- D. Key null được, value không

<details><summary>Đáp án O14</summary>

**B.** CHM không `null` key/value. `ConcurrentLinkedQueue` không null. `Hashtable` không null.

</details>

### O15. `(một đáp án)` SkipList map

- A. `ConcurrentSkipListMap` sorted, concurrent
- B. `TreeMap` thread-safe
- C. CHM ordered theo key
- D. `LinkedHashMap` concurrent

<details><summary>Đáp án O15</summary>

**A.** Cần thứ tự + đa luồng → SkipList. CHM không sort.

</details>

### O16. `(một đáp án)` `BlockingQueue.offer(e)` khi đầy (bounded)

- A. Block
- B. Return `false`
- C. Ném `IllegalStateException`
- D. Ghi đè phần tử cũ

<details><summary>Đáp án O16</summary>

**B.** `add` ném `IllegalStateException`. `put` block. `offer(e,time,unit)` timeout.

</details>

---

## 5. Executors — factory, reject, scheduled

### O17. `(một đáp án)` `newSingleThreadExecutor()`

- A. Queue bounded 1
- B. Một worker; task tuần tự; queue unbounded
- C. Fail nếu `execute` lúc worker bận
- D. Daemon mặc định

<details><summary>Đáp án O17</summary>

**B.**

</details>

### O18. `(nhiều đáp án)` `Future.isDone()` true khi

- A. Task xong bình thường
- B. Task ném exception
- C. `cancel` thành công
- D. Mới `submit`, chưa chạy

<details><summary>Đáp án O18</summary>

**A, B, C.**

</details>

### O19. `(một đáp án)` `invokeAll(collection)` 

- A. Return khi **mọi** task xong (hoặc timeout overload)
- B. Return task đầu tiên xong
- C. Không block
- D. Chỉ `Runnable`

<details><summary>Đáp án O19</summary>

**A.** `invokeAny` return kết quả **một** task thành công, hủy cái còn lại.

</details>

### O20. `(một đáp án)` Core pool 2, queue 1, max 4, `AbortPolicy`. Submit 6 Runnable CPU-bound chậm:

- A. Cả 6 chạy
- B. Một số chạy, vượt queue+max → `RejectedExecutionException`
- C. Queue unbounded nên không reject
- D. Tự đợi mãi

<details><summary>Đáp án O20</summary>

**B.** ThreadPoolExecutor: tạo core, rồi queue, rồi max, rồi handler. 2 running + 1 queued + 2 extra = 5; task 6 reject (số chính xác phụ thuộc timing; ý exam: saturation → reject). Đếm: first 2 start, 3rd queue, 4th–5th tạo thread tới max 4 (2 core + 2 extra) và 1 queue = 5 tasks accepted, 6th abort.

</details>

---

## 6. Synchronizers

### O21. `(một đáp án)` 3 thread phải gặp nhau rồi mới đi tiếp, lặp 10 round

- A. `CountDownLatch(3)` một lần — không reset
- B. `CyclicBarrier(3)` `await` mỗi round
- C. `volatile` flag
- D. `join` 3 lần

<details><summary>Đáp án O21</summary>

**B.** Latch không cyclic (trừ tự tạo mới mỗi round).

</details>

### O22. `(một đáp án)` `CyclicBarrier` party chưa đủ, một thread `reset()`

- A. Im lặng
- B. Thread đang `await` nhận `BrokenBarrierException`
- C. Barrier tự đầy
- D. Deadlock chắc

<details><summary>Đáp án O22</summary>

**B.** Broken barrier; các party khác cũng fail.

</details>

### O23. `(một đáp án)` `Semaphore(3).acquire(4)` (không timeout)

- A. Return ngay
- B. Block mãi nếu không ai `release` đủ
- C. Ném exception luôn
- D. Tự `release(1)`

<details><summary>Đáp án O23</summary>

**B.** `tryAcquire(4)` return false.

</details>

---

## 7. Virtual threads (1Z0-830)

### O24. `(nhiều đáp án)`

- A. `Thread.ofVirtual().unstarted(runnable).start()`
- B. `Executors.newVirtualThreadPerTaskExecutor()`
- C. Nên pool 200 virtual thread “cho chắc”
- D. Pin khi `synchronized` lâu (tùy JDK)

<details><summary>Đáp án O24</summary>

**A, B, D.** Không pool virtual như platform; một task một virtual thread.

</details>

### O25. `(một đáp án)` Platform vs virtual

- A. `Thread.ofPlatform().name("w").start(r)`
- B. `new Thread(r)` tạo virtual mặc định từ Java 21
- C. Virtual không `join` được
- D. Virtual cấm `sleep`

<details><summary>Đáp án O25</summary>

**A.** `new Thread(r)` vẫn platform. Virtual `join`/`sleep` bình thường (sleep unmount).

</details>

---

## 8. Predict output (câu chiếm nhiều điểm OCP)

### O26. `(nhiều đáp án)` Có thể in gì?

```java
var t1 = new Thread(() -> System.out.print("1"));
var t2 = new Thread(() -> System.out.print("2"));
t1.start();
t2.start();
System.out.print("0");
```

- A. `012`
- B. `102`
- C. `120`
- D. `210`
- E. `021`
- F. `201`

<details><summary>Đáp án O26</summary>

**Tất cả A–F.** Không `join` → 3 print độc lập. Mọi permutation.

</details>

### O27. `(một đáp án)`

```java
List<Integer> syn = Collections.synchronizedList(new ArrayList<>());
syn.add(1);
ExecutorService e = Executors.newFixedThreadPool(2);
e.execute(() -> syn.add(2));
e.execute(() -> {
    synchronized (syn) {
        syn.forEach(System.out::print);
    }
});
e.shutdown();
e.awaitTermination(1, TimeUnit.MINUTES);
```

- A. Iterate không lock → luôn CME
- B. Iterate **có** `synchronized (syn)` đúng Javadoc; output `1` hoặc `12` (thứ tự add vs iterate)
- C. Luôn `12`
- D. `synchronizedList` cấm `forEach`

<details><summary>Đáp án O27</summary>

**B.**

</details>

### O28. `(một đáp án)`

```java
Path p = Path.of("a.txt");
Files.writeString(p, "x");
var cf = CompletableFuture.runAsync(() -> {
    try { Files.writeString(p, "y"); } catch (Exception e) { throw new RuntimeException(e); }
});
cf.join();
System.out.print(Files.readString(p));
```

- A. `x` hoặc `y` không xác định
- B. Luôn `y` vì `join` happens-before tiếp main
- C. `xy`
- D. Race file nên exception

<details><summary>Đáp án O28</summary>

**B.** `join` HB; write trong task xong trước khi main đọc.

</details>

---

Ôn thêm: [trac-nghiem.md](trac-nghiem.md), [tinh-huong.md](tinh-huong.md). Nguồn hợp pháp: Enthuware, sách Wiley OCP.
