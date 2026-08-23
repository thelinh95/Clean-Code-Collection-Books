# Cheatsheet — nói trước khi viết

## Cache

| Loại | Cấu trúc | Chi phí | Thread-safe? |
| --- | --- | --- | --- |
| LRU LinkedHashMap | `super(cap, 0.75f, true)` + `removeEldestEntry` | get/put O(1) amortized | không |
| LRU DLL | HashMap + dummy head/tail | get/put O(1) | tự lock |
| TTL | value + `expireAt`, lazy evict | get O(1) | CHM OK |
| Memoizer | `CHM.computeIfAbsent` | 1 load / key | có, nhưng giữ lock bin |
| Singleflight | value map + inflight `CompletableFuture` | 1 load, N share | có, không giữ lock lúc I/O |

**Stampede:** cache hết hạn, 1000 request đua load DB. Chữa: singleflight / lock per key / early refresh.

**Cache-aside:** app đọc cache → miss thì DB → ghi cache. **Write-through:** ghi DB và cache cùng lúc. **Write-behind:** ghi cache, flush DB sau.

## CompletableFuture Java 8

```
supplyAsync(supplier[, exec])     // tạo
thenApply(fn)                     // map sync
thenApplyAsync(fn[, exec])        // map sang pool
thenCompose(fn -> Future)         // flatMap
thenCombine(other, fn)            // zip
allOf(f1,f2,...)                  // Void — join từng cái để lấy list
anyOf(f1,f2,...)                  // Object — phải cast
exceptionally(ex -> T)            // recover
handle((v,ex) -> T)               // luôn chạy
applyToEither(other, fn)          // race (dùng làm timeout)
```

Không có: `orTimeout`, `completeOnTimeout`, `exceptionallyCompose` (Java 9/12).

`join()` ném unchecked `CompletionException`. `get()` ném checked + wrap.

Default pool: `ForkJoinPool.commonPool()` — I/O thì truyền `Executor` riêng.

## Concurrency viết tay

- `wait` phải trong `synchronized` **đúng object**, trong `while (!cond)`.
- `notifyAll` khi nhiều điều kiện (full vs empty).
- `volatile` = visibility, không atomic `i++`.
- DCL singleton: `volatile` bắt buộc.

## Rate limit vs breaker vs retry vs pool

```
client  --retry+backoff-->  breaker  -->  limiter  -->  thread/conn pool  -->  dependency
```

- Retry: lỗi tạm (timeout, 503). Idempotent mới retry POST.
- Jitter: tránh retry storm.
- Breaker: dependency chết thì fail nhanh, đừng chất thêm.
- Limiter: chặn burst vào mình hoặc ra ngoài.
- Pool: giới hạn tài nguyên đắt (thread, socket).

## CS hay bị hỏi kèm system

| Cấu trúc | Dùng khi |
| --- | --- |
| Union-Find | connected, Kruskal, "số đảo" |
| Trie | prefix, autocomplete |
| Kahn topo | dependency, cycle = empty |
| Bloom | "chắc chưa thấy" — cache negative, crawl |
| Consistent hash | shard / cache node, thêm bớt máy |
| FSM | order, ticket, workflow — cấm transition lạ |

## Pattern 30 giây

- Observer: subject giữ list, `set` gọi `onChange`.
- Strategy: interface thuật toán, inject lúc gọi.
- Decorator: cùng interface, bọc thêm hành vi (`*text*`).
- Proxy: cùng interface, kiểm soát truy cập / cache.
- Builder: field optional, `build()` validate.
- Factory: `create(type)` giấu `new`.
- Pipeline: `List<Function>` apply lần lượt.

## Độ phức tạp nhớ miệng

- LRU get/put: O(1)
- Union-Find: ~O(α(n)) ≈ O(1)
- Trie insert/search: O(len)
- Kahn: O(V+E)
- Consistent hash get: O(log N) — `TreeMap.tailMap`
- Sliding window log: O(hits trong cửa sổ)
- Token bucket allow: O(1)
