# LeeTKOB — Cột mốc `PA#2`: Bộ khung Kiểm soát (The Harness) & Cổng Chất lượng (Quality Gate)

**Môn học:** CSC13114 — Lập trình Web Nâng cao (Năm học 2026–2027)  
**Nhóm thực hiện:**
- **Trà Văn Sỹ** — MSSV: `23120197`
- **Huỳnh Đức Thịnh** — MSSV: `23120199`
- **Lê Trung Thành Đạt** — MSSV: `23120228`

**LeeTKOB** là hệ thống hỗ trợ trợ giảng (TA) kiểm tra các ràng buộc thuật toán và cú pháp trên bài nộp C++ (`Accepted`) trước khi công bố điểm chính thức cho sinh viên. Cột mốc `PA#2` này thiết lập bộ khung kiểm soát kỹ thuật (The Harness), cổng kiểm tra tự động chặn hợp nhất trên GitHub (Quality Gate) và hiện thực đầy đủ ranh giới hợp đồng công bố điểm bài lab (`POST /v1/labs/{labId}/publish`). Theo đúng lộ trình môn học, cột mốc này chưa thực thi code C++ của sinh viên trong Sandbox, chưa gọi mô hình LLM và chưa xây dựng giao diện người dùng đầy đủ.

**Tài liệu trọng tâm trong Repository:**
- Báo cáo tổng hợp bằng chứng nộp bài: [`PA2.md`](PA2.md)
- Báo cáo tự đánh giá theo 7 tiêu chí Rubric: [`SELF_ASSESSMENT_REPORT.md`](SELF_ASSESSMENT_REPORT.md)
- Quy tắc làm việc cho AI Agent: [`AGENTS.md`](AGENTS.md) & [`CLAUDE.md`](CLAUDE.md)
- Phân quyền 3 tầng & Quản lý bí mật: [`docs/permissions.md`](docs/permissions.md) & [`.claude/settings.json`](.claude/settings.json)
- Phân tích yêu cầu & Quyết định thiết kế: [`docs/requirements.md`](docs/requirements.md)
- Hợp đồng API 5 phần (`POST /v1/labs/{labId}/publish`): [`docs/contract/publish-lab.md`](docs/contract/publish-lab.md)
- Bản đồ ánh xạ độ phủ hợp đồng (Coverage Map): [`docs/contract/coverage.md`](docs/contract/coverage.md)
- Nhật ký cộng tác với AI: [`AI-LOG.md`](AI-LOG.md)

---

## 1. Hướng dẫn Chạy từ Bản Clone Mới (Fresh Clone to Green)

### Yêu cầu môi trường
- **JDK 21 LTS** (`java -version`) — Maven đã được đóng gói sẵn trong dự án qua Maven Wrapper (`backend/mvnw` và `backend/mvnw.cmd`).
- **Node.js ≥ 22** (`node -v`) và `npm` (chỉ dùng để điều phối thống nhất các lệnh kiểm tra đa nền tảng, không yêu cầu cài đặt thư viện ngoài, không cần Docker hay máy chủ cơ sở dữ liệu rời).

### Các bước thực thi kiểm tra

```sh
git clone https://github.com/HuynhDucThinh/LeeTKOB-AWAD.git
cd LeeTKOB-AWAD
git checkout pa2
npm run lint
npm run format:check
npm run typecheck
npm test
```

### Bảng đối chiếu Một lệnh duy nhất cho mỗi loại kiểm tra (Local & CI)

| Kiểm tra bắt buộc | Lệnh chạy trên máy cá nhân (Local) | Lệnh chạy trên GitHub Actions CI | Công cụ thực thi bên dưới |
| :--- | :--- | :--- | :--- |
| **Lint** | `npm run lint` | `npm run lint` | Maven Checkstyle (`checkstyle.xml`, `failsOnError=true`) |
| **Format Check** | `npm run format:check` | `npm run format:check` | Spotless (`google-java-format`, chế độ chỉ đọc) |
| **Type Check** | `npm run typecheck` | `npm run typecheck` | Trình biên dịch tĩnh Java 21 (`mvnw test-compile`) |
| **Tests (Unit + Contract)** | `npm test` | `npm test` | JUnit 5 (23 bài HTTP Contract Tests + 9 bài Unit Tests = 32) |
| **Quét Bí mật (Secret Scan)** | `npm run sec:scan` | `npm run sec:scan` + Gitleaks `8.30.1` | Quét toàn bộ file và lịch sử Git (`git log --all -p`) |

Mỗi lệnh đều trả về mã thoát khác `0` ngay khi phát hiện lỗi. Lệnh `npm run format` được cung cấp để tự động căn chỉnh định dạng dưới máy cá nhân; trên CI luôn sử dụng lệnh chỉ đọc `npm run format:check`. Toàn bộ bài kiểm thử hợp đồng (Contract Tests) đều nằm trong `npm test` cùng với cấu hình `failIfNoTests=true` của Surefire để không bao giờ xảy ra tình trạng bỏ sót test hoặc chạy 0 test mà vẫn báo xanh.

---

## 2. Chạy Thử nghiệm Máy chủ HTTP Cục bộ (Local HTTP Demonstration)

Chỉ khởi chạy máy chủ độc lập trên máy phát triển đáng tin cậy. Xác thực luôn **fail-closed** (`AI-LOG A09`): máy chủ chỉ dùng `DemoAuthAdapter`, bắt buộc biến môi trường `DEMO_AUTH_TOKEN` dài tối thiểu 32 ký tự (thiếu hoặc yếu thì **không khởi động**) và ánh xạ duy nhất token đó tới trợ giảng mẫu `ta-demo`. Các nhãn định danh của bộ test (`own`, `admin`, …) chỉ tồn tại trong `src/test`, không bao giờ xác thực được trên máy chủ thật.

