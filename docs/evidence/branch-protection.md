# Cấu hình Bảo vệ Nhánh `main` (`docs/evidence/branch-protection.md`)

Quy tắc bảo vệ nhánh `main` trên repository công khai [`HuynhDucThinh/LeeTKOB-AWAD`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD).

## Quy trình Khởi tạo Nhánh và Bật Bảo vệ Nhánh `main` (`AI-LOG A12`)

Theo quy trình làm việc của nhóm, quá trình thiết lập dự án được chia thành 2 giai đoạn rõ ràng: (1) giai đoạn khởi tạo cấu trúc nền tảng trên nhánh chính trước khi khóa nhánh, và (2) giai đoạn vận hành cổng chất lượng bắt buộc qua Pull Request:

| Thời điểm (UTC, 10/11/2026) | Giai đoạn quy trình & Sự kiện | Bằng chứng |
| :--- | :--- | :--- |
| 05:46:52 | `163c426` bootstrap — Khởi tạo dự án và đẩy cấu hình CI ban đầu lên `main` | CI run [42764268531](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42764268531) (`event: push`) |
| 05:57:59 | `ee426da` refactor phân tầng — Hoàn tất phân tách cấu trúc package chuẩn Spring Boot trước khi khóa nhánh | CI run [42765386828](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42765386828) (`event: push`) |
| 05:58 – 06:04 | Kích hoạt Branch Protection (5 required checks, `enforce_admins: true`) sau khi ổn định cấu trúc nền | Kể từ mốc này, mọi thao tác push trực tiếp lên `main` đều bị chặn hoàn toàn |
| 06:04:10 | PR #1 mở — Kiểm chứng cổng `test` đỏ chặn merge thành công | CI run [42766034224](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766034224) |
| từ 06:06:50 | Toàn bộ thay đổi trên `main` đều đi qua Pull Request (`committer: web-flow`) | `git log --first-parent main` |

Đúng theo yêu cầu của Rubric (*"Nothing has been pushed straight to main since protection was turned on"*), kể từ thời điểm bật bảo vệ nhánh (trước PR #1), **không có bất kỳ commit nào được push thẳng lên `main`**. Hai commit khởi tạo nền tảng trước đó (`163c426`, `ee426da`) thuộc quy trình thiết lập ban đầu của nhóm trước khi khóa nhánh.

## Các quy tắc bắt buộc được áp dụng trên `refs/heads/main`

1. **Require a pull request before merging:** Bắt buộc mọi thay đổi phải đi qua Pull Request (không ai được phép `git push origin main` trực tiếp).
2. **Require status checks to pass before merging** (`strict: true` — nhánh phải cập nhật mới nhất với `main`) với đủ **5 cổng kiểm tra** từ GitHub Actions:
   - `lint` (`npm run lint`)
   - `format-check` (`npm run format:check`)
   - `typecheck` (`npm run typecheck`)
   - `test` (`npm test`)
   - `secrets` (`npm run sec:scan` + Gitleaks `8.30.1` trên toàn bộ lịch sử)
3. **Do not allow bypassing the above settings (`enforce_admins: true`):** Kể cả chủ sở hữu repository (`HuynhDucThinh`) cũng bị chặn.
4. `allow_force_pushes: false`, `allow_deletions: false`.

Kiểm chứng lại bất cứ lúc nào (repo public, không cần token):

```sh
curl -s https://api.github.com/repos/HuynhDucThinh/LeeTKOB-AWAD/branches/main | jq .protection
```

## Bằng chứng merge bị chặn

| PR | Lý do đỏ | Run đỏ | Thông báo khi cố merge | Commit sửa → run xanh |
| :--- | :--- | :--- | :--- | :--- |
| [#1](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1) | break cố ý: replay trả `201` | [42766034224](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766034224) | `Required status check "test" is failing` | `53e8b6d` → [42766200407](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766200407) |
| [#2](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2) | break cố ý: bỏ join `ta_classes` | [42766403260](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766403260) | `Required status check "test" is failing` | `58d8bcb` → [42766615499](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766615499) |
| [#4](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/4) | **lỗi thật**: auth fail-open (`AI-LOG A09`) | [42769824380](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42769824380) | `gh pr merge 4` → `the base branch policy prohibits the merge` (`mergeStateStatus: BLOCKED`) | `f70f2fe` → [42769997105](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42769997105) |

## File bằng chứng đi kèm

- Ảnh chụp trang cấu hình rule của `main` (Settings → Branches → rule `main`, URL `.../settings/branch_protection_rules/86092085`): ![Branch protection rule của main](branch-protection.png)
  Ảnh cho thấy: *Require status checks to pass before merging*, *Require branches to be up to date before merging*, 5 check bắt buộc (`lint`, `format-check`, `typecheck`, `test`, `secrets`, nguồn GitHub Actions) và **Do not allow bypassing the above settings**. Các mục không nằm trong khung ảnh (*Require a pull request before merging*, force push và deletions bị tắt) được xác nhận bằng file JSON bên dưới.
- Cấu hình JSON xuất từ GitHub REST API: [`main-protection.json`](main-protection.json)
- Nhật ký thực nghiệm cổng kiểm tra chặn merge: [`gate-experiments.json`](gate-experiments.json)
