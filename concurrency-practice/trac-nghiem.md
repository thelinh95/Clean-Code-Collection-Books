# Câu hỏi trắc nghiệm concurrency

Làm như đề: **đọc code, chọn đáp án, rồi mới mở đáp án**. Nhiều câu có hơn một output hợp lệ — đây là bẫy exam.

Ký hiệu: `(một đáp án)` hoặc `(nhiều đáp án)`.

---

## A. Thread cơ bản (kiểu oDesk / Upwork / OCA)

### A1. `start()` vs `run()` `(một đáp án)`

```java
class Job extends Thread {
    public void run() { System.out.print("R"); }
}
public class Main {
    public static void main(String[] args) {
        Job t = new Job();
        t.run();
        t.run();
        t.start();
    }
}
```

Chọn mô tả đúng:

- A. In `RRR`, luôn có 3 thread mới
- B. In `RR` rồi một thread mới in `R`; hai lần `run()` chạy trên thread `main`
- C. Compile error vì `run()` không được gọi trực tiếp
- D. `start()` lần hai ném `IllegalThreadStateException` vì `run()` đã chạy

<details><summary>Đáp án A1</summary>

**B.** `run()` là method thường: gọi trực tiếp không tạo thread. `start()` mới đăng ký thread mới rồi JVM gọi `run()`. Không được `start()` hai lần trên cùng instance (sẽ `IllegalThreadStateException`), nhưng ở đây `start()` chỉ một lần.

</details>

### A2. Process và thread `(nhiều đáp án)`

Chọn câu đúng:

- A. Thread tồn tại trong process
- B. Mỗi process có ít nhất một thread
- C. Một thread có thể thuộc nhiều process cùng lúc
- D. Thread và process luôn có không gian địa chỉ riêng biệt, không chia sẻ gì

<details><summary>Đáp án A2</summary>

**A, B.** Thread của cùng process chia sẻ heap. Một thread không thuộc hai process. D sai vì thread trong cùng process chia sẻ memory.

</details>

### A3. `sleep` vs `wait` `(một đáp án)`

- A. Cả hai đều nhả monitor đang giữ
- B. `sleep` nhả lock; `wait` không nhả lock
- C. `sleep` không nhả lock của `synchronized`; `wait` nhả monitor rồi chờ `notify`/`notifyAll` (hoặc timeout)
- D. `wait(ms)` luôn chính xác theo millisecond trên mọi OS

<details><summary>Đáp án A3</summary>

**C.** `Thread.sleep` không liên quan monitor (và không đòi phải đang giữ lock). `Object.wait` **bắt buộc** đang giữ monitor của object đó; khi wait thì nhả monitor.

</details>

### A4. Gọi `wait()` sai chỗ `(một đáp án)`

```java
public synchronized void hang() {
    try { Thread.sleep(1000); } catch (InterruptedException e) {}
}
```

và

```java
public void hang2() {
    try { wait(); } catch (InterruptedException e) {}
}
```

- A. Cả hai hợp lệ
- B. `hang` ném `IllegalMonitorStateException`
- C. `hang2` ném `IllegalMonitorStateException` (không giữ monitor)
- D. `sleep` trong `synchronized` là compile error

<details><summary>Đáp án A4</summary>

**C.** `wait()`/`notify()` phải được gọi khi thread đang own monitor (`synchronized` trên cùng object, hoặc `lock.lock()` không thay cho intrinsic wait). `sleep` trong `synchronized` hoàn toàn hợp lệ — và **giữ lock suốt lúc ngủ** (thường là ý xấu).

</details>

### A5. `join` `(một đáp án)`

```java
Thread t = new Thread(() -> System.out.print("A"));
t.start();
t.join();
System.out.print("B");
```

- A. Luôn `AB`
- B. Luôn `BA`
- C. `AB` hoặc `BA`
- D. Compile error: `join` không khai báo checked exception

<details><summary>Đáp án A5</summary>

**A** nếu `join()` được gọi (và không interrupt). `join` tạo happens-before: mọi action trong `t` hoàn tất trước khi `join` return. **D:** `join()` ném `InterruptedException` — code trên phải nằm trong `try` hoặc method `throws`; nếu thiếu thì **không compile**. Đề exam hay để sót `throws`. Nếu giả sử đã handle exception thì output luôn `AB`.

