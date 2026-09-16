# Project spine: tự build một hệ từ đầu

Khóa học cho khung. GitHub cho mẫu. **Spine** mới giữ kiến thức: một repo của bạn, nới theo phase [01-lo-trinh.md](01-lo-trinh.md).

Chọn **một** đề. Làm đủ invariant, đừng làm 3 CRUD.

## Đề A — Mini-bank (transaction khốc liệt)

**Use case:** khách có account, chuyển khoản nội bộ, thanh toán merchant (hold → capture), sao kê.

Module/service dần:

| Phase | Scope |
|-------|--------|
| 0 | 1 DB: `accounts`, `ledger_entries` (append-only), chuyển khoản 1 TX |
| 1 | Module `transfer`, `statement`; domain event in-process |
| 2–3 | `ledger-service` + `notification-service`; Outbox `MoneyTransferred` |
| 4 | Saga `PayMerchant`: fraud check → hold → capture → notify; fail thì release |
| 5 | CQRS statement (read model); optional event-sourced ledger |
| 6 | Idempotency key, traceId, job đối soát `balance vs sum(entries)` |

Invariant bắt buộc:

- Không âm available (available = posted − held).
- Mọi thay đổi số dư có `entry_id` unique.
- Retry API không tạo entry thứ hai.
- Compensation = entry đối ứng, không xóa lịch sử.

Tham khảo: money-transfer Eventuate, Seata TCC, DDIA chương transaction.

## Đề B — Order–Inventory–Ship (logistics)

**Use case:** đặt hàng, giữ kho, thanh toán, tạo vận đơn, hủy.

Luồng saga gợi ý:

1. Validate order.
2. Reserve inventory (TTL).
3. Authorize payment.
4. Confirm allocation + capture.
5. Create shipment.

Fail ở 3 → release reservation.
Fail sau khi pick (nếu bạn làm WMS) → không “rollback”; tạo nhiệm vụ trả kệ.

Invariant:

- Không oversell.
- Reservation hết hạn được worker dọn.
- Một `orderId` không ship hai lần (idempotent `CreateShipment`).

Tham khảo: FTGO, Ali food-ordering, ecom saga repos.

## Đề C — Booking ghế (nếu làm khóa airline)

Seat identity (flight+seat), hold TTL, payment, ticket.
Overbooking nếu làm thì là cờ chính sách + hàng đợi, không phải race bug.

## Definition of done (mọi đề)

Repo của bạn được coi là “xuyên suốt” khi:

- [ ] Docker compose: DB + broker + services chạy được trên máy trống.
- [ ] README: sequence happy + 2 fail path.
- [ ] Bảng outbox/inbox (hoặc CDC) — **không** dual-write.
- [ ] Saga state query được (`GET /sagas/{id}` hoặc admin).
- [ ] Test: concurrent reserve / concurrent transfer (property: không âm, không oversell).
- [ ] Test: restart consumer giữa chừng không double-apply.
- [ ] Một job reconciliation (dù chỉ `assert` trong test).

## Cách không tự lừa

- Không thêm Redis/Kafka vì checklist. Thêm khi Phase 0 đã fail đúng nghĩa (race, crash).
- Không microservices trước khi modular monolith chạy invariant.
- Mỗi PR nhỏ: một failure mode.
- Đọc FTGO/Eventuate **cùng lúc** với feature tương ứng, không đọc hết rồi mới code.

## Gắn với sách trong repo này

| Sách (folder gốc) | Dùng cho spine |
|-------------------|----------------|
| Clean Architecture / Clean Code | Ranh giới domain vs adapter, test |
| Effective Java | API, equals, concurrency local |
| Refactoring | Cắt module trước khi cắt service |

System design phân tán: *Microservice Patterns* + DDIA (mua/mượn ngoài repo).
