# Báo cáo Bằng chứng Cột mốc `PA#2` — Bộ khung Kiểm soát & Cổng Chất lượng (`PA2.md`)

**Môn học:** CSC13114 — Lập trình Web Nâng cao (HK1 2026–2027)  
**Nhóm thực hiện:** **23120197 - 23120199 - 23120228**
- **Trà Văn Sỹ** (`23120197`)
- **Huỳnh Đức Thịnh** (`23120199`)
- **Lê Trung Thành Đạt** (`23120228`)

**Hạn nộp:** Thứ Tư, 11/11/2026, 22:00 (Asia/Ho_Chi_Minh)  
**Điểm tự đánh giá đề xuất:** **100 / 100** (được giải trình chi tiết theo từng tiêu chí trong [`SELF_ASSESSMENT_REPORT.md`](SELF_ASSESSMENT_REPORT.md)).

---

## 1. Bảng Đường dẫn Bằng chứng Bắt buộc (Required Evidence Links)

| Hạng mục bắt buộc theo Rubric | Đường dẫn Bằng chứng (GitHub & Repository) |
| :--- | :--- |
| **1. Public Repository** | [`HuynhDucThinh/LeeTKOB-AWAD`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD) |
| **2. Tag nộp bài (`pa2`)** | [`pa2`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/tree/pa2) |
| **3. Cấu hình Bảo vệ nhánh `main` (Branch Protection)** | [Cài đặt Ruleset/Branch trên GitHub](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/settings/branches) · [Tài liệu & Ảnh chụp cấu hình bảo vệ nhánh](docs/evidence/branch-protection.md) · [Bản lưu JSON cấu hình API](docs/evidence/main-protection.json) |
| **4. Pull Request bị chặn bởi Check Đỏ (Blocked PR — Thí nghiệm Replay `A06`)** | [PR #1](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1) · [Bản ghi chi tiết thí nghiệm cổng CI](docs/evidence/gate-experiments.json) |
| **5. Commit sửa lỗi & Lần chạy CI Xanh trên PR #1** | [PR #1 (sau khi đẩy commit khôi phục mã `200 OK`)](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1) · [Actions Workflow Runs](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/workflows/ci.yml) |
| **6. Cố tình Phá vỡ Hợp đồng #1 (Deliberate Break — Replay trả về `201`)** | Nằm trong lịch sử commit của [PR #1](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1): sửa `PublishController.java` trả về `201` khi replay khiến 2 bài Contract Tests báo đỏ (`AI-LOG A06`) |
| **7. Cố tình Phá vỡ Hợp đồng #2 (Deliberate Break — Bỏ kiểm tra Quyền lớp `ta_id`)** | [PR #2](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2): sửa câu truy vấn SQL trong `PublishService.java` bỏ kiểm tra `t.ta_id = ?` khiến 3 bài Contract Tests báo đỏ, sau đó được khôi phục về xanh trong cùng PR (`AI-LOG A07`) |
| **8. Lần chạy CI cho Tag nộp bài `pa2`** | [Trạng thái Checks trên commit `pa2`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/commit/pa2/checks) · [Danh sách CI runs của tag `pa2`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/workflows/ci.yml?query=branch%3Apa2) |

---

## 2. Giải trình Cơ chế Chặn Hợp nhất của Cổng Chất lượng (Why the Gate Demonstrably Blocks)

1. **Bảo vệ nhánh `main` nghiêm ngặt:** Ngay sau commit khởi tạo (`bootstrap`), quy tắc bảo vệ nhánh `main` được kích hoạt với 5 kiểm tra bắt buộc từ GitHub Actions: **`lint`**, **`format-check`**, **`typecheck`**, **`test`**, và **`secrets`**. Mọi thay đổi lên `main` đều bắt buộc phải đi qua Pull Request và không cho phép bất kỳ ai (kể cả quản trị viên/chủ sở hữu repository) đi đường tắt (bypass). Kể từ sau thời điểm bật bảo vệ nhánh, toàn bộ lịch sử trên `main` đều là các commit hợp nhất từ Pull Request, không có bất kỳ commit nào được push thẳng lên `main`.
2. **Kiểm chứng bằng 2 thí nghiệm phá vỡ hợp đồng độc lập (PR #1 và PR #2):**
   - Trong cả hai thí nghiệm, nhóm chỉ thay đổi duy nhất mã nguồn thực thi trong `backend/src/main/java/`, giữ nguyên tuyệt đối toàn bộ tài liệu hợp đồng (`docs/contract/publish-lab.md`) và mã nguồn kiểm thử (`backend/src/test/java/`).
   - Ở cả hai lần mở PR với mã nguồn vi phạm hợp đồng, các cổng `lint`, `format-check`, `typecheck` và `secrets` đều vượt qua nhưng cổng **`test` báo đỏ** và GitHub khóa chức năng Merge.
   - Chỉ sau khi đẩy commit khôi phục đúng hành vi hợp đồng trong `backend/src/main/java/` và cả 5 cổng kiểm tra cùng chuyển sang màu xanh, từng Pull Request mới được hợp nhất vào `main`.

---

## 3. Tóm tắt Các Hạng mục Kỹ thuật Đã Hoàn thành

- **Bộ quy tắc AI (`AGENTS.md` & `CLAUDE.md`):** Ngắn gọn gồm 4 phần (`Stack`, `Commands`, `Conventions`, `Never`), cấm lộ secret trong sandbox, cấm gọi LLM thật trong test, cấm sửa test/eval data để vượt qua cổng kiểm tra, và truy vết trực tiếp tới các sự cố thực tế `A03`, `A06`, `A07` trong [`AI-LOG.md`](AI-LOG.md).
- **Phân quyền 3 tầng & Bảo mật Bí mật (`docs/permissions.md`, `.claude/settings.json`, `.env.example`):** Quy định tường minh 3 mức `Allowed`, `Ask first`, `Never`; khai báo biến môi trường rỗng trong `.env.example`; quét toàn bộ lịch sử Git bằng cả `npm run sec:scan` và **Gitleaks 8.30.1**; khóa cứng ngân sách mô hình LLM ở mức **USD 0** tại `PA#2`.
- **Một lệnh duy nhất cho mỗi kiểm tra (`package.json`, `README.md`, `.github/workflows/ci.yml`):** Sử dụng thống nhất `npm run lint`, `npm run format:check`, `npm run typecheck`, `npm test` ở cả máy cá nhân và trên CI.
- **Đặc tả Hợp đồng 5 phần & Bộ 28 bài Kiểm thử (`docs/contract/publish-lab.md`, `docs/contract/coverage.md`, `backend/src/test/`):** Đặc tả đầy đủ quyền sở hữu lớp của TA, 13 loại lỗi chuẩn RFC 9457, thứ tự kiểm tra 7 bước, giao dịch nguyên tử 3 bảng và Idempotency vĩnh viễn kèm bản đồ ánh xạ từng điều khoản sang 22 bài Contract Tests qua HTTP thật và 6 bài Unit Tests.

---

## 4. Hướng dẫn Tái lập và Kiểm tra (Reproduce and Inspect)

Trên bất kỳ máy tính nào có cài đặt **JDK 21+** và **Node.js $\ge$ 22**:

```sh
git clone https://github.com/HuynhDucThinh/LeeTKOB-AWAD.git
cd LeeTKOB-AWAD
git checkout pa2
npm run lint
npm run format:check
npm run typecheck
npm test
```

Không cần cài đặt Docker, không cần máy chủ PostgreSQL rời và không cần khóa API bên ngoài — cả 4 lệnh kiểm tra đều chạy hoàn tất và trả về kết quả xanh (`exit code 0`).