Trên PowerShell:

```powershell
$env:DEMO_AUTH_TOKEN = "<tự-sinh-chuỗi-ngẫu-nhiên-tối-thiểu-32-ký-tự>"
npm start
```

Mở một cửa sổ PowerShell thứ hai, thiết lập cùng biến `$env:DEMO_AUTH_TOKEN` và gửi yêu cầu công bố điểm cho Bài Lab 1:

```powershell
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8080/v1/labs/1/publish `
  -Headers @{ Authorization = "Bearer $env:DEMO_AUTH_TOKEN" } `
  -ContentType "application/json" `
  -Body "{}"
```

Gọi lặp lại lệnh trên lần thứ hai: nội dung JSON biên nhận trả về giữ nguyên tuyệt đối từng byte, đồng thời phản hồi lần hai có thêm header `Idempotent-Replayed: true`.

---

## 3. Cấu hình Biến Môi trường và Hạn mức Chi tiêu Mô hình (Spending Cap)

Tất cả các biến cấu hình của ứng dụng đều được khai báo tên (không chứa giá trị bí mật) trong [`.env.example`](.env.example). Các file `.env` cục bộ và file dữ liệu đều bị chặn bởi [`.gitignore`](.gitignore).

| Biến môi trường | Giá trị mặc định khi vắng mặt | Ý nghĩa |
| :--- | :--- | :--- |
| `SERVER_HOST` | `127.0.0.1` | Địa chỉ lắng nghe (`server.address` trong `application.properties`) |
| `SERVER_PORT` | `8080` | Cổng lắng nghe HTTP (`server.port`) |
| `DATABASE_URL` | `jdbc:h2:mem:leetkob;MODE=PostgreSQL...` | Chuỗi kết nối JDBC (`spring.datasource.url`) |
| `DEMO_AUTH_TOKEN` | Không có — **thiếu hoặc < 32 ký tự thì máy chủ không khởi động** | Token bí mật duy nhất của trợ giảng demo (`AppConfig`) |

Mỗi biến trên đều được đọc thật trong [`application.properties`](backend/src/main/resources/application.properties) hoặc [`AppConfig.java`](backend/src/main/java/vn/edu/hcmus/leetkob/config/AppConfig.java); không có biến nào khác.

**Nhà cung cấp và mô hình LLM đang kích hoạt ở `PA#2`: Không có (`none`). Hạn mức chi tiêu mô hình (Model Spending Cap): USD 0 / bài lab và USD 0 tổng cộng. Chi phí mô hình thực tế mỗi bài lab: USD 0.**  
Ở cột mốc `PA#2`, không có khóa API của bất kỳ nhà cung cấp mô hình nào được cấp phát, không có biến môi trường nào nhận khóa mô hình hoạt động, và không có bài kiểm thử nào gọi ra dịch vụ LLM bên ngoài. Trước khi kích hoạt tính năng kiểm tra ràng buộc bằng LLM ở `PA#3`, nhóm bắt buộc phải phê duyệt lựa chọn mô hình, thiết lập giới hạn chi phí cứng cho từng bài lab và cài đặt hạn mức chi tiêu (spending cap) trên tài khoản nhà cung cấp.

---

## 4. Kiến trúc và Đảm bảo Toàn vẹn Dữ liệu

- [`PublishController.java`](backend/src/main/java/vn/edu/hcmus/leetkob/controller/PublishController.java): Kiểm soát luồng HTTP theo đúng thứ tự 7 bước của hợp đồng, kiểm tra quyền sở hữu lớp trước khi đọc body, giới hạn kích thước payload tối đa 4096 bytes UTF-8 và chuẩn hóa mọi phản hồi lỗi theo RFC 9457 (`application/problem+json`).
- [`PublishService.java`](backend/src/main/java/vn/edu/hcmus/leetkob/service/PublishService.java): Mở giao dịch ghi, lấy khóa dòng `SELECT … FOR UPDATE` trên đúng bài lab (hết `LOCK_TIMEOUT` → `503`), kiểm tra lại quyền phân công lớp bên trong giao dịch, đảm bảo tính nguyên tử (atomic transaction) khi lưu đồng thời biên nhận (`publications`), bản chụp điểm (`published_marks`) và nhật ký kiểm toán (`publication_audit`).
- [`DemoAuthAdapter.java`](backend/src/main/java/vn/edu/hcmus/leetkob/security/DemoAuthAdapter.java) & [`MarkCalculator.java`](backend/src/main/java/vn/edu/hcmus/leetkob/domain/MarkCalculator.java): Xác thực token theo cơ chế fail-closed (so sánh hằng thời gian `MessageDigest.isEqual`) và công thức tính điểm bài lab không bao giờ âm.
- [`PublishEndpointContractTest.java`](backend/src/test/java/vn/edu/hcmus/leetkob/controller/PublishEndpointContractTest.java): Bộ 23 bài kiểm thử hợp đồng qua HTTP thật kết hợp với 9 bài Unit Tests trong [`MarkCalculatorUnitTest.java`](backend/src/test/java/vn/edu/hcmus/leetkob/domain/MarkCalculatorUnitTest.java) , [`DemoAuthAdapterUnitTest.java`](backend/src/test/java/vn/edu/hcmus/leetkob/security/DemoAuthAdapterUnitTest.java) và [`AppConfigUnitTest.java`](backend/src/test/java/vn/edu/hcmus/leetkob/config/AppConfigUnitTest.java) (tổng cộng **32 bài test**).
