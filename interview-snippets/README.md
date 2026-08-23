# Java 8 interview snippets

Bộ snippet **viết tay được** bằng Java 8: cache/LRU, `CompletableFuture`, và các kiến trúc kinh điển hay bị bắt implement lại trên bảng / CoderPad.

Không phải đề thi có bản quyền. Code tự soạn, bám đúng thứ interviewer hay gọi: "viết LRU", "chống stampede", "circuit breaker", "consistent hash".

Khác [concurrency-practice](../concurrency-practice/README.md) (nếu có trên nhánh kia): pack đó luyện *câu hỏi / race / OCP*. Pack này là *code mẫu để thuộc rồi viết lại*.

## Học thế nào

1. Đọc [cheatsheet.md](cheatsheet.md) — 1 trang: khi nào dùng cái gì, bẫy hay mắc.
2. Đọc [viet-tay.md](viet-tay.md) — bản siêu ngắn, đúng thứ cần nhớ khi không có IDE.
3. Mở `exercises/src/iv/` — bản đủ test, comment giải thích.
4. Đóng file, viết lại trên giấy / notepad trắng. Chạy test để tự chấm.

```bash
cd interview-snippets/exercises
chmod +x run-tests.sh
./run-tests.sh
```

Compile `--release 8`. Không dùng `var`, `List.of`, `orTimeout`, record.

## Map chủ đề — họ hay bắt gì

| Họ nói | Viết | File |
| --- | --- | --- |
| LRU cache | `LinkedHashMap(accessOrder)` rồi bản DLL | `LruLinkedHashMap`, `LruCache` |
| TTL / expire | lazy expire + clock inject | `TtlCache` |
| memoize / cache-aside | `computeIfAbsent` | `Memoizer` |
| stampede / singleflight | 1 load, N waiter share `CompletableFuture` | `SingleflightCache` |
| async Java 8 | map / flatMap / zip / allOf / timeout / retry | `CfPatterns` |
| producer-consumer | bounded buffer `wait/notify` | `BoundedBuffer` |
| rate limit | token bucket + sliding window | `TokenBucket`, `SlidingWindowLimiter` |
| circuit breaker | CLOSED / OPEN / HALF_OPEN | `CircuitBreaker` |
| retry | exp backoff + jitter | `Retry` |
| thread pool | N worker + queue | `SimpleThreadPool` |
| connection pool | borrow / release | `ObjectPool` |
| pub/sub | EventBus | `EventBus` |
| consistent hash | `TreeMap` ring + vnode | `ConsistentHashRing` |
| load balancer | RR + least-conn | `LoadBalancer` |
| webhook / payment trùng | idempotency key | `IdempotencyStore` |
| nhiều đọc ít ghi | `ReadWriteLock` | `ReadWriteCache` |
| Kruskal / connected | Union-Find | `UnionFind` |
| autocomplete | Trie | `Trie` |
| build order / course | Kahn topo | `TopoSort` |
| "có thể có / chắc không có" | Bloom filter | `BloomFilter` |
| singleton multithread | DCL + `volatile` | `SingletonDcl` |
| order / workflow | state machine | `OrderStateMachine` |
| cache trước DB | caching proxy | `CachingProxy` |
| pipes and filters | `Pipeline` | `Pipeline` |
| Observer / Strategy / Decorator / Builder / Factory | bản tối giản | `ClassicPatterns` |

## Câu follow-up nên nói được

- LRU `LinkedHashMap` **không** thread-safe. `ConcurrentHashMap` **không** LRU.
- `computeIfAbsent` giữ lock bin lúc load — loader chậm làm nghẽn. Singleflight nhả lock, waiter `join` future.
- Java 8 không có `orTimeout`: race với `ScheduledExecutorService` + `applyToEither`.
- `thenApply` = map; `thenCompose` = flatMap (tránh `CompletableFuture<CompletableFuture<T>>`).
- Token bucket cho burst; sliding window cứng hơn, đắt hơn về bộ nhớ.
- Circuit breaker bảo *hệ mình*, rate limit bảo *hệ người khác / chính mình khỏi spam*.
- Consistent hash: virtual node để key không dồn 1 máy.
- Bloom: false positive OK, false negative không — đừng dùng cho quyền truy cập.
- Singleton DCL thiếu `volatile` → thread khác thấy object nửa khởi tạo.

Viết từ trí nhớ. Đừng copy lúc luyện.
