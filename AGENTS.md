# Quy tắc làm việc cho AI Agent — Dự án LeeTKOB (`AGENTS.md`)

Tài liệu này là nguồn quy tắc duy nhất cho mọi trợ lý lập trình AI (Claude Code, Codex, Cursor, Antigravity) làm việc trên repository này. File `CLAUDE.md` nạp trực tiếp nội dung từ đây.

## 1. Stack

- **Backend (`backend/`):** Java 21 LTS, Spring Boot 3.4.3, Maven Wrapper (`./mvnw` / `mvnw.cmd`), Spring JDBC + H2 SQL (chế độ tương thích PostgreSQL `MODE=PostgreSQL`), chuẩn lỗi HTTP Problem Details (RFC 9457).
- **Trình điều phối lệnh gốc:** Node.js $\ge$ 22 (`package.json` + `scripts/mvnw.mjs`) giúp đồng nhất lệnh kiểm tra trên Windows, macOS và Linux CI.
- **Phạm vi cột mốc `PA#2`:** Chỉ cài đặt ranh giới công bố điểm bài lab (`POST /v1/labs/{labId}/publish`) cùng bộ khung kiểm soát chất lượng (Harness & Quality Gate). Chưa thực thi code C++ của sinh viên trong Sandbox và chưa kết nối mô hình LLM ở `PA#2`.

## 2. Commands

Chạy từ thư mục gốc của repository. Cả 4 lệnh kiểm tra dưới đây đều chỉ đọc (read-only), trả về mã thoát khác `0` khi có lỗi, và được gọi **giống hệt nhau** trên máy cá nhân lẫn trong GitHub Actions CI:

```sh
npm run lint          # Kiểm tra quy tắc mã nguồn Java bằng Checkstyle
npm run format:check  # Kiểm tra định dạng chuẩn Google Java Format bằng Spotless
npm run typecheck     # Kiểm tra kiểu dữ liệu và biên dịch toàn bộ main + test
npm test              # Chạy toàn bộ Unit Tests và HTTP Contract Tests (JUnit 5)
```

- Lệnh tự động sửa định dạng (chỉ dùng dưới máy local): `npm run format`.
- Lệnh quét lộ lọt secret trên toàn bộ lịch sử Git: `npm run sec:scan`.
- Bắt buộc chạy xanh cả 4 lệnh kiểm tra trước khi mở hoặc cập nhật bất kỳ Pull Request nào.

## 3. Conventions

- Đọc kỹ hợp đồng đặc tả tại `docs/contract/publish-lab.md` trước khi sửa mã nguồn trong `backend/src/main/`. Mọi phản hồi lỗi phải tuân thủ chuẩn **RFC 9457 (`application/problem+json`)** kèm `Cache-Control: no-store` và `X-Content-Type-Options: nosniff`.
- Kiểm tra quyền sở hữu lớp của trợ giảng (`ta_classes`) cả trước khi phân tích cú pháp body lẫn bên trong giao dịch ghi (`@Transactional`) để chống rò rỉ trạng thái bài lab của lớp khác (truy vết từ sự cố `AI-LOG A07`).
- Lưu biên nhận công bố (`publications`), bản chụp điểm số (`published_marks`) và sự kiện kiểm toán (`publication_audit`) nguyên tử (atomic) trong cùng một giao dịch SQL.
- Khi kiểm thử trạng thái không có tác dụng phụ (no-side-effect) trên cơ sở dữ liệu, truy vấn trực tiếp giá trị vô hướng `COUNT(*)` trên từng bảng thay vì dựa vào kiểu đối tượng trung gian của driver (truy vết từ sự cố `AI-LOG A03`).
- Mọi thay đổi sau khi bật bảo vệ nhánh `main` đều phải đi qua Pull Request và điền đầy đủ `.github/pull_request_template.md`. Tuân thủ nghiêm ngặt 3 tầng quyền hạn trong `docs/permissions.md` và `.claude/settings.json`.

## 4. Never

- **Không bao giờ** đưa secret (API key, token, mật khẩu) vào mã nguồn, file test, log, lịch sử commit, hay vào trong môi trường Sandbox chạy code C++ của sinh viên ở các cột mốc sau. Sandbox tương lai tuyệt đối không có mạng, không chạy quyền `root`, không gắn host mount và không truy cập Docker socket.
- **Không bao giờ** gọi mô hình LLM thật từ Unit Tests hoặc Contract Tests. Tại `PA#2`, ngân sách và chi tiêu cho mô hình LLM bị khóa cứng ở mức **USD 0**.
- **Không bao giờ** sửa đổi bài test hiện có, dữ liệu đánh giá (`eval/`), hoặc nhãn đáp án (`labels.jsonl`) để ép một bước kiểm tra đang đỏ thành xanh. Nếu hợp đồng thực sự thay đổi, phải cập nhật `docs/contract/publish-lab.md` và giải trình trong một commit/PR độc lập.
- **Không bao giờ** bỏ qua bước kiểm tra quyền sở hữu lớp (`ta_classes`) hoặc coi việc đã xác thực (`401`) là đủ quyền truy cập bài lab của lớp khác (truy vết từ sự cố `AI-LOG A07`).
- **Không bao giờ** dùng biến cờ trong bộ nhớ (in-memory flag) để xử lý Idempotency hoặc trả về mã `201 Created` / tạo mới timestamp khi gọi lặp lại (replay) trên bài lab đã công bố (truy vết từ sự cố `AI-LOG A06`).
- **Không bao giờ** thêm `|| true`, `continue-on-error: true`, `--passWithNoTests`, hoặc push trực tiếp / force-push lên nhánh `main`.

