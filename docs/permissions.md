# Phân quyền cho Trợ lý AI và Quản lý Bí mật (`docs/permissions.md`)

Tài liệu này quy định cụ thể **3 tầng quyền hạn (Three Tiers of Action)** của trợ lý AI khi làm việc trên dự án **LeeTKOB**, cách thức quản lý biến môi trường và hạn mức chi tiêu cho mô hình LLM ở cột mốc `PA#2`. Các quy tắc này được thực thi tự động thông qua `.claude/settings.json` và quy trình kiểm tra trên GitHub Actions.

---

## 1. Ba tầng quyền hạn của Trợ lý AI (Three Permission Tiers)

### Tầng 1 — Được phép thực hiện ngay (Allowed / No Prompt)
- Đọc toàn bộ mã nguồn, tài liệu hợp đồng (`docs/contract/`), báo cáo và các tài liệu đặc tả của môn học trong repository.
- Chạy các lệnh kiểm tra tất định, không làm thay đổi trạng thái hệ thống ngoài vùng làm việc:
  - `npm run lint`
  - `npm run format:check`
  - `npm run typecheck`
  - `npm test`
  - `npm run sec:scan`
  - `npm run check`
  - `git status`, `git diff`, `git log`
- Viết hoặc chỉnh sửa mã nguồn trong `backend/src/main/`, bổ sung các bài kiểm thử mới trong `backend/src/test/`, và cập nhật tài liệu báo cáo theo đúng yêu cầu của hợp đồng.

### Tầng 2 — Phải hỏi và được con người phê duyệt trước (Ask First)
- Thêm mới, nâng cấp hoặc gỡ bỏ bất kỳ thư viện phụ thuộc nào trong `backend/pom.xml` hoặc `package.json`.
- Chỉnh sửa cấu hình đường ống CI/CD (`.github/workflows/ci.yml`), quy tắc Checkstyle/Spotless, cấu trúc lược đồ cơ sở dữ liệu (`schema.sql`), hoặc nội dung hợp đồng đã chốt (`docs/contract/publish-lab.md`).
- Thực hiện các thao tác Git làm thay đổi lịch sử hoặc trạng thái từ xa: `git commit`, `git push` lên nhánh tính năng, mở hoặc hợp nhất (merge) Pull Request trên GitHub.
- Bất kỳ thao tác nào liên quan đến việc kết nối dịch vụ trả phí bên ngoài hoặc thay đổi cài đặt của repository.

### Tầng 3 — Cấm tuyệt đối (Never / Blocked)
- Đọc, in ra màn hình, ghi vào log, hoặc chỉnh sửa file `.env` và bất kỳ file nào chứa khóa bí mật thật.
- Đưa token xác thực, mật khẩu cơ sở dữ liệu, hoặc khóa API của mô hình LLM vào mã nguồn, lịch sử Git, test fixtures, hoặc vào bên trong môi trường Sandbox chấm code C++ của sinh viên.
- Sửa đổi dữ liệu đánh giá (`eval/`), nhãn đáp án (`labels.jsonl`), hoặc nới lỏng/xóa bỏ các câu lệnh `assert` trong `backend/src/test/` nhằm làm cho một bản build đang lỗi vượt qua cổng kiểm tra CI.
- Chạy các lệnh phá hủy hoặc vượt rào bảo vệ: `git push --force`, `git push origin main`, xóa nhánh bảo vệ, tắt Branch Protection, hoặc dùng `rm -rf`.

---

## 2. Quản lý Biến môi trường (`.env.example`)

Tất cả các biến cấu hình của ứng dụng được liệt kê (chỉ gồm tên biến, **không chứa giá trị**) trong file [`.env.example`](../.env.example):

| Tên biến | Giá trị mặc định khi chạy dev/test | Ý nghĩa |
| :--- | :--- | :--- |
| `SERVER_HOST` | `127.0.0.1` | Địa chỉ lắng nghe cục bộ của HTTP server |
| `SERVER_PORT` | `8080` | Cổng HTTP hợp lệ (`1`–`65535`) |
| `DATABASE_URL` | `jdbc:h2:mem:leetkob;MODE=PostgreSQL...` | Chuỗi kết nối cơ sở dữ liệu SQL |
| `DEMO_AUTH_TOKEN` | Không có — thiếu hoặc < 32 ký tự thì máy chủ **không khởi động** | Token bí mật duy nhất của trợ giảng demo (`AppConfig`, fail-closed — `AI-LOG A09`) |

- File `.env` và mọi biến thể `.env.*` (trừ `.env.example`) đều bị chặn bởi [`.gitignore`](../.gitignore).
- Ứng dụng khi chạy độc lập (standalone server) sẽ kiểm tra cấu hình ngay lúc khởi động (fail-closed): nếu `DEMO_AUTH_TOKEN` bị bỏ trống hoặc ngắn hơn 32 ký tự, bộ xác thực demo sẽ từ chối khởi tạo để ngăn chặn việc chạy server với khóa mặc định yếu.

---

## 3. Nhà cung cấp Mô hình LLM và Hạn mức Chi tiêu (Spending Cap)

- **Nhà cung cấp và mô hình đang kích hoạt ở `PA#2`:** **Không có (`none`)**.
- **Hạn mức chi tiêu (Model Spending Cap) ở `PA#2`:** **USD 0 / bài lab và USD 0 tổng cộng**.
- **Chi phí thực tế cho mỗi bài lab ở `PA#2`:** **USD 0 (0 cuộc gọi mô hình)**.
- **Giải trình kỹ thuật:** Cột mốc `PA#2` chỉ xây dựng bộ khung kiểm soát (Harness), cổng chất lượng CI (Quality Gate) và ranh giới hợp đồng công bố điểm (`POST /v1/labs/{labId}/publish`). Không có khóa API của bất kỳ nhà cung cấp LLM nào (OpenAI, Google Gemini, Anthropic) được cấp phát hay nạp vào dự án ở giai đoạn này. Trước khi bước sang `PA#3` (tích hợp tính năng kiểm tra ràng buộc bằng LLM), nhóm bắt buộc phải thiết lập giới hạn chi tiêu cứng (hard spending cap) trên bảng điều khiển của nhà cung cấp và cơ chế chặn chi phí theo từng bài lab trong ứng dụng.

---

## 4. Quét Bí mật Lịch sử Git và Quy trình Ứng phó Sự cố

- Toàn bộ lịch sử Git (`git log --all -p`) và cây thư mục làm việc được quét tự động ở mỗi lần chạy kiểm tra bằng cả hai lớp:
  1. Script kiểm tra nhanh cục bộ `npm run sec:scan` ([`scripts/secret-scan.mjs`](../scripts/secret-scan.mjs)).
  2. Job **`secrets`** bắt buộc trên GitHub Actions sử dụng công cụ **Gitleaks 8.30.1** (được xác thực mã băm SHA-256 chính thức trước khi thực thi).
- **Quy trình ứng phó nếu phát hiện lộ secret:** Ngay lập tức thu hồi và xoay vòng (revoke & rotate) khóa bị lộ trên hệ thống cấp phát, bởi một khi khóa đã xuất hiện trong lịch sử Git hoặc log của phiên chat AI thì phải coi như đã bị lộ công khai (xóa file ở commit kế tiếp không làm biến mất khóa trong `git log -p`). Sau khi xoay vòng khóa mới, ghi nhận trung thực sự cố vào `AI-LOG.md`.