Đáp án exam nếu code **không** `throws`/`try`: **D**. Nếu đã bọc exception: **A**.

</details>

### A6. Interrupt `(nhiều đáp án)`

```java
t.interrupt();
```

khi `t` đang `sleep`/`wait`/`join`:

- A. Ném `InterruptedException` trong `t`, và status interrupt bị **xóa**
- B. `t.isInterrupted()` luôn `true` sau khi exception được ném
- C. Nếu `t` không ở trạng thái waiting, chỉ set interrupt status, không ném ngay
- D. `Thread.stop()` là cách khuyến nghị để hủy thread

<details><summary>Đáp án A6</summary>

**A, C.** Khi ném `InterruptedException`, JVM clear flag. Phải tự `Thread.currentThread().interrupt()` nếu muốn truyền cờ đi tiếp. `Thread.stop()` deprecated/removed vì phá vỡ invariant.

</details>

### A7. Daemon `(một đáp án)`

- A. JVM thoát khi chỉ còn daemon thread
- B. JVM không bao giờ thoát nếu còn daemon
- C. `setDaemon(true)` gọi **sau** `start()` vẫn được
- D. Daemon thread tự join các non-daemon khi shutdown

<details><summary>Đáp án A7</summary>

**A.** `setDaemon` phải gọi **trước** `start()`, không thì `IllegalThreadStateException`.

</details>

### A8. Thread state `(một đáp án)`

`Thread.sleep(1000)` đưa thread sang trạng thái nào trong `Thread.State`?

- A. `BLOCKED`
- B. `WAITING`
- C. `TIMED_WAITING`
- D. `NEW`

<details><summary>Đáp án A8</summary>

**C.** `sleep`, `wait(timeout)`, `join(timeout)`, `LockSupport.parkNanos` → `TIMED_WAITING`. `wait()` không timeout → `WAITING`. `BLOCKED` = chờ vào `synchronized`.

</details>

---

## B. `synchronized`, lock object, bẫy monitor

### B1. Hai lock khác object `(một đáp án)`

```java
class C {
    int n;
    public synchronized void inc() { n++; }
    public void dec() { synchronized (new Object()) { n--; } }
}
```

Hai thread gọi `inc` và `dec` cùng lúc trên **cùng instance**:

- A. Thread-safe vì cả hai đều synchronized
- B. `dec` không đồng bộ với `inc` (lock khác nhau); race trên `n`
- C. Compile error
- D. `n` là atomic nên không sao

<details><summary>Đáp án B1</summary>

**B.** `inc` lock `this`. `dec` lock object mới mỗi lần — vô dụng. `n++`/`n--` không atomic.

</details>

### B2. `static synchronized` vs instance `(một đáp án)`

```java
class S {
    static int x;
    synchronized void a() { x++; }
    static synchronized void b() { x++; }
}
```

- A. `a` và `b` loại trừ lẫn nhau
- B. `a` lock instance, `b` lock `S.class` — hai thread có thể vào `a` và `b` đồng thời
- C. `static synchronized` lock `this`
- D. `x++` atomic vì `int`

<details><summary>Đáp án B2</summary>

**B.** Đây là bẫy kinh điển OCP.

</details>

### B3. Không được lock cái gì? `(nhiều đáp án)`

Theo SEI CERT / thực tế exam, **tránh** `synchronized` trên:

- A. `this` khi class có thể bị subclass / client lock cùng object
- B. String intern (`synchronized ("LOCK")`)
- C. `Boolean.TRUE` / `Integer.valueOf(1)`
- D. `private final Object lock = new Object();`

<details><summary>Đáp án B3</summary>

**A, B, C.** String intern và boxed cache là object **dùng chung JVM** → deadlock ẩn với thư viện khác. D là pattern đúng (LCK00/LCK01).

</details>

### B4. Reentrant `(một đáp án)`

```java
synchronized void outer() { inner(); }
synchronized void inner() { }
```

Gọi `outer()` trên cùng instance:

- A. Deadlock ngay vì `inner` cần lock đang bị `outer` giữ
- B. OK: intrinsic lock là reentrant
- C. Chỉ OK nếu `inner` là `static`
- D. OK chỉ với `ReentrantLock`, không với `synchronized`

<details><summary>Đáp án B4</summary>

