# Luyện Java Concurrency

Bộ tài liệu này giúp ôn **concurrency / multithreading** theo phong cách đề chứng chỉ Oracle (OCP) và bài test kỹ năng kiểu oDesk/Upwork: nhiều câu bẫy, nhiều tình huống race/deadlock/visibility, rồi bài tập code để làm sau khi nắm lý thuyết.

Không copy dump đề thi có bản quyền. Câu hỏi là **đề luyện tự soạn**, bám đúng kiến thức hay ra trong exam và interview.

## Học theo thứ tự

1. Đọc [theo-chu-de.md](theo-chu-de.md) — map topic (Oracle / LeetCode / MCQ).
2. Đọc [nguon.md](nguon.md) — nguồn hợp pháp (không dump đề thi).
3. Làm [oracle-theo-chu-de.md](oracle-theo-chu-de.md) + [trac-nghiem.md](trac-nghiem.md).
4. Làm [tinh-huong.md](tinh-huong.md).
5. Ôn [cheatsheet.md](cheatsheet.md).
6. Code: [leetcode.md](leetcode.md) + [bai-tap.md](bai-tap.md) + `exercises/`.

Chạy bài tập:

```bash
cd concurrency-practice/exercises
./run-tests.sh
```

Ban đầu test sẽ fail (stub). Làm xong, đối chiếu `exercises/solutions/`.
