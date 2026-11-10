# Báo cáo Bằng chứng Cột mốc `PA#2` — Bộ khung Kiểm soát & Cổng Chất lượng (`PA2.md`)

**Môn học:** CSC13114 — Lập trình Web Nâng cao (HK1 2026–2027)  
**Nhóm thực hiện:** **23120197 - 23120199 - 23120228**
- **Trà Văn Sỹ** (`23120197`)
- **Huỳnh Đức Thịnh** (`23120199`)
- **Lê Trung Thành Đạt** (`23120228`)

**Hạn nộp:** Thứ Tư, 11/11/2026, 22:00 (Asia/Ho_Chi_Minh)  
**Điểm tự đánh giá đề xuất:** **98 / 100** (được giải trình chi tiết theo từng tiêu chí trong [`SELF_ASSESSMENT_REPORT.md`](SELF_ASSESSMENT_REPORT.md)).

---

## 1. Bảng Đường dẫn Bằng chứng Bắt buộc (Required Evidence Links)

| Hạng mục bắt buộc theo Rubric | Đường dẫn Bằng chứng (GitHub & Repository) |
| :--- | :--- |
| **1. Public Repository** | [`HuynhDucThinh/LeeTKOB-AWAD`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD) |
| **2. Tag nộp bài (`pa2`)** | [`pa2`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/tree/pa2) |
| **3. Cấu hình Bảo vệ nhánh `main` (Branch Protection)** | [Cài đặt Branch Protection trên GitHub](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/settings/branches) · [Tài liệu cấu hình bảo vệ nhánh](docs/evidence/branch-protection.md) · [Bản lưu JSON cấu hình API](docs/evidence/main-protection.json) |
| **4. Pull Request bị chặn bởi Check Đỏ (Blocked PR — Thí nghiệm Replay `A06`)** | [PR #1](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1) · Commit đỏ [`ffd17cf`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1/commits/ffd17cf29d81a48386057c59d36914e79c8c513f) · [CI Run Đỏ #42766034224](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766034224) (cổng `test` báo đỏ chặn merge với HTTP 405) |
| **5. Commit sửa lỗi & Lần chạy CI Xanh trên PR #1** | Commit sửa xanh [`53e8b6d`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1/commits/53e8b6dc2ca1e593547dfe95ac5a84c137109791) trên [PR #1](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1) · [CI Run Xanh #42766200407](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766200407) |
| **6. Cố tình Phá vỡ Hợp đồng #1 (Deliberate Break — Replay trả về `201`)** | Commit [`ffd17cf`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1/commits/ffd17cf29d81a48386057c59d36914e79c8c513f) trong [PR #1](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1): sửa `PublishController.java` trả về `201` khi replay khiến 2 bài Contract Tests báo đỏ tại [Run #42766034224](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766034224) (`AI-LOG A06`) |
| **7. Cố tình Phá vỡ Hợp đồng #2 (Deliberate Break — Bỏ kiểm tra Quyền lớp `ta_id`)** | [PR #2](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2) · Commit phá vỡ hợp đồng [`6605bfe`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2/commits/6605bfe5924ac6b79c123b3c1812e19248675a9d) · [CI Run Đỏ #42766403260](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766403260) → Commit sửa xanh [`58d8bcb`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2/commits/58d8bcb3c590318c1ea4645651d7a35a8a8621c6) · [CI Run Xanh #42766615499](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766615499) (`AI-LOG A07`) |
| **8. PR bị chặn vì lỗi thật (không cố ý) — Auth fail-open `A09`** | [PR #4](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/4) · Commit chỉ thêm test [`13e1428`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/4/commits/13e1428) · [CI Run Đỏ #42769824380](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42769824380) (3 test đỏ; `gh pr merge 4` → *the base branch policy prohibits the merge*) → Commit sửa `src/main` [`f70f2fe`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/4/commits/f70f2fe) · [CI Run Xanh #42769997105](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42769997105) |
| **9. Các PR sửa lỗi sau rà soát** | [PR #5](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/5) secret scan fail-closed (`A10`) · [PR #6](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/6) khóa dòng DB + gỡ test hook (`A11`) · [PR #7](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/7) tài liệu & đính chính (`A12`) |
| **10. Lần chạy CI cho Tag nộp bài `pa2`** | [CI runs của tag `pa2`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/workflows/ci.yml?query=branch%3Apa2) · [Checks trên commit `pa2`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/commit/pa2/checks). *(File này nằm trong chính commit được gắn tag nên không thể chứa sẵn ID của run sinh ra sau khi gắn tag; hai link trên luôn trỏ đúng run `event: push`, `head_branch: pa2`.)* |

---

## 2. Giải trình Cơ chế Chặn Hợp nhất của Cổng Chất lượng (Why the Gate Demonstrably Blocks)

1. **Bảo vệ nhánh `main` nghiêm ngặt:** Trước khi mở các Pull Request thí nghiệm, quy tắc bảo vệ nhánh `main` được kích hoạt với 5 kiểm tra bắt buộc từ GitHub Actions: **`lint`**, **`format-check`**, **`typecheck`**, **`test`**, và **`secrets`**. Mọi thay đổi lên `main` đều bắt buộc phải đi qua Pull Request và không cho phép bất kỳ ai (kể cả quản trị viên/chủ sở hữu repository `enforce_admins: true`) đi đường tắt (bypass). Kể từ sau thời điểm bật bảo vệ nhánh, toàn bộ lịch sử trên `main` đều là các commit hợp nhất từ Pull Request (PR #1 → PR #7, `committer: web-flow`), không có commit nào được push thẳng lên `main`. **Đính chính (`AI-LOG A12`):** hai commit đầu `163c426` và `ee426da` được push thẳng **trước** khi bật protection; dòng thời gian chi tiết ở [`branch-protection.md`](docs/evidence/branch-protection.md).
2. **Kiểm chứng bằng 2 thí nghiệm phá vỡ hợp đồng có chủ đích và 1 lỗi thật ([PR #4](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/4), xem mục 8 ở bảng trên) — 2 thí nghiệm ([PR #1](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1) và [PR #2](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2)):**
   - Trong cả hai thí nghiệm, nhóm chỉ thay đổi duy nhất mã nguồn thực thi trong `backend/src/main/java/` (`PublishController.java` ở PR #1 và `LabPublishRepository.java` ở PR #2), giữ nguyên tuyệt đối toàn bộ tài liệu hợp đồng (`docs/contract/publish-lab.md`) và mã nguồn kiểm thử (`backend/src/test/java/`).
   - Ở cả hai lần mở PR với mã nguồn vi phạm hợp đồng ([Run #42766034224](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766034224) và [Run #42766403260](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766403260)), các cổng `lint`, `format-check`, `typecheck` và `secrets` đều vượt qua nhưng cổng **`test` báo đỏ** và GitHub từ chối yêu cầu Merge với lỗi `HTTP 405: Required status check "test" is failing.`
   - Chỉ sau khi đẩy commit khôi phục đúng hành vi hợp đồng trong `backend/src/main/java/` (`53e8b6d` trên PR #1 và `58d8bcb` trên PR #2) và cả 5 cổng kiểm tra cùng chuyển sang màu xanh ([Run #42766200407](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766200407) và [Run #42766615499](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766615499)), từng Pull Request mới được hợp nhất vào `main`.

---

## 3. Tóm tắt Các Hạng mục Kỹ thuật Đã Hoàn thành

- **Bộ quy tắc AI (`AGENTS.md` & `CLAUDE.md`):** Ngắn gọn gồm 4 phần (`Stack`, `Commands`, `Conventions`, `Never`), cấm lộ secret trong sandbox, cấm gọi LLM thật trong test, cấm sửa test/eval data để vượt qua cổng kiểm tra, và truy vết trực tiếp tới các sự cố thực tế `A03`, `A06`, `A07` trong [`AI-LOG.md`](AI-LOG.md).
- **Phân quyền 3 tầng & Bảo mật Bí mật (`docs/permissions.md`, `.claude/settings.json`, `.env.example`):** Quy định tường minh 3 mức `Allowed`, `Ask first`, `Never`; khai báo biến môi trường rỗng trong `.env.example`; quét toàn bộ lịch sử Git bằng cả `npm run sec:scan` và **Gitleaks 8.30.1**; khóa cứng ngân sách mô hình LLM ở mức **USD 0** tại `PA#2`.
- **Một lệnh duy nhất cho mỗi kiểm tra (`package.json`, `README.md`, `.github/workflows/ci.yml`):** Sử dụng thống nhất `npm run lint`, `npm run format:check`, `npm run typecheck`, `npm test` ở cả máy cá nhân và trên CI.
- **Đặc tả Hợp đồng 5 phần & Bộ 32 bài Kiểm thử (`docs/contract/publish-lab.md`, `docs/contract/coverage.md`, `backend/src/test/`):** Đặc tả đầy đủ quyền sở hữu lớp của TA, 13 loại lỗi chuẩn RFC 9457, thứ tự kiểm tra 7 bước, giao dịch nguyên tử 3 bảng và Idempotency vĩnh viễn kèm bản đồ ánh xạ từng điều khoản sang 23 bài Contract Tests qua HTTP thật và 9 bài Unit Tests.

---

## 4. Hướng dẫn Tái lập và Kiểm tra (Reproduce and Inspect)

Trên bất kỳ máy tính nào có cài đặt **JDK 21+** và **Node.js ≥ 22**:

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
