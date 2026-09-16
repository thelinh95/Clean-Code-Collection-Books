# Lộ trình: từ 1 database tới hệ thống lớn

Không nhảy thẳng “design Uber”. Làm **một spine project** (xem [06-project-xuyen-suot.md](06-project-xuyen-suot.md)) và nới architecture theo phase.

## Phase 0 — Transaction local (bắt buộc)

Mục tiêu: giải thích được *tại sao* microservice làm mất ACID.

- Isolation level, dirty/non-repeatable/phantom, lost update.
- Pessimistic lock (`SELECT … FOR UPDATE`) vs optimistic (`version`).
- Unique constraint như hàng rào nghiệp vụ (số tài khoản, reservation id).
- Transaction boundary: service method ≠ DB transaction.

Thực hành (1 Postgres, 1 service):

- Chuyển khoản nội bộ: `debit A` + `credit B` trong **một** transaction.
- Reserve inventory: không oversell khi 100 request cùng SKU.
- Hold tiền: trạng thái `AVAILABLE / HELD / CAPTURED / RELEASED`.

Sách nền: *Designing Data-Intensive Applications* (Kleppmann) — chương replication, transactions, partitioning. Đọc chậm, ghi note theo bài toán của bạn, không highlight cả trang.

## Phase 1 — Modular monolith + DDD nhẹ

Trước khi cắt service: tách module theo bounded context.

- Aggregate, invariant, domain event (in-process trước).
- Anti-corruption layer nếu gọi hệ thống cũ.
- Một DB, nhiều schema/module vẫn ổn.

Repo tham khảo kiến trúc sạch (chưa cần distributed): code Clean Architecture trong khóa Ali Gelenler, hoặc hexagonal trong food-ordering.

Sách đang có trong repo này: *Clean Architecture* (Uncle Bob) — dùng cho ranh giới module, **không** thay system design phân tán.

## Phase 2 — Cắt service = bài toán dữ liệu phân tán

Chỉ cắt khi có lý do: team, scale độc lập, vòng đời khác nhau.

Hệ quả bắt buộc:

- Không còn JOIN xuyên service.
- Không còn 1 commit cho “trừ tiền + tạo order + trừ kho”.
- Mọi use case đa service phải chọn: **orchestration / choreography / 2PC / chấp nhận inconsistency**.

Viết explicit: use case nào **cần** strong consistency, use case nào eventual được.

## Phase 3 — Messaging đáng tin: Outbox + Inbox

Trước Saga “đẹp”, làm được:

- Transactional Outbox: ghi nghiệp vụ + outbox **cùng transaction**.
- CDC (Debezium) hoặc poller đẩy Kafka.
- Consumer idempotent (inbox table / idempotency key).
- At-least-once là mặc định; exactly-once là **ảo** ở app layer.

GitHub: Eventuate Tram core + example Customers and Orders.

## Phase 4 — Saga (orchestration rồi mới choreography)

Học trên **cùng một use case**: Create Order / Transfer money.

1. Vẽ state machine: `PENDING → APPROVED | REJECTED`, kèm compensating.
2. Implement orchestration (orchestrator giữ saga state).
3. Implement lại choreography (event) — so sánh debug và coupling.
4. Thêm timeout, retry, dead letter, compensation fail (cần human/ops).

Khóa + repo: Ali Gelenler food-ordering; FTGO `CreateOrderSaga`.
Banking: Eventuate money-transfer; Seata TCC transfer.

Đọc song song: [05-saga-va-transaction.md](05-saga-va-transaction.md).

## Phase 5 — Đọc lệch (CQRS) và khi nào Event Sourcing

- CQRS: query cần join nhiều service (order history, account statement).
- Event Sourcing: khi audit/ledger *là* source of truth (banking), không phải vì “microservice cool”.

FTGO: `ftgo-order-history-service` (CQRS), `ftgo-accounting-service` (event sourcing).
Money transfer: `cer/event-sourcing-examples`.

## Phase 6 — Production constraints

Nhét vào cùng project, đừng học riêng:

- Idempotency key trên API gateway.
- Timeout + retry + jitter + circuit breaker (bạn đã có snippet Java 8 trong repo).
- Distributed tracing (`traceId` xuyên saga).
- Outbox lag, consumer lag, reconciliation job (banking: end-of-day).
- Exactly-once payment: unique `paymentIntentId` + ledger append-only.

## Song song: theory interview (không thay project)

Sau khi đã có 1 hệ thống chạy:

- [donnemartin/system-design-primer](https://github.com/donnemartin/system-design-primer)
- [ByteByteGoHq/system-design-101](https://github.com/ByteByteGoHq/system-design-101)
- ByteByteGo / *System Design Interview* (Alex Xu) — luyện vẽ, không luyện transaction.

MIT 6.824 (free) nếu muốn hiểu Raft/replication đúng nghĩa — hữu ích khi design ledger/database, không bắt buộc cho saga CRUD.

## Nhịp học gợi ý

Mỗi pattern: 1 ngày lý thuyết domain + 2–4 ngày đọc/sửa code repo + 1 failure drill (kill Kafka giữa saga, restart consumer, double-submit API).
