# Cheatsheet: đã hiểu pattern chưa?

In ra, đánh dấu khi **giải thích được failure**, không khi “nhớ định nghĩa”.

## Transaction

- [ ] Isolation level nào đủ cho reserve inventory / hold tiền trên 1 DB.
- [ ] `SELECT FOR UPDATE` vs `version` column: lúc nào.
- [ ] Unique constraint giải quyết race nào.
- [ ] Vì sao 2PC đau ở multi-region / mixed technology.

## Messaging

- [ ] At-least-once vs at-most-once vs “exactly-once” (ở app).
- [ ] Dual-write là gì; Outbox chữa chỗ nào.
- [ ] Consumer idempotent: key nào (`eventId` / `transferId` / `orderId+step`).
- [ ] Ordering: 1 partition key = 1 aggregate id.

## Saga

- [ ] Vẽ được state machine + compensating step.
- [ ] Compensation ≠ SQL rollback; ví dụ banking + logistics.
- [ ] Semantic lock / `PENDING` để giả lập isolation.
- [ ] Orchestration vs choreography: debug, coupling, audit.
- [ ] Timeout + compensation fail → trạng thái nào, ai xử lý.

## Domain

- [ ] Banking: available vs held vs posted; bút toán đối ứng.
- [ ] Logistics: reserve vs allocate vs ship; TTL giỏ hàng.
- [ ] Payment: authorize vs capture vs refund vs webhook trùng.
- [ ] Booking: hold ghế + unique seat; overbooking là policy.

## Ops

- [ ] `traceId` xuyên mọi bước saga.
- [ ] Metric: outbox lag, consumer lag, saga stuck.
- [ ] Reconciliation: so hai nguồn sự thật.

## Repo đã mở đúng file

- [ ] Eventuate `CreateOrderSaga` (orchestration)
- [ ] Eventuate choreography customers-and-orders
- [ ] FTGO `CreateOrderSaga` + 1 command handler
- [ ] Một outbox implementation (Tram hoặc food-ordering)
- [ ] Money-transfer **hoặc** Seata TCC transfer

Link đầy đủ: [03-github-projects.md](03-github-projects.md).
