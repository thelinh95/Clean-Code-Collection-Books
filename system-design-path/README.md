# Lộ trình System Design: học pattern trong domain thật

Cách học sai: đọc định nghĩa Saga, Outbox, CQRS như một list thuật ngữ.
Cách học đúng (ý bạn): **khi học Saga phải hiểu transaction của hệ thống lớn** — banking, logistics, booking — vì pattern chỉ tồn tại để giải quyết constraint của domain đó.

Bộ này chọn:

1. Khóa học **build một project xuyên suốt từ đầu** (không nhảy demo rời).
2. GitHub project **thiết kế tốt** để đọc code, không chỉ xem slide.
3. Map **pattern ↔ bài toán domain** (banking / logistics / ecommerce / airline).

## Đọc theo thứ tự

| # | File | Dùng khi |
|---|------|----------|
| 0 | [00-nguyen-tac.md](00-nguyen-tac.md) | Hiểu nguyên tắc: pattern + domain + constraint |
| 1 | [01-lo-trinh.md](01-lo-trinh.md) | Timeline học: local ACID → messaging → saga → CQRS |
| 2 | [02-khoa-hoc-project.md](02-khoa-hoc-project.md) | Chọn **một** khóa build full project |
| 3 | [03-github-projects.md](03-github-projects.md) | Repo vàng để clone và đọc |
| 4 | [04-pattern-x-domain.md](04-pattern-x-domain.md) | Saga/Outbox/TCC trong banking vs logistics |
| 5 | [05-saga-va-transaction.md](05-saga-va-transaction.md) | Deep dive: Saga **không tách** khỏi transaction |
| 6 | [06-project-xuyen-suot.md](06-project-xuyen-suot.md) | Đề bài project tự build (spine) |
| — | [cheatsheet.md](cheatsheet.md) | Checklist trước khi nói “đã hiểu pattern” |

## Spine khuyến nghị

Chọn **một** domain làm xương sống, đừng học 5 hệ thống song song:

- **Logistics / food-to-go** (dễ thấy saga nhất): Order → Kitchen → Accounting → Delivery.
- **Mini-bank / ledger** (transaction nghiêm nhất): hold tiền, double-entry, chuyển khoản, đối soát.

Khóa học khớp spine nhất: food-ordering (Ali Gelenler) hoặc FTGO (sách *Microservice Patterns*).
Banking: đọc money-transfer + Seata TCC, rồi tự build ledger.

## Không có trong bộ này

- Dump đề phỏng vấn / slide bản quyền.
- Copy code của khóa Udemy.
- “System design interview” thuần URL shortener nếu bạn chưa nắm consistency.