**B.** Cùng thread vào lại cùng monitor được. `ReentrantLock` cũng reentrant (đúng như tên).

</details>

### B5. `wait` phải trong loop `(một đáp án)`

Vì sao `while (!ready) wait();` chứ không `if (!ready) wait();`?

- A. `wait` không nhả lock
- B. Spurious wakeup + điều kiện có thể lại false sau `notify`
- C. `notify` luôn đánh thức đúng thread
- D. Loop làm `wait` thành `sleep`

<details><summary>Đáp án B5</summary>

**B.** JLS cho phép spurious wakeup. `notify` không gửi “lý do”; phải re-check predicate.

</details>

### B6. `notify` vs `notifyAll` `(một đáp án)`

Bounded buffer, nhiều producer và consumer, `wait` trên cùng lock:

- A. `notify()` luôn đủ
- B. `notifyAll()` an toàn hơn khi có nhiều điều kiện wait khác nhau (full vs empty)
- C. `notifyAll` chỉ đánh thức daemon
- D. Không cần lock khi `notify`

<details><summary>Đáp án B6</summary>

**B.** `notify()` có thể đánh thức nhầm phía (producer khi đang full) → missed signal. `notifyAll` + loop là mặc định an toàn.

</details>

---

## C. Visibility, `volatile`, atomic, JMM

### C1. `volatile` làm gì? `(nhiều đáp án)`

- A. Đảm bảo visibility: đọc thấy write mới nhất của biến đó
- B. Tạo happens-before giữa write và read sau đó của cùng biến volatile
- C. Làm `i++` atomic
- D. Thay thế hoàn toàn `synchronized` trong mọi trường hợp

<details><summary>Đáp án C1</summary>

**A, B.** `volatile` **không** atomic với compound action (`i++` = read-modify-write).

</details>

### C2. Stop flag `(một đáp án)`

```java
boolean stop = false;
// thread 1
while (!stop) { work(); }
// thread 2
stop = true;
```

- A. Thread 1 luôn dừng
- B. Thread 1 có thể **không bao giờ** thấy `stop == true` (cache / compiler reorder)
- C. `boolean` luôn atomic và visible
- D. Chỉ sai nếu `stop` là `static`

<details><summary>Đáp án C2</summary>

**B.** Cần `volatile` (hoặc lock, hoặc `AtomicBoolean`). Ghi 32-bit `boolean` là atomic về mặt tear, **không** nghĩa là visible.

</details>

### C3. `count++` `(một đáp án)`

```java
volatile int count;
void add() { count++; }
```

1000 thread, mỗi thread `add()` 1 lần. `count` cuối cùng:

- A. Luôn 1000
- B. Có thể < 1000
- C. Có thể > 1000
- D. Compile error: `volatile` không dùng với `int`

<details><summary>Đáp án C3</summary>

**B.** Lost update. `volatile` chỉ visibility cho từng read/write riêng, không atomic RMW. Dùng `AtomicInteger.incrementAndGet()` hoặc lock.

</details>

### C4. `AtomicInteger` vs lock `(một đáp án)`

Hai thread: `if (ai.get() == 0) ai.set(1);`

- A. Thread-safe vì mỗi method atomic
- B. Check-then-act: cả hai có thể thấy 0 rồi cả hai set 1 — race
- C. `get` không atomic
- D. Chỉ sai trên 32-bit JVM

<details><summary>Đáp án C4</summary>

**B.** VNA03: nhóm lời gọi atomic **không** tạo thành một hành động atomic. Dùng `compareAndSet(0, 1)` / `updateAndGet`.

</details>

### C5. `long` / `double` 64-bit `(một đáp án)`

Không `volatile`, không lock, ghi `long` trên implementation cho phép tear:

- A. Reader luôn thấy 0 hoặc giá trị mới đủ 64-bit
- B. Reader có thể thấy “nửa cũ nửa mới”
- C. Java cấm `long` share giữa thread
- D. Chỉ `double` bị tear, `long` không

<details><summary>Đáp án C5</summary>

**B.** VNA05 / JLS: 64-bit non-volatile write có thể không atomic. `volatile long`/`double` hoặc lock.

</details>

### C6. Happens-before `(nhiều đáp án)`

Cái nào **tạo** happens-before?

