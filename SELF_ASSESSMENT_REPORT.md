# Báo cáo Tự đánh giá Cột mốc `PA#2` — Dự án LeeTKOB (`SELF_ASSESSMENT_REPORT.md`)

- **Môn học:** CSC13114 — Lập trình Web Nâng cao (Năm học 2026–2027)
- **Cột mốc:** `PA#2` — Harness and Quality Gate (4% tổng điểm môn học · Thang điểm 100)
- **Mã nhóm:** `23120197-23120199-23120228`
- **Repository công khai:** [`https://github.com/HuynhDucThinh/LeeTKOB-AWAD`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD)
- **Tag nộp bài:** [`pa2`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/tree/pa2)
- **Tổng điểm tự đánh giá:** **100 / 100**

---

## 1. Thành viên Nhóm và Phân bổ Trách nhiệm

| Họ và tên | MSSV | Tỷ lệ đóng góp | Phạm vi phụ trách chính |
| :--- | :---: | :---: | :--- |
| **Trà Văn Sỹ** | `23120197` | 1/3 (33.3%) | Thiết lập `AGENTS.md`, `CLAUDE.md`, phân quyền 3 tầng (`docs/permissions.md`, `.claude/settings.json`), `.env.example` và cấu hình quét bí mật lịch sử Git (`Gitleaks` / `sec:scan`). |
| **Huỳnh Đức Thịnh** | `23120199` | 1/3 (33.3%) | Khởi tạo kiến trúc Spring Boot 3 (Java 21), chuẩn hóa 4 lệnh kiểm tra đa nền tảng, thiết lập GitHub Actions CI 5 jobs, cấu hình Branch Protection trên nhánh `main` và tổng hợp hồ sơ `PA2.md` / `SELF_ASSESSMENT_REPORT.md`. |
| **Lê Trung Thành Đạt** | `23120228` | 1/3 (33.3%) | Soạn thảo hợp đồng 5 phần (`docs/contract/publish-lab.md`), bản đồ độ phủ (`docs/contract/coverage.md`), cài đặt `PublishController` / `PublishService` và viết bộ 28 bài kiểm thử JUnit 5 (Contract Tests + Unit Tests). |

---

## 2. Bảng Tự chấm điểm Chi tiết theo 7 Tiêu chí Rubric

