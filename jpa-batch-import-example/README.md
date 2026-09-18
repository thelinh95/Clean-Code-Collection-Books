# JPA / Hibernate memory-safe batch import (10_000 records)

Ví dụ Spring Boot 3 + Spring Data JPA + Hibernate import **10.000 order line** từ CSV, không nhét toàn bộ entity vào persistence context.

## Mô hình quan hệ

```
Customer 1 ──< SalesOrder 1 ──< OrderLine >── 1 Product
```

- `Customer` `@OneToMany` `SalesOrder`
- `SalesOrder` `@ManyToOne` `Customer`, `@OneToMany` `OrderLine`
- `OrderLine` `@ManyToOne` `SalesOrder` và `Product`
- `Product` `@OneToMany` `OrderLine`

**Không** `CascadeType.ALL` trên collection. Import persist từng tầng, không đi qua parent graph.

## Vì sao làm vậy

| Việc | Mục đích |
|---|---|
| `GenerationType.SEQUENCE` + `allocationSize = 50` | JDBC batch chạy được (IDENTITY thì không) |
| `hibernate.jdbc.batch_size=50` | Hibernate gom 50 INSERT / statement |
| `hibernate.order_inserts=true` | Gom INSERT cùng entity type |
| Chunk 500 + `flush()` + `clear()` | Session tối đa ~500 entity |
| Đọc CSV bằng `Stream` (2 pass) | Không `readAllLines()` 10k row vào List lớn |
| `entityManager.getReference()` | Set FK không `SELECT` Customer/Product/Order |
| Lookup `code → id` (Map) | Không giữ managed entity của master data |
| `show-sql=false` | Log không phình hơn chính import |
| `open-in-view=false` | Không giữ session ngoài transaction |

## Cấu hình

Dùng **flat key** (không nest `hibernate.jdbc.batch_size` trong YAML — Spring sẽ không truyền đúng cho Hibernate):

```properties
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
```

Hoặc `HibernatePropertiesCustomizer` trong `HibernateBatchConfig`.

`flush()` **không** `clear()`. `clear()` mới nhả first-level cache.

## CSV

```
order_code,customer_code,ordered_at,product_sku,quantity,unit_price
ORD-000001,CUST-001,2026-01-15T00:01:00Z,SKU-001,2,11.00
```

Luồng import:

1. Seed Customer + Product (id map).
2. Pass 1 — stream file, persist `SalesOrder` unique, nhớ `orderCode → id`.
3. Pass 2 — stream file lần nữa, persist `OrderLine` với `getReference(SalesOrder)` và `getReference(Product)`.

Mỗi 500 entity: `persist` → `flush` → `clear` → `chunk.clear()`.

## Chạy demo

```bash
cd jpa-batch-import-example
mvn test
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Demo ghi CSV 10.000 dòng rồi import vào H2 in-memory.

## File chính

- `domain/` — entity + quan hệ
- `importdata/OrderImportService.java` — import 2 pass
- `importdata/ChunkPersister.java` — persist + flush + clear
- `importdata/CsvRowStream.java` — đọc file lazy
- `importdata/DemoCsvGenerator.java` — tạo CSV 10k dòng
- `config/HibernateBatchConfig.java` — bật `hibernate.jdbc.batch_size` (không dùng nested YAML)