- A. Unlock monitor M happens-before lock sau đó trên M
- B. Write volatile v happens-before read sau đó của v
- C. `t.start()` happens-before hành động đầu trong `t`
- D. `Thread.sleep` happens-before thread khác đọc biến thường
- E. Kết thúc thread `t` happens-before `t.join()` return trên thread khác

<details><summary>Đáp án C6</summary>

**A, B, C, E.** `sleep` **không** tạo HB với thread khác. Program order trong **một** thread cũng là HB.

</details>

### C7. Double-checked locking `(một đáp án)`

```java
private static Helper h;
static Helper get() {
    if (h == null) {
        synchronized (Helper.class) {
            if (h == null) h = new Helper();
        }
    }
    return h;
}
```

- A. An toàn trên mọi JMM
- B. Unsafe: publish object chưa khởi tạo xong (thiếu `volatile` trên `h`)
- C. Unsafe chỉ vì `synchronized` trên class
- D. `new Helper()` là atomic publish

<details><summary>Đáp án C7</summary>

**B.** LCK10: field phải `volatile` (hoặc dùng holder idiom / enum). Không `volatile`, reader qua nhánh `h != null` có thể thấy reference != null nhưng field trong `Helper` vẫn default.

</details>

### C8. Safe DCL `(một đáp án)`

Cách lazy singleton **an toàn** và đơn giản nhất thường khuyến nghị:

- A. DCL không `volatile`
- B. Initialization-on-demand holder (`private static class Holder { static final T I = new T(); }`)
- C. `ThreadLocal`
- D. `HashMap` cache

<details><summary>Đáp án C8</summary>

**B.** Class init được JVM đồng bộ. Enum singleton cũng an toàn. DCL + `volatile` đúng nhưng dễ viết sai.

</details>

### C9. `this` escape `(một đáp án)`

```java
class Ev {
    Ev() {
        new Thread(() -> System.out.println(this.toString())).start();
    }
}
```

- A. OK vì constructor chạy xong trước `start`
- B. `this` publish khi object **chưa** khởi tạo xong (TSM01)
- C. Chỉ sai nếu có subclass
- D. `start` trong constructor luôn deadlock

<details><summary>Đáp án C9</summary>

**B.** Thread mới có thể chạy trước khi constructor return (đặc biệt khi có subclass field chưa gán). Đừng đăng ký listener / start thread với `this` trong constructor.

</details>

### C10. Immutable `(một đáp án)`

Object chỉ có `final` field, không leak `this` lúc construct, không mutator:

- A. Vẫn cần `synchronized` mọi getter
- B. Thread-safe khi share reference **sau khi** construction xong (safe publication)
- C. `final` không liên quan JMM
- D. Phải `volatile` mọi field `final`

<details><summary>Đáp án C10</summary>

**B.** JLS: freeze `final` field khi constructor kết thúc. Vẫn cần không leak `this` và không mutate (kể cả object lồng nhau).

</details>

---

## D. Concurrent collections và parallel stream

### D1. `HashMap` đa luồng `(một đáp án)`

Nhiều thread `put` cùng `HashMap` không synchronize:

- A. Chỉ chậm hơn
- B. Có thể infinite loop / corrupt (đặc biệt resize, Java 7 linked list) hoặc mất data
- C. `HashMap` thread-safe từ Java 8
- D. `put` atomic

<details><summary>Đáp án D1</summary>

**B.** Dùng `ConcurrentHashMap` hoặc lock ngoài.

</details>

### D2. `ConcurrentHashMap` `(nhiều đáp án)`

- A. Iterator weakly consistent, không `ConcurrentModificationException` vì structural change của thread khác
- B. `size()` luôn snapshot chính xác tuyệt đối mọi lúc
- C. Compound: `if (!map.containsKey(k)) map.put(k,v)` vẫn race — dùng `putIfAbsent`/`computeIfAbsent`
- D. Không cho `null` key/value (khác `HashMap`)

<details><summary>Đáp án D2</summary>

**A, C, D.** `size` là ước lượng / không phải atomic snapshot của toàn map.

</details>

### D3. `CopyOnWriteArrayList` phù hợp khi `(một đáp án)`

- A. Rất nhiều write
- B. Nhiều read, ít write (ví dụ listener list)
- C. Cần random access O(1) write rẻ
- D. Thay `ArrayList` mọi chỗ

