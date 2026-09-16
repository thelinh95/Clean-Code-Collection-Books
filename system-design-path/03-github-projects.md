# GitHub project nên đọc (thiết kế tốt)

Không clone 20 repo. Với mỗi repo: chạy local (nếu được) → tìm **saga / outbox / domain event** → vẽ lại sequence → cố tình fail 1 bước.

Cách đọc một repo distributed (đừng bắt đầu từ `pom.xml`):

1. README + docker-compose: service nào, broker nào, DB nào.
2. Tìm class `*Saga*`, `*Outbox*`, `*CommandHandler*`, `*EventHandler*`.
3. Tìm bảng `outbox`, `inbox`, `saga_instance`, trạng thái aggregate (`PENDING`).
4. Happy path rồi compensation path.
5. Test: integration test saga còn quý hơn unit test controller.

## A. Nên học sâu (gold)

### 1. FTGO — logistics + saga + CQRS + event sourcing

https://github.com/microservices-patterns/ftgo-application

App của sách *Microservice Patterns*. Domain giao đồ ăn:

- `CreateOrderSaga`, `CancelOrderSaga`, `ReviseOrderSaga`
- Participant: Order, Kitchen, Accounting, Consumer
- CQRS: `ftgo-order-history-service`
- Event sourcing: `Account` trong accounting-service

Đọc khi: phase 4–5 của [01-lo-trinh.md](01-lo-trinh.md).
Cảnh báo: stack Eventuate, setup nặng hơn demo Udemy.

### 2. Eventuate Customers & Orders — saga tối giản

Orchestration:

https://github.com/eventuate-tram/eventuate-tram-sagas-examples-customers-and-orders

Choreography + CQRS view:

https://github.com/eventuate-tram/eventuate-tram-examples-customers-and-orders

Use case nhỏ: tạo order + reserve credit (mầm **banking**: hạn mức).
Đọc orchestrator `CreateOrderSaga` rồi so với bản choreography. Cùng bài toán, hai kiểu phối hợp.

Outbox nằm trong https://github.com/eventuate-tram/eventuate-tram-core

### 3. Food ordering (khóa Ali) — hexagonal + outbox tự viết

https://github.com/agelenler/food-ordering-system

Bốn service, Saga + Outbox + Kafka, Clean/Hexagonal.
Dễ map 1-1 với video. Tốt để **tự gõ lại**, không phải để “nghiên cứu framework Eventuate”.

### 4. Money transfer — banking choreography + event sourcing

https://github.com/cer/event-sourcing-examples

Chuyển tiền giữa account (Java Spring trong `java-spring/`).
Đúng tinh thần “học saga thì phải hiểu transaction banking”: ledger, event, view CQRS.

## B. Banking / TCC / Seata

### 5. Apache Seata samples — AT vs TCC vs Saga

https://github.com/apache/incubator-seata-samples

Nhìn `tcc-transfer` (hold tiền / confirm / cancel) và so với Saga compensation.
Banking thích TCC vì **reserve** rõ (blocked balance), không phải trừ rồi hoàn lung tung.

Docs: https://seata.apache.org/docs/user/saga/

### 6. Banking-as-a-service (tham khảo, không gold)

https://github.com/rajeswarandhandapani/saga-orchestrated-banking-as-service

Onboarding, account, payment, saga orchestrator, Kafka.
Đọc architecture và saga lifecycle; đừng coi mọi “production-ready” trên README là production.

## C. Ecommerce (choreography vs orchestration)

### 7. Ecom choreography + CDC + observability

https://github.com/tahaberkamcadev/ecom

Saga choreography, transactional outbox, Debezium CDC, CQRS, Prometheus/Grafana.
Tốt để thấy **ops** (lag, dashboard) — thứ sách hay bỏ.

### 8. Ecom orchestration tập trung

https://github.com/pacman-cli/e-commerce

Order orchestrator: reserve inventory → payment → compensate.
So với repo (7): cùng domain, khác kiểu điều phối — làm bài tập “chọn choreography hay orchestration”.

## D. Theory / hình (không phải backend chạy)

- https://github.com/donnemartin/system-design-primer (~cỡ lớn nhất GitHub chủ đề này)
- https://github.com/ByteByteGoHq/system-design-101

Dùng để ôn thuật ngữ sau khi đã đụng code.

## E. Cố ý **không** lấy làm mẫu saga

https://github.com/spring-petclinic/spring-petclinic-microservices

Giỏi Spring Cloud (gateway, discovery, config). **Không** dạy distributed transaction.
Học xong gateway rồi quay lại FTGO/Eventuate.

## Lịch đọc 4 tuần (một người, tối)

| Tuần | Repo | Việc |
|------|------|------|
| 1 | Eventuate saga customers-and-orders | Chạy docker, tạo order, làm customer hết hạn mức |
| 2 | agelenler food-ordering **hoặc** tự code theo khóa | Outbox table + publish Kafka |
| 3 | FTGO CreateOrderSaga + Kitchen command handler | Vẽ compensation; kill 1 service giữa chừng |
| 4 | money-transfer **hoặc** Seata tcc-transfer | Hold/capture; đối soát số dư vs event log |

Song song: ghi note vào [04-pattern-x-domain.md](04-pattern-x-domain.md) theo cột domain bạn chọn.
