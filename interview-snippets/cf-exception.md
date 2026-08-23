# CompletableFuture: exception nhiều stage

## Trả lời ngắn (câu họ hay hỏi)

**Exception ở 1 step bất kỳ có bắt được ở step cuối không?**

- **Có**, nếu chưa ai cứu giữa đường **và** step cuối là `exceptionally` / `handle` (hoặc bạn `try/catch` quanh `join()`/`get()`).
- Các `thenApply` / `thenCompose` / `thenAccept` **sau chỗ lỗi bị bỏ**, không chạy — chúng **không** phải `catch`.
- **Không còn exception** ở cuối nếu đã `exceptionally`/`handle` giữa đường (đã đổi thành giá trị bình thường).
- `whenComplete` **nhìn** được lỗi nhưng **không cứu** — lỗi vẫn chảy tiếp.

Giống `try` nhiều dòng, `catch` chỉ chạy nếu bạn thật sự viết `catch` ở dưới; `thenApply` giống dòng code thường, không phải `catch`.

## Ví dụ đời thực: đặt đồ ăn

Bạn đặt phở trên app:

```
nhận đơn  →  bếp nấu  →  shipper lấy  →  giao cửa  →  app hiện kết quả
thenApply     thenApply     thenApply      thenApply     exceptionally / handle / join
```

**Case 1 — bếp cháy món, không ai xử lý giữa đường**

Bếp hỏng (exception). Shipper không tới lấy, không ai gõ cửa. App cuối vẫn hiện: *“Nhà hàng hủy, hoàn tiền”*.

→ Lỗi ở giữa **vẫn capture được ở cuối**. Các khâu giữa bị skip.

**Case 2 — bếp cháy món, bếp đổi cơm tấm ngay**

`exceptionally` ngay sau bếp: đổi món thay thế. Shipper lấy cơm tấm, giao bình thường. App hiện thành công.

→ Đã cứu giữa đường thì **step cuối không còn thấy exception** (`exceptionally` cuối không chạy).

**Case 3 — quản lý chỉ ghi sổ**

`whenComplete`: quản lý ghi “bếp cháy”. Không nấu lại. Shipper vẫn không lấy. App vẫn hoàn tiền.

→ Ghi log ≠ bắt và cứu.

**Case 4 — app chỉ `thenApply` hiện chữ “đã nhận”**

Không có `exceptionally`. Mở hộp (`join()`) mới văng lỗi vào mặt bạn.

→ Step cuối kiểu `thenApply` **không catch**.

## Map sang API

| Đời thực | API | Có cứu lỗi? | Stage sau còn chạy? |
| --- | --- | --- | --- |
| Tô phở xong, shipper lấy | `thenApply` / `thenCompose` | không | chỉ khi step trước **OK** |
| Bếp đổi món khác | `exceptionally(ex -> fallback)` | có → thành công | có, nhận fallback |
| App luôn hiện *một* màn: OK hoặc lỗi | `handle((v, ex) -> …)` | có nếu bạn return giá trị | đây thường là khâu cuối |
| Quản lý ghi camera | `whenComplete((v, ex) -> log)` | không | nếu trước đó lỗi thì sau vẫn skip |
| Bạn mở túi / `join()` | `join()` / `get()` | ném ra, tự `try/catch` | hết pipeline |

```
lỗi chưa cứu  ──skip──► thenApply ──skip──► thenApply ──► exceptionally/handle  (bắt được)
lỗi đã cứu    ──chạy──► thenApply ──chạy──► thenApply ──► exceptionally         (không chạy)
```

## Code 4 case

Xem [`exercises/src/iv/CfExceptionPipeline.java`](exercises/src/iv/CfExceptionPipeline.java).

```java
// Case 1: bắt ở cuối
accept()
  .thenApply(o -> cook(false))   // ném
  .thenApply(f -> pickup(f))     // SKIP
  .thenApply(f -> deliver(f))    // SKIP
  .exceptionally(ex -> "Hoan tien: " + ex.getMessage()); // CHẠY

// Case 2: cứu giữa đường
accept()
  .thenApply(o -> cook(false))
  .exceptionally(ex -> "com-tam") // cứu
  .thenApply(f -> pickup(f))      // CHẠY với com-tam
  .thenApply(f -> deliver(f))
  .exceptionally(ex -> "...");    // không chạy

// Case 3: whenComplete không cứu
.thenApply(o -> cook(false))
.whenComplete((v, ex) -> log(ex)) // chỉ nhìn
.thenApply(f -> pickup(f))        // SKIP
.handle((v, ex) -> ex == null ? v : "loi"); // bắt được

// Case 4: thenApply cuối không bắt
.thenApply(o -> cook(false))
.thenApply(f -> "App nhan: " + f); // SKIP
// join() → CompletionException
```

`exceptionally` / `handle` nhận `CompletionException` bọc lỗi gốc — nhớ `ex.getCause()`.

## `allOf` / `thenCombine` (nhiều nhánh, không chỉ 1 chuỗi)

Hai quán nấu song song, gộp đơn:

- Một quán cháy → future gộp fail. `exceptionally` **sau** `allOf`/`thenCombine` vẫn bắt được.
- Quán kia có thể vẫn nấu xong (chạy song song), nhưng kết quả gộp vẫn là lỗi nếu không ai `handle` từng nhánh.

Muốn “một quán hỏng thì vẫn ăn quán còn lại”: `handle`/`exceptionally` **từng** future trước khi `allOf`.

## Câu nói 20 giây trong phỏng vấn

> Exception trên CF giống kiện hàng hỏng giữa đường: các trạm `thenApply` sau không mở kiện, nhưng bưu điện cuối (`exceptionally`/`handle`/`join`) vẫn nhận được phiếu lỗi — trừ khi một trạm giữa đã đổi kiện mới, lúc đó cuối đường chỉ thấy hàng thay thế.