<details><summary>Đáp án D3</summary>

**B.** Mỗi write copy cả mảng.

</details>

### D4. `Collections.synchronizedList` `(một đáp án)`

```java
List<E> list = Collections.synchronizedList(new ArrayList<>());
for (E e : list) { process(e); }
```

- A. An toàn, for-each đã lock
- B. Phải `synchronized (list) { for (...) }` khi iterate
- C. Iterator fail-fast nên an toàn
- D. `synchronizedList` cấm iterate

<details><summary>Đáp án D4</summary>

**B.** Compound iteration không atomic. Javadoc bắt lock trên list khi traverse.

</details>

### D5. Parallel stream `(một đáp án)`

```java
List<Integer> acc = new ArrayList<>();
IntStream.range(0, 10_000).parallel().forEach(acc::add);
```

- A. `acc.size()` luôn 10000
- B. Race: `ArrayList` không thread-safe; mất phần tử / exception
- C. `forEach` parallel tự synchronize
- D. Parallel stream cấm side effect mọi lúc, kể cả `ConcurrentLinkedQueue`

<details><summary>Đáp án D5</summary>

**B.** Side effect lên collection không thread-safe. Dùng collect, hoặc concurrent collection **nếu** thật sự cần side effect (vẫn mất order).

</details>

### D6. `Hashtable` / `Vector` `(một đáp án)`

- A. Mọi compound (`if (!v.contains(x)) v.add(x)`) đều atomic
- B. Từng method synchronized; check-then-act vẫn race
- C. Nhanh hơn `ConcurrentHashMap`
- D. Cho phép `null` như `HashMap`

<details><summary>Đáp án D6</summary>

**B.** Đây là bẫy “class thread-safe nên code tôi thread-safe”.

</details>

---

## E. Executor, Future, pool (OCP + Alibaba manual)

### E1. `execute` vs `submit` `(một đáp án)`

- A. `execute(Runnable)` trả `Future`
- B. `submit` trả `Future`; exception của task được gói, ném khi `get()`
- C. `execute` luôn nuốt exception
- D. `submit(Callable)` không cho checked exception trong `call()`

<details><summary>Đáp án E1</summary>

**B.** `execute`: exception lên thread’s `UncaughtExceptionHandler`. `submit`: exception trong `ExecutionException` khi `Future.get()`. `Callable.call()` **được** throw Exception.

</details>

### E2. `shutdown` vs `shutdownNow` `(một đáp án)`

- A. `shutdown` interrupt mọi task đang chạy ngay
- B. `shutdown`: không nhận task mới, cho task đã submit chạy xong; `shutdownNow`: cố interrupt đang chạy và trả task chưa chạy
- C. `shutdownNow` đợi `awaitTermination`
- D. Sau `shutdown` vẫn `execute` được

<details><summary>Đáp án E2</summary>

**B.** Task phải **đáp ứng interrupt**. `awaitTermination` là bước riêng.

</details>

### E3. `Executors.newFixedThreadPool(n)` `(nhiều đáp án)`

Theo Alibaba Java Manual / thực tế production:

- A. Queue **unbounded** (`LinkedBlockingQueue` không capacity) → OOM khi task đến nhanh
- B. Nên dùng `ThreadPoolExecutor` tự set bound queue + `RejectedExecutionHandler`
- C. `newCachedThreadPool` tạo tối đa 2 thread
- D. `newCachedThreadPool` không bound số thread → dễ bùng thread

<details><summary>Đáp án E3</summary>

**A, B, D.** `newCachedThreadPool`: `SynchronousQueue`, keep-alive 60s, `Integer.MAX_VALUE` threads.

</details>

### E4. `Future.get()` `(một đáp án)`

Task ném `RuntimeException`:

- A. `get()` ném đúng exception đó
- B. `get()` ném `ExecutionException` (cause là exception gốc)
- C. `get()` ném `InterruptedException`
- D. `get()` return `null`

<details><summary>Đáp án E4</summary>

**B.** `CancellationException` nếu cancel. `InterruptedException` nếu thread gọi `get` bị interrupt.

</details>

### E5. Rejected task `(một đáp án)`

Pool `shutdown` rồi `execute`:

- A. Im lặng
- B. `RejectedExecutionException` (handler mặc định `AbortPolicy`)
- C. Chạy trên caller luôn, không exception
- D. Queue mãi mãi