| # | Tiêu chí trong Rubric `PA#2` | Điểm tối đa | Điểm tự chấm | Bằng chứng cụ thể và Lý do tự đánh giá |
| :-: | :--- | :-: | :-: | :--- |
| **1** | **Rules file** | 15 | **15** | [`AGENTS.md`](AGENTS.md) (được nạp bởi [`CLAUDE.md`](CLAUDE.md)) ngắn gọn, súc tích gồm đúng 4 mục: `Stack`, `Commands`, `Conventions`, `Never`. Danh sách `Never` gắn chặt với đặc thù của LeeTKOB: cấm đưa secret vào mã nguồn/lịch sử/sandbox, cấm gọi mô hình LLM thật trong unit/contract tests, cấm sửa test/eval data/labels để ép check vượt qua. Quy tắc kiểm tra `COUNT(*)` trong DB truy vết trực tiếp tới sự cố `A03`, quy tắc giữ nguyên trạng thái replay truy vết tới `A06`, và quy tắc kiểm tra quyền sở hữu lớp trước khi đọc body truy vết tới `A07` trong [`AI-LOG.md`](AI-LOG.md). |
| **2** | **Permissions and secrets** | 10 | **10** | [`docs/permissions.md`](docs/permissions.md) và [`.claude/settings.json`](.claude/settings.json) quy định rõ 3 tầng quyền hạn (`Allowed`, `Ask first`, `Never`). [`.env.example`](.env.example) liệt kê đầy đủ 4 biến cấu hình với giá trị rỗng; các file `.env` bị chặn bởi [`.gitignore`](.gitignore). Toàn bộ lịch sử Git được quét tự động bằng `npm run sec:scan` và **Gitleaks 8.30.1** (0 phát hiện rò rỉ). [`README.md`](README.md) và [`docs/permissions.md`](docs/permissions.md) nêu rõ ở `PA#2` chưa kết nối mô hình LLM (`none`) và hạn mức chi tiêu (spending cap) được khóa ở mức **USD 0 / bài lab và USD 0 tổng cộng**. |
| **3** | **One command for every check** | 10 | **10** | [`package.json`](package.json), [`README.md`](README.md) và [`.github/workflows/ci.yml`](.github/workflows/ci.yml) sử dụng **chính xác cùng 4 câu lệnh**: `npm run lint` (Checkstyle), `npm run format:check` (Spotless Google Java Format), `npm run typecheck` (`mvnw test-compile`), và `npm test` (JUnit 5). Bản clone mới chỉ cần JDK 21+ và Node 22+ là chạy xanh toàn bộ 4 lệnh mà không cần cài đặt Docker hay cơ sở dữ liệu rời. |
| **4** | **CI that blocks a merge** | 20 | **20** | Nhánh `main` được bật bảo vệ ([`docs/evidence/branch-protection.md`](docs/evidence/branch-protection.md), [`docs/evidence/main-protection.json`](docs/evidence/main-protection.json)), yêu cầu Pull Request, bắt buộc vượt qua cả 5 checks (`lint`, `format-check`, `typecheck`, `test`, `secrets`) và cấm bypass kể cả đối với chủ sở hữu repo. [PR #1](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1) và [PR #2](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2) đều bị chặn merge khi check `test` báo đỏ và chỉ hợp nhất được sau khi đẩy commit sửa lỗi trong `src/main/`. Không có commit nào được push thẳng lên `main` sau khi bật bảo vệ. |
| **5** | **The contract of one LeeTKOB endpoint** | 25 | **25** | [`docs/contract/publish-lab.md`](docs/contract/publish-lab.md) tuân thủ cấu trúc 5 phần của `IA#3`: quy ước chung, 13 loại lỗi chuẩn **RFC 9457 (`application/problem+json`)**, cấu trúc biên nhận & giao dịch nguyên tử 3 bảng (`publications`, `published_marks`, `publication_audit`), thứ tự kiểm tra 7 bước & cơ chế Idempotency vĩnh viễn theo bài lab (`Idempotent-Replayed: true`), cùng chính sách phiên bản `/v1` và trích dẫn rõ `FAQ-01`, `FAQ-02`, `FAQ-09`, `FAQ-12`. |
| **6** | **Contract tests that bite** | 15 | **15** | [`docs/contract/coverage.md`](docs/contract/coverage.md) ánh xạ toàn bộ các điều khoản và 13 mã lỗi trong hợp đồng sang 22 bài Contract Tests trong [`PublishEndpointContractTest.java`](backend/src/test/java/vn/edu/hcmus/leetkob/publish/PublishEndpointContractTest.java) (cùng 6 bài Unit Tests = 28 bài test chạy bắt buộc trong `npm test`). Mọi trường hợp từ chối đều kiểm tra `COUNT(*) == 0` trên cả 3 bảng đầu ra. Hai thí nghiệm cố tình phá vỡ hợp đồng trên [PR #1](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1) và [PR #2](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2) đã chứng minh bộ test lập tức báo đỏ khi mã nguồn vi phạm hợp đồng. |
| **7** | **`AI-LOG.md`** | 5 | **5** | [`AI-LOG.md`](AI-LOG.md) ghi nhận đầy đủ công cụ AI đã sử dụng cho từng hạng mục, vai trò của các thành viên và 8 mục nhật ký (`A01`–`A08`) với các quyết định **Observed / Changed / Rejected** thực chất (bao gồm lỗi khẳng định DB, lỗi trailing tokens của Jackson, cấu hình CI và 2 thí nghiệm phá vỡ hợp đồng). |
| | **Tổng cộng (Total)** | **100** | **100** | Đầy đủ 7 tiêu chí với bằng chứng kiểm tra được trực tiếp trong mã nguồn và lịch sử Git. |

---

## 3. Những gì Nhóm chưa thực hiện ở `PA#2` (What We Did Not Manage / Scope Boundaries)

- **Các tính năng thuộc cột mốc sau (`PA#3`–`PA#5`):** Theo đúng phạm vi của `PA#2`, nhóm chưa cài đặt hệ thống quản lý tài khoản đa người dùng hoàn chỉnh, hàng đợi chấm code C++ trong container Sandbox cô lập, bộ kiểm tra ràng buộc bằng Hard Rules + LLM, cổng đánh giá `eval.mjs` trên bộ dev set, giao diện người dùng cho TA/Sinh viên, luồng phúc khảo (Appeals) và bài kiểm thử chịu tải 80 bài nộp đồng thời.
- **Không kết nối khóa API LLM trả phí ở `PA#2`:** Vì tính năng gọi mô hình LLM thuộc về `PA#3`, ứng dụng ở `PA#2` không nạp khóa API bên ngoài và duy trì chi phí mô hình bằng **USD 0**.

