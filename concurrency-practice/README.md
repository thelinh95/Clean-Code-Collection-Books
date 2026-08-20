# Luyện Java Concurrency

Bộ tài liệu này giúp ôn **concurrency / multithreading** theo phong cách đề chứng chỉ Oracle (OCP) và bài test kỹ năng kiểu oDesk/Upwork: nhiều câu bẫy, nhiều tình huống race/deadlock/visibility, rồi bài tập code để làm sau khi nắm lý thuyết.

Không copy dump đề thi có bản quyền. Câu hỏi là **đề luyện tự soạn**, bám đúng kiến thức hay ra trong exam và interview.

## Học theo thứ tự

1. Đọc [nguon.md](nguon.md) — biết nên ôn ở đâu, nguồn nào đáng tin.
2. Làm [trac-nghiem.md](trac-nghiem.md) — ~60 câu MCQ, mỗi câu có đáp án ẩn.
3. Làm [tinh-huong.md](tinh-huong.md) — predict-the-output, deadlock, JMM.
4. Ôn nhanh [cheatsheet.md](cheatsheet.md).
5. Sau khi master lý thuyết: làm [bai-tap.md](bai-tap.md) rồi code trong `exercises/`.

Chạy bài tập:

```bash
cd concurrency-practice/exercises
./run-tests.sh
```

Ban đầu test sẽ fail (stub). Làm xong, đối chiếu `exercises/solutions/`.
