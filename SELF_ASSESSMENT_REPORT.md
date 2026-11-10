# Báo cáo Tự đánh giá Cột mốc `PA#2` — Dự án LeeTKOB (`SELF_ASSESSMENT_REPORT.md`)

- **Môn học:** CSC13114 — Lập trình Web Nâng cao (Năm học 2026–2027)
- **Cột mốc:** `PA#2` — Harness and Quality Gate (4% tổng điểm môn học · Thang điểm 100)
- **Mã nhóm:** `23120197-23120199-23120228`
- **Repository công khai:** [`https://github.com/HuynhDucThinh/LeeTKOB-AWAD`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD)
- **Tag nộp bài:** [`pa2`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/tree/pa2)
- **Tổng điểm tự đánh giá:** **98 / 100**

---

## 1. Thành viên Nhóm và Phân bổ Trách nhiệm

| Họ và tên | MSSV | Tỷ lệ đóng góp | Phạm vi phụ trách chính |
| :--- | :---: | :---: | :--- |
| **Trà Văn Sỹ** | `23120197` | 1/3 (33.3%) | Thiết lập `AGENTS.md`, `CLAUDE.md`, phân quyền 3 tầng (`docs/permissions.md`, `.claude/settings.json`), `.env.example` và cấu hình quét bí mật lịch sử Git (`Gitleaks` / `sec:scan`). |
| **Huỳnh Đức Thịnh** | `23120199` | 1/3 (33.3%) | Khởi tạo kiến trúc phân tầng Spring Boot 3 (Java 21), chuẩn hóa 4 lệnh kiểm tra đa nền tảng, thiết lập GitHub Actions CI 5 jobs, cấu hình Branch Protection trên nhánh `main`, các PR sửa lỗi sau rà soát (#4–#7) và tổng hợp hồ sơ `PA2.md` / `SELF_ASSESSMENT_REPORT.md`. |
| **Lê Trung Thành Đạt** | `23120228` | 1/3 (33.3%) | Soạn thảo hợp đồng 5 phần (`docs/contract/publish-lab.md`), bản đồ độ phủ (`docs/contract/coverage.md`), cài đặt `PublishController` / `PublishService` / `LabPublishRepository` và bộ kiểm thử JUnit 5 (Contract Tests + Unit Tests). |

---

## 2. Bảng Tự chấm điểm Chi tiết theo 7 Tiêu chí Rubric

| # | Tiêu chí trong Rubric `PA#2` | Điểm tối đa | Điểm tự chấm | Bằng chứng cụ thể và Lý do tự đánh giá |
| :-: | :--- | :-: | :-: | :--- |
| **1** | **Rules file** | 15 | **14** | [`AGENTS.md`](AGENTS.md) (nạp bởi [`CLAUDE.md`](CLAUDE.md)) gồm 4 mục `Stack` / `Commands` / `Conventions` / `Never`. List `Never` gắn với LeeTKOB: không secret trong sandbox, không gọi LLM thật trong test, không sửa test / `eval/` / `labels.jsonl` để ép check xanh. Các dòng truy vết được tới **sự cố thật có commit**: auth fail-closed → `A09` ([PR #4](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/4), test đỏ trước rồi mới sửa), cấm `catch {}` rỗng → `A10` ([PR #5](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/5)), cấm hook test trong `src/main` và khóa trong JVM → `A11` ([PR #6](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/6)), cấm push thẳng `main` → `A12`. **Tự trừ 1 điểm:** các dòng dẫn A06/A07 được soạn trước khi thí nghiệm chạy (xem ghi chú minh bạch trong [`AI-LOG.md`](AI-LOG.md)). |
| **2** | **Permissions and secrets** | 10 | **10** | 3 tầng `Allowed` / `Ask first` / `Never` trong [`docs/permissions.md`](docs/permissions.md), được thực thi bằng [`.claude/settings.json`](.claude/settings.json). [`.env.example`](.env.example) liệt kê đủ 4 biến, không có giá trị; **mỗi biến đều được code đọc thật** (`application.properties`, `AppConfig`) sau [PR #4](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/4). Auth fail-closed. Gitleaks 8.30.1 quét toàn bộ lịch sử trong job `secrets` bắt buộc: 0 phát hiện. [`README.md`](README.md) ghi rõ: chưa dùng model (`none`), spending cap **USD 0**. |
| **3** | **One command for every check** | 10 | **10** | [`package.json`](package.json), [`README.md`](README.md) và [`.github/workflows/ci.yml`](.github/workflows/ci.yml) dùng **đúng cùng 4 lệnh** `npm run lint` / `format:check` / `typecheck` / `npm test`. Fresh clone chỉ cần JDK 21 + Node 22: xanh, log in `Tests run: 32`. `sec:scan` giờ fail-closed khi git lỗi ([PR #5](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/5)). |
| **4** | **CI that blocks a merge** | 20 | **19** | `main` protected: 5 required checks, `strict`, `enforce_admins: true` ([ảnh chụp](docs/evidence/branch-protection.png), [`branch-protection.md`](docs/evidence/branch-protection.md), [`main-protection.json`](docs/evidence/main-protection.json)). PR bị chặn: [#1](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1) (run đỏ [42766034224](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766034224) → `53e8b6d` xanh), [#2](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2) (run đỏ [42766403260](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766403260) → `58d8bcb` xanh), và [#4](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/4) — **lỗi thật** (run đỏ [42769824380](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42769824380), `gh pr merge` bị từ chối → `f70f2fe` xanh [42769997105](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42769997105)). Từ PR #1 trở đi `main` chỉ có merge commit. **Tự trừ 1 điểm:** `ee426da` được push thẳng `main` trước khi bật protection, và tài liệu cũ ghi sai thời điểm (đã đính chính ở `A12`). |
| **5** | **The contract of one LeeTKOB endpoint** | 25 | **25** | [`docs/contract/publish-lab.md`](docs/contract/publish-lab.md): role `ta` **và** quy tắc sở hữu `ta_classes` (TA lớp khác nhận `404` giống hệt lab không tồn tại); field rules của request/receipt; 13 problem type RFC 9457 kèm status/title; ví dụ `409`, `400 validation` (mảng `errors`), `503`, và replay; thứ tự kiểm tra 7 bước; idempotency + đồng thời; lý do không cần paging; chính sách `/v1` → `/v2`. Code khớp contract §2 (503 khi khóa dòng DB bận) sau [PR #6](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/6). |
| **6** | **Contract tests that bite** | 15 | **15** | [`coverage.md`](docs/contract/coverage.md) ánh xạ từng điều khoản và 13 mã lỗi sang 23 contract test qua HTTP thật + DB thật trong [`PublishEndpointContractTest.java`](backend/src/test/java/vn/edu/hcmus/leetkob/controller/PublishEndpointContractTest.java), cùng 9 unit test (tổng 32, chạy trong job `test` bắt buộc). Mỗi lần từ chối đều đọc `COUNT(*)` của 3 bảng. Không còn hook test trong `src/main`. Đã chứng minh test cắn: 2 break cố ý (PR #1, #2) và 1 lỗi thật bắt được bằng test viết trước (PR #4). Không commit nào sửa test để làm đỏ thành xanh (PR #6 chỉ đổi setup, không xoá/nới assertion). |
| **7** | **`AI-LOG.md`** | 5 | **5** | [`AI-LOG.md`](AI-LOG.md): công cụ theo từng phần (gồm Antigravity cho PR #4–#7), mục `A01`–`A12` với Observed / Changed / Rejected. `A10` chính là ví dụ "bước CI do assistant viết không bao giờ fail được" kèm cách tái hiện. Có ghi chú minh bạch rằng A06/A07 được soạn trước và đã đối chiếu với log CI thật. |
| | **Tổng cộng (Total)** | **100** | **98** | |

---

## 3. Những gì Nhóm chưa thực hiện ở `PA#2` (What We Did Not Manage)

- **Lịch sử không hoàn hảo, giữ nguyên chứ không viết lại:** `163c426` và `ee426da` được push thẳng `main` trước khi bật branch protection. A06/A07 trong `AI-LOG.md` được soạn trước khi thí nghiệm chạy. Nhóm không force-push để che dấu vết; cả hai được đính chính trong `AI-LOG A12` và ghi chú minh bạch ở A06/A07.
- **Chưa có `CODEOWNERS`** cho `backend/src/test/`, `.github/` và `docs/contract/` (slide 7 khuyến nghị).
- **Các tính năng thuộc cột mốc sau (`PA#3`–`PA#5`):** chưa có tài khoản đa người dùng hoàn chỉnh, hàng đợi chấm C++ trong sandbox, hard rules + LLM, cổng `eval.mjs`, giao diện TA/sinh viên, phúc khảo và kiểm thử tải 80 bài nộp đồng thời.
- **Không kết nối LLM ở `PA#2`:** không nạp khóa API, chi phí mô hình **USD 0**.
