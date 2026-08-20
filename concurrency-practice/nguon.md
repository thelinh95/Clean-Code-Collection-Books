# Nên kiểm tra / ôn concurrency ở đâu?

## 1. Chứng chỉ chính thức (chất lượng cao nhất)

Đây là nguồn “đúng chuẩn exam”, không phải dump lậu.

| Nguồn | Dùng để làm gì |
| --- | --- |
| [Oracle Java SE 17 Developer (1Z0-829)](https://education.oracle.com/java-se-17-developer/pexam_1Z0-829) / [Java SE 21 (1Z0-830)](https://education.oracle.com/java-se-21-developer/pexam_1Z0-830) | Topic chính thức: `Runnable`/`Callable`, `ExecutorService`, lock, concurrent collections, parallel streams |
| Sách **OCP Java SE 17 Developer Study Guide** (Boyarsky & Selikoff, Wiley) | Chương concurrency rất gần đề thật: start vs run, `volatile`, `CyclicBarrier`, parallel stream side-effect |
| **Enthuware** (bộ mock 1Z0-829 / 1Z0-830, trả phí hợp pháp) | Nhiều câu code “what is possible output” — đây là kiểu khó nhất |
| [Java Tutorials — Concurrency](https://docs.oracle.com/javase/tutorial/essential/concurrency/) | Ôn khái niệm trước khi làm đề |
| [JLS Chapter 17 — Threads and Locks](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html) | Happens-before, `volatile`, wait/notify — khi muốn hiểu “vì sao” |

**Topic Oracle luôn hỏi:**

- `start()` vs `run()`, lifecycle, daemon, interrupt
- `synchronized` / `Lock` / `volatile` / atomic — visibility vs atomicity
- `ExecutorService`: `execute` vs `submit`, `shutdown` vs `shutdownNow`, `Future`
- `CountDownLatch`, `CyclicBarrier`, `Semaphore`
- `ConcurrentHashMap`, `CopyOnWriteArrayList`, parallel stream + shared mutable state
- Deadlock: nhiều lock, thứ tự lock khác nhau

## 2. oDesk / Upwork — thực tế ra sao?

Test cũ **oDesk / Upwork Java** (và “Java Multithreading”) không còn là cổng chính để nhận job, nhưng ngân hàng câu hỏi vẫn lưu hành trên blog/GitHub dump. Những dump đó:

- **Có bản quyền**, thường sai/lỗi thời (Java 6/7, EJB, RMI).
- Dùng dump để “pass test” là gian lận với nền tảng.

**Nên làm gì:** ôn **cùng topic** bằng đề tự luyện (file `trac-nghiem.md` trong thư mục này), không cần copy đề lậu.

Topic hay xuất hiện trên test kiểu Upwork/oDesk:

- Thread nằm trong process; mọi process có ≥ 1 thread
- `sleep()` vs `wait()`
- `start()` mới tạo thread; gọi `run()` trực tiếp thì chạy trên thread hiện tại
- `synchronized` trên cùng một monitor
- `notify` vs `notifyAll`
- `join()`
- `InterruptedException` khi `sleep`/`wait`/`join`

Nếu vẫn muốn làm test trên Upwork: vào **Upwork → Profile → Skills tests** (nếu còn mở cho account của bạn) và làm **chính thức**, không dùng answer key.

## 3. Interview / tình huống khó (sau exam)

| Nguồn | Ghi chú |
| --- | --- |
| [Oracle Java Concurrency tutorial](https://docs.oracle.com/javase/tutorial/essential/concurrency/) | Nền |
| [SEI CERT Java — VNA / LCK / THI / TPS / TSM](https://wiki.sei.cmu.edu/confluence/display/java/SEI+CERT+Oracle+Coding+Standard+for+Java) | Rule bảo mật concurrency; README repo này đã link wiki này |
| *Java Concurrency in Practice* (Goetz) | Sách nền; chương 3 (visibility), 10 (liveness), 11 (performance) |
| *Effective Java* 3rd (trong repo) Item 78–84 | `synchronized`, `wait`, executor, concurrency tools, lazy init, thread groups |
| *Alibaba Java Development Manual* (trong repo, bản Thái Sơn) | Thread pool, `SimpleDateFormat`, `ThreadLocal.remove`, lock |
| [GeeksforGeeks — Java Concurrency Tools](https://www.geeksforgeeks.org/interview-prep/java-concurrency-tools-modern-java-techniques-interview-questions/) | Ôn API hiện đại |
| LeetCode tag **Concurrency**: 1114, 1115, 1116, 1117, 1195, 1226 | Bài tập ngắn, đúng “tình huống khó” |

## 4. Bài tập code (sau khi master lý thuyết)

Xem [bai-tap.md](bai-tap.md). Thứ tự gợi ý:

1. Thread-safe counter
2. Bounded buffer (producer–consumer)
3. In thứ tự / barrier
4. Chuyển tiền không deadlock
5. Cache đồng thời
6. Dining philosophers
7. LeetCode 1114–1117, 1226
8. Tự viết thread pool nhỏ (queue + worker)

## 5. Sách trong repo này liên quan concurrency

- `AW.Effective.Java.3rd.Edition...` / `Effective.Java.3rd.Edition.2018.1.pdf` — Item 78–84
- `java开发手册-泰山版于4.22.pdf` — chương concurrent processing
- SEI CERT (link trong README gốc)

*Clean Code / Clean Architecture* gần như **không** dạy Java Memory Model; đừng kỳ vọng ôn concurrency chỉ từ các sách đó.