<details><summary>Đáp án E5</summary>

**B.** `CallerRunsPolicy` mới chạy trên caller. `AbortPolicy` là default.

</details>

### E6. `ThreadLocal` + pool `(một đáp án)`

Worker pool tái sử dụng thread, task set `ThreadLocal` rồi quên `remove()`:

- A. Không sao, GC thu ngay
- B. Memory leak / data leak sang task sau trên cùng worker (TPS04)
- C. `ThreadLocal` không hoạt động trong pool
- D. Chỉ leak nếu `InheritableThreadLocal`

<details><summary>Đáp án E6</summary>

**B.** Alibaba manual: sau request/task phải `remove()`.

</details>

### E7. `scheduleAtFixedRate` vs `scheduleWithFixedDelay` `(một đáp án)`

Task chạy 3s, period/delay 2s:

- A. Hai cái giống nhau
- B. `atFixedRate`: cố gắng mỗi 2s theo lịch (có thể chồng nếu overlap không được — pool single thì trễ chồng chất); `withFixedDelay`: chờ **xong rồi** mới + 2s
- C. `atFixedRate` luôn chồng nhiều instance trên 1 thread
- D. Cả hai interrupt task quá hạn

<details><summary>Đáp án E7</summary>

**B.** Single-thread executor không chạy song song hai lần cùng task; rate sẽ drift nếu task lâu hơn period.

</details>

---

## F. Synchronizers

### F1. `CountDownLatch` vs `CyclicBarrier` `(nhiều đáp án)`

- A. Latch: đếm xuống, không reset; barrier: tái sử dụng được
- B. Latch: một/nhiều thread chờ sự kiện; barrier: N party gặp nhau tại điểm
- C. `CountDownLatch.countDown` chỉ caller của `await` mới gọi được
- D. Barrier có thể có `barrierAction` chạy khi đầy

<details><summary>Đáp án F1</summary>

**A, B, D.** Ai cũng `countDown` được. Latch không cyclic.

</details>

### F2. `Semaphore(1)` `(một đáp án)`

- A. Giống `synchronized` 100% (reentrant, wait/notify)
- B. Binary semaphore ≈ mutex **không** reentrant (cùng thread `acquire` 2 lần có thể tự chặn)
- C. `Semaphore` tự nhả khi exception, không cần `release` trong `finally`
- D. `acquire` không interruptible

<details><summary>Đáp án F2</summary>

**B.** Luôn `release` trong `finally`. Có `acquireUninterruptibly`. ReentrantLock thì reentrant.

</details>

### F3. Fair lock `(một đáp án)`

`new ReentrantLock(true)`:

- A. Nhanh hơn unfair
- B. FIFO hơn, giảm starvation, throughput thường thấp hơn unfair
- C. Không thể deadlock
- D. Không cần `unlock` trong `finally`

<details><summary>Đáp án F3</summary>

**B.** Default `synchronized` và `ReentrantLock()` là **unfair**.

</details>

### F4. `tryLock` `(một đáp án)`

Dùng `tryLock` khi lock thứ 2 fail thì unlock lock 1:

- A. Gây deadlock chắc hơn
- B. Có thể tránh deadlock (LCK07: lock ordering hoặc backoff)
- C. `tryLock` bỏ qua interrupt
- D. `tryLock` luôn đợi mãi

<details><summary>Đáp án F4</summary>

**B.** `tryLock()` không đợi (overload có timeout). Vẫn phải unlock đã acquire.

</details>

### F5. `Phaser` `(một đáp án)`

- A. Chỉ 2 thread
- B. Barrier động: đăng ký/hủy party lúc chạy, nhiều phase
- C. Thay `volatile`
- D. Deprecated

<details><summary>Đáp án F5</summary>

**B.** OCP đôi khi nhắc tên; CyclicBarrier cố định số party lúc tạo (trừ `reset`).

</details>

---

## G. `CompletableFuture`, lock nâng cao, virtual thread

### G1. `thenApply` vs `thenApplyAsync` `(một đáp án)`

- A. Giống nhau
- B. `thenApply` thường chạy trên thread hoàn thành stage trước (có thể là caller); `thenApplyAsync` đẩy sang executor (default `ForkJoinPool.commonPool()`)
- C. `thenApplyAsync` luôn main thread
- D. `thenApply` song song mặc định

