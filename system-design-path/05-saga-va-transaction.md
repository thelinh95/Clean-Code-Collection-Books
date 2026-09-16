# Saga phải học cùng transaction

Saga ra đời vì **không còn một transaction ACID xuyên service**. Nếu chưa đau với ACID local và 2PC, Saga chỉ là flowchart.

## 1. Transaction local — đơn vị vẫn sống trong saga

Mỗi bước saga:

```
begin;
  -- đổi aggregate (Order PENDING, Account HOLD, Seat RESERVED)
  -- ghi outbox event/command
commit;
```

Nếu bước này không ACID, Outbox cũng vô nghĩa (lệch state vs message).

Cần nắm:

- **Atomicity:** trừ tiền mà không ghi ledger → cấm.
- **Isolation:** hai saga reserve cùng 1 ghế / cùng 1 đống tiền.
- **Durability:** commit xong, crash app, state vẫn còn; CDC đọc outbox sau.

Bài tập: 1 service, 1 DB, 200 thread reserve 100 item. Đạt đúng 100 success. Đó là nền saga.

## 2. Vì sao 2PC không phải câu trả lời mặc định

Two-phase commit (XA): prepare mọi RM, rồi commit.

Ở hệ lớn (banking multi-region, logistics nhiều kho):

- Coordinator chết → resource lock lâu (blocking).
- Latency WAN; availability: một participant chậm = cả giao dịch đứng.
- Heterogeneous: Kafka, Redis, payment HTTP, DB — không cùng XA.

Saga đổi deal: **availability và độc lập service** đổi lấy **eventual consistency** + **compensation**.

Banking vẫn dùng 2PC *bên trong* một core ledger. Xuyên core banking ↔ card ↔ AML thì thường saga/TCC/message, không phải XA toàn cục.

## 3. Compensation không phải ROLLBACK

| Rollback (1 DB) | Compensation (saga) |
|-----------------|---------------------|
| Engine hoàn nguyên row chưa commit / undo log | Nghiệp vụ **ngược**, đã commit từng bước |
| Tàng hình với user | User có thể đã thấy “PENDING” / email |
| Tự động, hoàn hảo | Có thể fail; cần retry / manual |

Ví dụ:

- Banking: bước đã `HOLD` 100k → compensate = `RELEASE` (bút toán mới), không xóa hold.
- Logistics: đã tạo pick list → compensate = hủy phiếu + trả kệ, có khi phải người.
- Payment: đã capture → refund (tiền về sau ngày), không undo capture magically.

Vì thế saga state phải lưu: bước nào xong, compensate tới đâu, version nào.

## 4. Isolation của saga = trạng thái domain

Không có isolation level xuyên service. Người ta mô phỏng bằng:

- **Semantic lock:** `RESERVED`, `HELD`, `PENDING_REVIEW`.
- **Counter + conditional update:** `UPDATE stock SET qty = qty - 1 WHERE qty >= 1`.
- **Unique resource:** `INSERT seat_lock(flight, seat)`; conflict = hết ghế.
- **Rereads / countermeasures** (Richardson): dirty read, lost update giữa hai saga — đọc chapter sagas, rồi tìm trong FTGO chỗ order revise.

Banking: hold là lock tiền. Logistics: reservation là lock hàng. Đó là cùng một ý.

## 5. Bộ ba bắt buộc: Outbox + Idempotency + Saga

```
Service A                          Broker                     Service B
─────────                          ──────                     ─────────
TX {
  update A
  insert outbox
}
        ── CDC / poller ──► Kafka ── at-least-once ──►
                                                      TX {
                                                        inbox unique(event_id)
                                                        update B
                                                        insert outbox reply
                                                      }
```

Lỗi điển hình:

1. Publish Kafka **sau** commit, không outbox → crash giữa hai lệnh = A xong, B không biết.
2. Consumer không idempotent → rebalance Kafka = double capture.
3. Orchestrator không persist saga instance → restart = mất bước, compensate mù.

## 6. Orchestration vs choreography (chọn theo domain)

**Orchestration:** một saga instance (FTGO CreateOrderSaga, Eventuate sagas example).

- Dễ thấy progress, timeout, compensate thứ tự.
- Banking/onboarding thích kiểu này (audit trail một chỗ).

**Choreography:** mỗi service nghe event, tự đi tiếp (Eventuate customers-and-orders choreography, ecom choreography).

- Ít điểm trung tâm, dễ nở event spaghetti.
- Logistics đơn giản (order created → warehouse nghe) có thể ổn.

Bài tập: cài **cùng** “create order + reserve credit” hai kiểu, so debug khi payment fail.

## 7. Failure drill (không drill = chưa học)

Trên project spine:

1. Double-submit API cùng idempotency key → 1 ledger entry.
2. Kill broker sau khi outbox ghi, trước khi publish → message vẫn ra sau khi lên.
3. Consumer xử lý xong, crash trước commit inbox → không double.
4. Bước 3 saga fail → bước 1–2 compensate; log đủ để người ops hiểu.
5. Compensation fail (refund API 500) → trạng thái `COMPENSATION_PENDING`, không im lặng.

Banking thêm: job nightly so `sum(ledger) == account.balance` (nếu bạn denormalize số dư).

## 8. Thứ tự đọc code

1. Eventuate `CreateOrderSaga` (orchestration, ngắn).
2. Bảng outbox trong Tram / food-ordering.
3. FTGO `CreateOrderSaga` + kitchen command handler (nhiều participant).
4. Seata TCC transfer hoặc money-transfer events (tiền).

Mỗi file, viết vào note: *invariant bị phá nếu skip bước này*.
