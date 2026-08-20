# LeetCode Java Concurrency (9 bài)

Làm **trên** [leetcode.com/problemset/concurrency](https://leetcode.com/problemset/concurrency/) để có đề + judge gốc.

Dưới đây là **đặc tả tự viết** (không copy đề LeetCode) + stub trong `exercises/` để drill local. Nộp lên LeetCode khi đã quen API.

```bash
cd concurrency-practice/exercises
./run-tests.sh            # làm stub
./run-tests.sh solutions  # xem reference
```

Class map:

| LeetCode | Class local | Sẵn từ trước? |
| --- | --- | --- |
| 1114 | `OrderedPrinter` | có |
| 1115 | `FooBar` | mới |
| 1116 | `ZeroEvenOdd` | mới |
| 1117 | `H2O` | mới |
| 1188 | `BoundedBuffer` | có |
| 1195 | `FizzBuzzMT` | mới |
| 1226 | `DiningPhilosophers` | có |
| 1242 | `WebCrawler` | mới |
| 1279 | `TrafficLight` | mới |

---

## 1114 Print in Order — [link](https://leetcode.com/problems/print-in-order/)

Ba thread gọi `first` / `second` / `third` thứ tự bất kỳ. In đúng `first` rồi `second` rồi `third`.

Local: `OrderedPrinter`. Gợi ý: hai `CountDownLatch`.

## 1115 Print FooBar Alternately — [link](https://leetcode.com/problems/print-foobar-alternately/)

Hai thread: `foo` in `"foo"` đúng *n* lần, `bar` in `"bar"` *n* lần. Output xen kẽ `foobar` lặp *n*.

Gợi ý: `Semaphore(1)` cho foo, `Semaphore(0)` cho bar.

## 1116 Print Zero Even Odd — [link](https://leetcode.com/problems/print-zero-even-odd/)

Ba thread: zero in `0`, odd in số lẻ, even in số chẵn. Với *n*, dãy `0,1,0,2,...,0,n`.

Gợi ý: 3 semaphore.

## 1117 Building H2O — [link](https://leetcode.com/problems/building-h2o/)

Nhiều thread H và O. Mỗi phân tử: đúng 2 hydrogen + 1 oxygen (thứ tự H/O trong một phân tử bất kỳ). Không được 3H hoặc 2O “dính” thành nhóm sai.

Gợi ý: `Semaphore(2)` + `Semaphore(1)` + `CyclicBarrier(3)` reset permit.

## 1188 Design Bounded Blocking Queue — [link](https://leetcode.com/problems/design-bounded-blocking-queue/)

Queue capacity cố định; `enqueue` block khi đầy, `dequeue` block khi rỗng.

Local: `BoundedBuffer`. Gợi ý: `synchronized` + `while` + `wait`/`notifyAll`.

## 1195 Fizz Buzz Multithreaded — [link](https://leetcode.com/problems/fizz-buzz-multithreaded/)

Bốn thread chia việc: bội 3 (không 5) → `fizz`, bội 5 (không 3) → `buzz`, bội 15 → `fizzbuzz`, còn lại → số. Chạy *i = 1..n* đúng thứ tự.

Gợi ý: lock + `Condition` hoặc 4 semaphore.

## 1226 The Dining Philosophers — [link](https://leetcode.com/problems/the-dining-philosophers/)

5 triết gia, 5 đũa, mỗi người cần 2 đũa. Không deadlock, không đói mãi.

Local: `DiningPhilosophers`. Gợi ý: lock đũa theo `min(id,id+1)` trước.

## 1242 Web Crawler Multithreaded — [link](https://leetcode.com/problems/web-crawler-multithreaded/)

Crawl từ `startUrl`, chỉ URL **cùng hostname**, đa luồng, mỗi URL visit một lần. `HtmlParser.getUrls(url)` cho link.

Gợi ý: `ConcurrentHashMap.newKeySet()` + thread pool; đếm inflight để biết lúc hết việc.

## 1279 Traffic Light Controlled Intersection — [link](https://leetcode.com/problems/traffic-light-controlled-intersection/)

Hai đường (road 1 / 2). Một lúc chỉ một đường được xanh. Xe gọi `carArrived`; nếu đường mình đỏ thì `turnGreen` rồi `crossCar`. Ban đầu đường 1 xanh. Hai xe khác đường không được `crossCar` chồng nhau.

Gợi ý: một mutex; nhớ `greenRoad`; đổi đèn khi xe đến đường kia.

---

Làm trên LeetCode (Java): chọn `Semaphore` / `synchronized` / `Lock`. Đừng block `ForkJoinPool.commonPool()` bằng I/O.
