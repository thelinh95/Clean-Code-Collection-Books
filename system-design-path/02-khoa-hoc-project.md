# Khóa học build **một** project xuyên suốt

Tiêu chí chọn (đúng nhu cầu của bạn):

- Một domain từ đầu tới deploy, không phải 20 demo rời.
- Có Saga / transaction phân tán **trong nghiệp vụ thật**.
- Có source GitHub để đối chiếu.
- Java/Spring ưu tiên (khớp sách trong repo).

Chọn **một** khóa làm spine. Khóa thứ hai chỉ xem chapter liên quan.

## 1. Khớp nhất: Food ordering + DDD + Saga + Outbox

**Microservices: Clean Architecture, DDD, SAGA, Outbox & Kafka** — Ali Gelenler (Udemy)

- Domain: đặt món / thanh toán / nhà hàng (gần logistics + payment).
- Build 4 service Spring Boot từ đầu: customer, order, payment, restaurant.
- Pattern: Hexagonal/Clean, DDD, **Saga**, **Outbox**, CQRS, Kafka; sau đó K8s.

GitHub đi kèm (đọc song song bài giảng, đừng chỉ copy):

- https://github.com/agelenler/food-ordering-system
- https://github.com/agelenler/food-ordering-system-infra

Vì sao hợp: đúng kiểu “một project xuyên suốt”, và Saga được gắn vào **order–payment–restaurant**, không phải slide trừu tượng.

Cách học: pause trước mỗi pattern, tự implement trên branch của bạn, rồi so với repo. Ghi failure path (payment fail, restaurant reject).

## 2. Domain “hệ thống lớn” rõ hơn: Airline booking

**Java Spring Boot Microservices: Build Airline Booking System** (Udemy)

- Domain: đặt vé / GDS mini — inventory ghế, booking, payment, saga.
- Dài (~40h+), thiên production: Gateway, Kafka, Redis, JWT, Docker.

Hợp nếu bạn muốn constraint giống banking+logistics: **ghế là tài nguyên hữu hạn**, double-booking không chấp nhận được, cần lock/idempotency.

Nhược: ít “sách vở” DDD/hexagonal hơn khóa Ali; chọn nếu bạn đã quen Clean Architecture.

## 3. Gold standard (sách + app, không phải video Udemy)

**Microservice Patterns** — Chris Richardson + app FTGO

- Sách map từng chương → đúng package trong code.
- Domain: Food to Go (đặt món + giao = **logistics**).
- Saga orchestration: Create / Cancel / Revise order.
- Accounting dùng event sourcing (gần banking).

Code: https://github.com/microservices-patterns/ftgo-application

Cách học: đọc chapter 4 (sagas) rồi mở đúng `CreateOrderSaga.java`. Đừng đọc sách xong mới clone.

Trang pattern: https://microservices.io/patterns/data/saga.html

## 4. Interview / hình (bổ trợ, không thay project)

Những thứ này **không** build một backend xuyên suốt:

| Nguồn | Dùng để |
|-------|---------|
| [system-design-primer](https://github.com/donnemartin/system-design-primer) | Nền tảng scale, cache, queue; bài Pastebin/Twitter |
| [system-design-101](https://github.com/ByteByteGoHq/system-design-101) | Hình: saga, outbox, consistent hash |
| ByteByteGo / Alex Xu *System Design Interview* | Luyện phỏng vấn, vẽ box |
| MIT 6.824 | Replication, Raft — nền tảng DB/ledger |

Học xong khóa project rồi mới dùng nhóm này để **giải thích** hệ thống bạn đã build.

## 5. Transaction phân tán kiểu Java enterprise (banking)

Không phải “một khóa Udemy xuyên suốt”, nhưng là curriculum đúng cho money:

- **Seata** (AT / TCC / Saga): docs + samples — hold/confirm/cancel giống banking.
  - https://seata.apache.org/
  - https://github.com/apache/incubator-seata-samples
- Eventuate money transfer (choreography + event sourcing):
  - https://github.com/cer/event-sourcing-examples
  - Liệt kê: https://eventuate.io/exampleapps.html

Kết hợp: khóa food-ordering (kỹ năng build) + Seata/TCC + money-transfer (domain tiền).

## Cách chọn nhanh

```
Muốn code Java + DDD + Saga ngay     → Ali Gelenler + repo agelenler
Muốn chuẩn sách + logistics          → Microservice Patterns + FTGO
Muốn ghế/booking khốc liệt           → Airline booking course
Muốn banking thật                    → DDIA tx + money-transfer + Seata TCC
Muốn phỏng vấn box diagram           → Primer + ByteByteGo (sau project)
```

Tránh: mua 4 khóa, xem 20% mỗi khóa. Một spine chạy được còn hơn.
