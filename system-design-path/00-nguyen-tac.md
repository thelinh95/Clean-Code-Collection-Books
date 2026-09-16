# Nguyên tắc: đừng học pattern trần

Một pattern system design luôn trả lời **ba câu cùng lúc**:

1. **Domain:** tiền bị trừ hai lần thì sao? kho hết hàng giữa lúc thanh toán thì sao?
2. **Constraint:** latency, consistency, pháp lý, audit, retry mạng.
3. **Pattern:** Saga, Outbox, TCC, CQRS… chỉ là tên của cách xử lý (1)+(2).

Nếu bỏ (1) và (2), bạn nhớ được buzzword nhưng không thiết kế được hệ thống.

## Ví dụ bạn nêu: học Saga phải học transaction

Saga **không phải** “gọi nhiều service theo thứ tự”.
Saga là câu trả lời cho: *làm sao giữ đúng nghiệp vụ khi không còn một ACID transaction xuyên nhiều database*.

Muốn hiểu câu đó, phải biết trước:

| Nền tảng | Vì sao bắt buộc |
|----------|-----------------|
| ACID local (1 DB) | Mỗi bước saga vẫn là một transaction cục bộ |
| Isolation (READ COMMITTED vs SERIALIZABLE) | Hai order cùng trừ inventory: lost update / oversell |
| 2PC / XA | Vì sao banking lớn **không** tin 2PC xuyên data center |
| Compensation ≠ rollback | Rollback SQL hoàn nguyên row; compensation là **nghiệp vụ ngược** (hoàn tiền, nhả kho) |
| Idempotency | Kafka/HTTP retry → trừ tiền 2 lần nếu không có idempotency key |
| Pending / semantic lock | Order `PENDING`, tiền `HOLD`, ghế `RESERVED` — đó là isolation ở tầng domain |

Banking và logistics là hai phòng thí nghiệm tốt nhất vì constraint **không tha**:

- **Banking:** tiền không được tạo/mất; cần ledger, hold/capture, reconciliation.
- **Logistics:** không bán quá tồn; reservation hết hạn; pick/pack/ship; reverse logistics.

## Vòng học 4 bước (lặp cho mọi pattern)

Với mỗi pattern (Saga, Outbox, CQRS, Event Sourcing, Inbox…):

1. **Bài toán domain** — viết 5–10 dòng: ai mất gì nếu fail giữa chừng.
2. **Constraint** — consistency? latency? audit? legal?
3. **Cơ chế** — sequence diagram happy path + 2 failure path.
4. **Code thật** — mở đúng file saga/outbox trong một GitHub project (xem [03-github-projects.md](03-github-projects.md)).

Đừng bước 3 rồi dừng. Không có bước 4 thì kiến thức bay sau 2 tuần.

## Pattern không đứng một mình

Các cụm **phải học cùng nhau** (không tách chương):

```
Local ACID  +  Outbox  +  Consumer Inbox/Idempotency  +  Saga
       \         |                    |                   /
        \--------|-------- messaging -|------------------/
                         |
                    Observability
                 (traceId xuyên saga)
```

Học Saga mà bỏ Outbox = dual-write: commit DB xong, publish Kafka fail → hệ thống lệch.
Học Outbox mà bỏ idempotent consumer = event deliver 2 lần → trừ tiền 2 lần.
Học cả hai mà không có trace = production không debug được bước nào fail.

Chi tiết cụm này: [05-saga-va-transaction.md](05-saga-va-transaction.md).
