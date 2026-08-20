# Cheatsheet — nhớ trước khi làm bài tập

## Visibility vs atomicity

| Công cụ | Visibility | Atomic compound (`i++`, check-then-act) | Mutual exclusion |
| --- | --- | --- | --- |
| không gì | không | không | không |
| `volatile` | có (biến đó + piggyback HB) | không | không |
| `Atomic*` (một method) | có | method đó | không (nhóm method thì không) |
| `synchronized` / `Lock` | có | có (trong critical section) | có |

## Happens-before (rút gọn JLS 17)

- Program order trong **một** thread
- Unlock monitor M → lock M sau đó
- Write `volatile` v → read v sau đó
- `t.start()` → body của `t`
- Hết `t` → `t.join()` return
- Mọi HB đều transitive

`sleep` / `yield` **không** tạo HB với thread khác.

## `wait` / `notify`

- Phải đang hold monitor của **đúng** object
- `wait` nhả lock; `sleep` không
- `while (!predicate) wait();` — không dùng `if`
- Lost notify nếu signal trước khi wait và không check predicate
- `notifyAll` khi nhiều điều kiện wait

## Lock

- Cùng một object mới loại trừ được
- `static synchronized` lock `Class`, instance lock `this`
- Reentrant: cùng thread vào lại được
- Thứ tự lock toàn cục để tránh deadlock
- `unlock`/`release` trong `finally`
- Không I/O/đợi lâu khi đang giữ lock
- Không lock String intern, `Boolean.TRUE`, `Integer` cache

## Pool

- `execute`: exception lên thread; `submit`: gói trong `Future.get()` → `ExecutionException`
- `shutdown` vs `shutdownNow`
- `Executors.newFixedThreadPool` queue unbounded; `newCachedThreadPool` unbounded threads → tự `ThreadPoolExecutor`
- `ThreadLocal.remove()` khi dùng pool

## Collection

- `HashMap` không share đa luồng
- `ConcurrentHashMap`: `putIfAbsent` / `compute*`, không `null`
- `synchronizedList`: lock list khi iterate
- Parallel stream: không side-effect lên `ArrayList`

## Liveness

- Deadlock: 4 điều kiện Coffman — thường phá circular wait
- Livelock: vẫn chạy nhưng không tiến
- Starvation: unfair lock / priority
- JVM không ném `DeadlockException` — `jstack`
