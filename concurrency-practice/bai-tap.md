# Bài tập code — làm sau khi đã master MCQ + tình huống

Mục tiêu: biến kiến thức thành reflex (lock order, `finally`, interrupt, không share `HashMap`).

## Cách làm

```bash
cd concurrency-practice/exercises
./run-tests.sh
```

Stub ném `UnsupportedOperationException` → test fail. Implement trong `src/`. Đối chiếu `solutions/` **sau khi** tự làm.

Java 21, không cần Maven. Script tự `javac` + chạy test harness.

## Thứ tự bài

| # | Class | Ý | Độ khó |
| --- | --- | --- | --- |
| 1 | `SafeCounter` | Nhiều thread tăng đếm, kết quả đúng | Dễ |
| 2 | `BoundedBuffer` | Producer–consumer, capacity cố định, `put`/`take` block | Trung bình |
| 3 | `OrderedPrinter` | 3 thread in `first second third` đúng thứ tự (kiểu LeetCode 1114) | Trung bình |
| 4 | `Bank` | Chuyển tiền không mất tiền, không deadlock | Khó |
| 5 | `Memoizer` | Cache `computeIfAbsent` an toàn, không tính 2 lần cùng key | Khó |
| 6 | `DiningPhilosophers` | 5 nhà triết học, không deadlock | Khó |
| 7 | `RateLimiter` | Tối đa N tác vụ đồng thời (Semaphore) | Trung bình |
| 8 | `LazySingleton` | Lazy, thread-safe, không DCL sai | Trung bình |

## Bài tập thêm (ngoài repo — khi làm xong 8 bài trên)

LeetCode (Java):

- [1114. Print in Order](https://leetcode.com/problems/print-in-order/)
- [1115. Print FooBar Alternately](https://leetcode.com/problems/print-foobar-alternately/)
- [1116. Print Zero Even Odd](https://leetcode.com/problems/print-zero-even-odd/)
- [1117. Building H2O](https://leetcode.com/problems/building-h2o/)
- [1195. Fizz Buzz Multithreaded](https://leetcode.com/problems/fizz-buzz-multithreaded/)
- [1226. The Dining Philosophers](https://leetcode.com/problems/the-dining-philosophers/)

Tự thiết kế:

1. Thread pool: queue + N worker, `submit`, `shutdown`, `awaitTermination`.
2. Read-write lock đơn giản (reader count + writer flag).
3. Barrier: N thread `await` rồi cùng đi tiếp.
4. Timeout map: TTL + concurrent get/put.

## Checklist khi review bài mình

- [ ] Mọi `lock()` / `acquire()` có `unlock`/`release` trong `finally`
- [ ] `wait` trong `while` (predicate), không `if`
- [ ] Không `synchronized` trên String intern / boxed cache
- [ ] Interrupt: không nuốt mà không restore (trừ khi chủ đích)
- [ ] Không I/O dài trong lúc giữ lock
- [ ] Shared `long`/`boolean` flags: `volatile` hoặc atomic
- [ ] Collection: CHM / BlockingQueue, không `HashMap` trần