<details><summary>Đáp án G1</summary>

**B.** Bẫy: block `commonPool` bằng I/O làm chết parallel stream / CF khác.

</details>

### G2. Exception CF `(một đáp án)`

```java
CompletableFuture.supplyAsync(() -> { throw new RuntimeException("x"); })
    .thenApply(s -> s + "!")
    .join();
```

- A. Return `null`
- B. `join()` ném `CompletionException` (cause `RuntimeException`)
- C. `thenApply` nuốt lỗi
- D. Compile error

<details><summary>Đáp án G2</summary>

**B.** `get()` ném `ExecutionException`. Dùng `exceptionally` / `handle` / `whenComplete` để phục hồi.

</details>

### G3. `ReadWriteLock` `(một đáp án)`

- A. Nhiều writer đồng thời
- B. Nhiều reader đồng thời; writer exclusive
- C. Writer không cần unlock
- D. Luôn nhanh hơn `synchronized` dù 100% write

<details><summary>Đáp án G3</summary>

**B.** Lợi khi đọc nhiều. Write nặng thì không hơn mutex.

</details>

### G4. Virtual threads (Java 21) `(nhiều đáp án)`

- A. Rẻ khi block I/O; đừng pool virtual thread như platform thread
- B. Pin carrier khi `synchronized` lâu / native JNI (cải thiện dần qua các bản JDK)
- C. Thay thế nhu cầu thread-safety của shared mutable state
- D. `Thread.ofVirtual().start(runnable)` tạo virtual thread

<details><summary>Đáp án G4</summary>

**A, B, D.** Shared mutable vẫn cần đồng bộ. OCP 21 có thể hỏi tạo platform vs virtual.

</details>

### G5. `synchronized` giữ lúc I/O `(một đáp án)`

- A. Best practice
- B. Trái LCK09: không block khi đang giữ lock (chậm, deadlock, pin virtual thread)
- C. JVM tự nhả lock khi I/O
- D. Chỉ sai với file, không sai với socket

<details><summary>Đáp án G5</summary>

**B.**

</details>

---

## H. Liveness: deadlock, livelock, starvation

### H1. Điều kiện deadlock Coffman `(nhiều đáp án)`

- A. Mutual exclusion
- B. Hold and wait
- C. No preemption
- D. Circular wait
- E. `volatile` thiếu

<details><summary>Đáp án H1</summary>

**A–D.** Phá 1 điều kiện là đủ (thường: lock order toàn cục, timeout, `tryLock`).

</details>

### H2. Code deadlock `(một đáp án)`

```java
// T1
synchronized (a) { synchronized (b) { } }
// T2
synchronized (b) { synchronized (a) { } }
```

- A. Không bao giờ deadlock
- B. Có thể deadlock
- C. Compile error
- D. JVM phát hiện và ném `DeadlockException`

<details><summary>Đáp án H2</summary>

**B.** JVM **không** ném exception; dùng `jstack` / JFR. Cố định thứ tự lock (ví dụ theo `System.identityHashCode`).

</details>

### H3. Livelock vs deadlock `(một đáp án)`

- A. Giống nhau
- B. Livelock: thread vẫn chạy, đổi trạng thái, nhưng không tiến triển (ví dụ cả hai `tryLock` fail rồi retry đối xứng)
- C. Livelock = starvation của lock fair
- D. Livelock chỉ xảy ra với `wait`

<details><summary>Đáp án H3</summary>

**B.** Starvation: một thread mãi không được scheduler/lock (unfair lock, priority). Deadlock: vòng chờ, không ai chạy tiếp critical section.

</details>

### H4. `SimpleDateFormat` `(một đáp án)`

Nhiều thread dùng **một** instance `SimpleDateFormat`:

- A. Thread-safe từ Java 8
- B. Không thread-safe → `DateTimeFormatter` (immutable) hoặc ThreadLocal + remove, hoặc instance mới
- C. Chỉ `parse` không an toàn, `format` an toàn
- D. Cần `volatile` là đủ

<details><summary>Đáp án H4</summary>

**B.** Alibaba manual. `volatile` không giúp Calendar nội bộ.

</details>

---

## I. Câu “output nào có thể”

### I1. `(nhiều đáp án)`

