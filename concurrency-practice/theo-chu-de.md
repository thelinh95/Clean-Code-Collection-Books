# Phân loại theo chủ đề (học local)

Đây **không** phải dump đề Oracle/Upwork. Oracle và LeetCode giữ bản quyền đề gốc; “chỉ học local” không làm dump thành hợp pháp. File này **phân loại đề luyện tự soạn** + link chính thức.

## Lộ trình

| Bước | File | Mục đích |
| --- | --- | --- |
| 1. Map chủ đề | file này | biết câu nào thuộc topic nào |
| 2. MCQ Oracle-style | [oracle-theo-chu-de.md](oracle-theo-chu-de.md) + [trac-nghiem.md](trac-nghiem.md) | ôn 1Z0-829/830 |
| 3. Tình huống | [tinh-huong.md](tinh-huong.md) | JMM / deadlock / pool |
| 4. LeetCode concurrency | [leetcode.md](leetcode.md) + `exercises/` | 9 bài tag Concurrency |
| 5. Cheatsheet | [cheatsheet.md](cheatsheet.md) | tra nhanh |

## Oracle / OCP — theo objective

### A. Thread API: `start`/`run`, lifecycle, interrupt, daemon, `join`

- MCQ: A1–A8, I1–I3, J4
- Tình huống: S9, S16, S21
- Oracle thêm: mục **Thread API** trong `oracle-theo-chu-de.md`

### B. `synchronized`, monitor, `wait`/`notify`

- MCQ: A3–A4, B1–B6, I4
- Tình huống: S3, S4, S5, S10, S23, S24
- Oracle thêm: mục **Locking**

### C. JMM: `volatile`, atomic, happens-before, publication

- MCQ: C1–C10, I1, C6
- Tình huống: S1, S2, S6, S17, S22
- Oracle thêm: mục **Visibility**

### D. Concurrent collections + parallel stream

- MCQ: D1–D6
- Tình huống: S7, S8, S12, S14, S15
- Oracle thêm: mục **Collections**
- Code: `BoundedBuffer`, LeetCode 1188

### E. Executor, `Future`, pool, `ThreadLocal`

- MCQ: E1–E7, G1–G2
- Tình huống: S9, S13, S19
- Oracle thêm: mục **Executors**

### F. Synchronizers: latch, barrier, semaphore, phaser

- MCQ: F1–F5, J1
- Code: `OrderedPrinter`, `RateLimiter`, LeetCode 1114–1117, 1195

### G. Liveness: deadlock, livelock, starvation

- MCQ: H1–H4, B3
- Tình huống: S3, S10, S11, S20, S24
- Code: `Bank`, `DiningPhilosophers`, LeetCode 1226, 1279

### H. Java 21 virtual threads (1Z0-830)

- MCQ: G4–G5
- Oracle thêm: mục **Virtual threads**

## LeetCode tag Concurrency (9 bài)

Xem [leetcode.md](leetcode.md). Map nhanh:

| ID | Bài | Chủ đề drill |
| --- | --- | --- |
| 1114 | Print in Order | latch / thứ tự happens-before |
| 1115 | Print FooBar Alternately | semaphore đôi |
| 1116 | Print Zero Even Odd | 3-way gate |
| 1117 | Building H2O | barrier + permit |
| 1188 | Bounded Blocking Queue | `wait`/`notify` hoặc `Condition` |
| 1195 | Fizz Buzz Multithreaded | 4 thread predicate |
| 1226 | Dining Philosophers | lock order |
| 1242 | Web Crawler Multithreaded | pool + visited set |
| 1279 | Traffic Light | mutex giao lộ |

## oDesk/Upwork (cùng topic, không dump)

A2, A3, A1, B6, A5 — thread/process, sleep/wait, start/run, notify, join.
