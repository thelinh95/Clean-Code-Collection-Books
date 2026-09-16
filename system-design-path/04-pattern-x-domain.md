# Pattern × domain: cùng tên, khác invariant

Học pattern theo **cột domain**, không theo list Wikipedia.

## Bảng nhanh

| Pattern | Banking | Logistics / food delivery | Ecommerce checkout | Airline / booking |
|---------|---------|---------------------------|--------------------|-------------------|
| **Local ACID** | Double-entry 1 ledger DB: debit+credit cùng commit | Reserve 1 dòng inventory + ghi reservation | Trừ coupon + tạo order line nếu cùng DB | Giữ ghế trong 1 DB inventory |
| **Saga** | Disburse loan: mở TK → giải ngân → kế toán → thông báo; fail thì reverse bút toán | Create order: validate consumer → authorize card → create ticket kitchen → … | Order → inventory → payment → shipment | Hold ghế → thanh toán → ticket; fail thì release ghế |
| **TCC** | `Try` hold số dư, `Confirm` capture, `Cancel` release | `Try` reserve SKU, `Confirm` allocate, `Cancel` hết TTL | Hold flash-sale quota | Hold seat 15 phút |
| **Outbox** | Ghi bút toán + event `MoneyTransferred` cùng TX | `StockReserved` không được mất khi commit kho | `OrderPlaced` để fulfillment | `SeatHeld` tới payment service |
| **Inbox / idempotency** | Webhook payment: cùng `txnId` không cộng tiền 2 lần | `ShipCommand` retry không tạo 2 vận đơn | Trừ kho 2 lần vì Kafka rebalance | Double-click đặt vé |
| **CQRS** | Statement / số dư realtime vs ledger append-only | Tracking page vs WMS | Catalog search vs checkout write | Tìm chuyến (đọc) vs book (ghi) |
| **Event sourcing** | Ledger *là* nguồn sự thật | Tracking mốc scan | Ít khi bắt buộc | Ít khi bắt buộc |
| **Reconciliation** | Cuối ngày: so core banking vs card network | So tồn kho hệ thống vs kiểm kê | Chargeback vs order status | PNR vs inventory hãng |

## Banking: invariant “tiền không tự sinh”

Bài toán gốc: chuyển A → B, hoặc thanh toán merchant.

Constraint:

- Không âm số dư (trừ overdraft có hợp đồng).
- Mọi thay đổi số dư = bút toán; reverse cũng là bút toán mới, không `UPDATE` xóa lịch sử.
- Retry mạng là bình thường (at-least-once).
- Pháp lý / audit: giải thích được từng đồng.

Pattern thường đi cùng:

1. Ledger append-only (gần event sourcing).
2. Idempotency key = `transferId`.
3. Hold/capture (TCC) cho authorization thẻ / chuyển khoản chờ.
4. Saga khi luồng xuyên nhiều hệ: fraud → ledger → notification → reporting.
5. Reconciliation job — **không optional**.

Repo: `cer/event-sourcing-examples`, Seata `tcc-transfer`.

Saga banking **không** “undo UPDATE balance”. Compensation là: bút toán đối ứng + đổi trạng thái `FAILED` + alert nếu không khớp.

## Logistics: invariant “không oversell, hàng có vị trí”

Bài toán gốc: nhận order → giữ hàng → pick/pack → bàn giao vận chuyển.

Constraint:

- Tồn kho là số hữu hạn; race khi flash sale.
- Reservation có TTL (giỏ hàng bỏ).
- Trạng thái vật lý (đã pick) không rollback bằng SQL; cần quy trình ngược (put-back).
- Nhiều bên: warehouse, 3PL, shipper.

Pattern:

1. Semantic lock: `RESERVED` trên SKU/location.
2. Saga: Order → Inventory → Payment → Fulfillment (thứ tự payment vs inventory là **quyết định nghiệp vụ**).
3. Compensation: release reservation; nếu đã pick thì tạo reverse task, không `DELETE` reservation lặng lẽ.
4. Outbox: `Allocated`, `Shipped`, `Delivered`.

Repo: FTGO (kitchen ticket ~ allocation), food-ordering Ali, ecom saga.

## Ecommerce: invariant “checkout không double-charge”

Giống logistics + payment. Thêm:

- Coupon/điểm: trừ 2 lần nếu không idempotent.
- Payment gateway webhook vs redirect (hai kênh, cùng payment).
- Partial capture, refund một phần.

Nhìn ecom choreography vs orchestration (hai repo mục C trong [03-github-projects.md](03-github-projects.md)).

## Airline: invariant “ghế là unique resource”

Gần banking (tiền) + logistics (tồn):

- Seat map = inventory có identity (không chỉ `count`).
- Overbooking là **chính sách**, không phải bug — phải model explicit.
- Hold TTL, thanh toán timeout, GDS khác hãng.

Khóa airline booking (file khóa học) + tự so với TCC hold/confirm.

## Câu hỏi bắt buộc trước khi chọn pattern

Viết ra giấy cho use case:

1. Fail giữa bước 2 và 3: tiền/hàng/ghế đang ở trạng thái nào?
2. Retry bước 2: có side effect 2 lần không?
3. Cần thấy kết quả ngay (UX) hay chấp nhận “đang xử lý”?
4. Có quy trình người (ops) khi compensation fail không?
5. Có job đối soát không? Đối soát cái gì với cái gì?

Không trả lời được thì chưa tới lúc vẽ Kafka.