```java
static int x = 0;
public static void main(String[] args) throws Exception {
    Thread t = new Thread(() -> x = 1);
    t.start();
    System.out.print(x);
    t.join();
    System.out.print(x);
}
```

Output có thể:

- A. `01`
- B. `11`
- C. `10`
- D. `00`

<details><summary>Đáp án I1</summary>

**A, B.** Trước `join`, `x` có thể 0 hoặc 1. Sau `join` luôn 1 (HB). Không thể `10` hay `00`.

</details>

### I2. `(một đáp án)`

```java
Thread t = new Thread(() -> System.out.print("X"));
t.start();
System.out.print("Y");
```

- A. Luôn `XY`
- B. Luôn `YX`
- C. `XY` hoặc `YX`
- D. Không in gì

<details><summary>Đáp án I2</summary>

**C.** Không `join` → không thứ tự. Exam rất hay hỏi “which are possible”.

</details>

### I3. Gọi `run` `(một đáp án)`

```java
new Thread(() -> System.out.print("T")).run();
System.out.print("M");
```

- A. `TM` luôn, không thread mới
- B. `TM` hoặc `MT`
- C. Không in `T`
- D. `IllegalThreadStateException`

<details><summary>Đáp án I3</summary>

**A.** Đồng bộ trên main.

</details>

### I4. Two synchronized methods `(một đáp án)`

Hai thread, cùng instance `Foo`, một gọi `a()` một gọi `b()`:

```java
synchronized void a() { System.out.print("A"); }
synchronized void b() { System.out.print("B"); }
```

- A. Chỉ `AB`
- B. `AB` hoặc `BA`, không xen `A` và `B` giữa chừng (mỗi method một lock `this`)
- C. Có thể `AABB` xen từng ký tự nếu print không atomic
- D. Deadlock chắc chắn

<details><summary>Đáp án I4</summary>

**B.** `print` một char trong critical section đã lock `this` — thread kia không xen vào `a`/`b`. Order giữa hai method không cố định.

</details>

---

## J. Câu “chọn API đúng”

### J1. Cần đếm đủ N worker xong rồi main mới tiếp `(một đáp án)`

- A. `CyclicBarrier` bắt buộc
- B. `CountDownLatch(N)` — worker `countDown`, main `await`
- C. `Thread.yield`
- D. `volatile int`++ không lock

<details><summary>Đáp án J1</summary>

**B.** Barrier cũng làm được nếu main là một party, nhưng latch đúng bài toán “sự kiện một chiều”.

</details>

### J2. Producer–consumer bounded `(một đáp án)`

- A. `HashMap`
- B. `ArrayBlockingQueue` / `LinkedBlockingQueue(capacity)` — `put`/`take`
- C. `ConcurrentHashMap`
- D. `CopyOnWriteArrayList` làm queue

<details><summary>Đáp án J2</summary>

**B.**

</details>

### J3. Counter cực hot nhiều thread `(một đáp án)`

- A. `synchronized` luôn nhanh nhất
- B. `LongAdder` (hoặc `LongAccumulator`) thường scale tốt hơn `AtomicLong` khi contention cao
- C. `volatile long++`
- D. `int` thường

<details><summary>Đáp án J3</summary>

**B.** `LongAdder` cells. `AtomicLong` đủ khi contention thấp.

</details>

### J4. Interrupt-aware lock `(một đáp án)`

- A. `synchronized` có thể interrupt lúc chờ lock
- B. `ReentrantLock.lockInterruptibly()` chờ lock và đáp ứng interrupt
- C. `lock()` luôn interruptible
- D. `synchronized` nhả lock khi interrupt

<details><summary>Đáp án J4</summary>

**B.** Chờ vào `synchronized` **không** interruptible (`BLOCKED`).

</details>

### J5. CAS ABA `(một đáp án)`

- A. Không tồn tại trên Java
- B. `AtomicStampedReference` / `AtomicMarkableReference` / version stamp
- C. `volatile` giải ABA
- D. `synchronized` gây ABA nhiều hơn CAS

<details><summary>Đáp án J5</summary>

**B.** Câu interview khó; OCP ít đi sâu.

</details>

---

Làm xong MCQ → sang [tinh-huong.md](tinh-huong.md). Sau đó [bai-tap.md](bai-tap.md).
