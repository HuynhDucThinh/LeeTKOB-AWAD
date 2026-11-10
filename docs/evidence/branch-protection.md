# Cấu hình Bảo vệ Nhánh `main` (`docs/evidence/branch-protection.md`)

Quy tắc bảo vệ nhánh `main` trên repository công khai [`HuynhDucThinh/LeeTKOB-AWAD`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD).

## Dòng thời gian (đính chính — `AI-LOG A12`)

Phiên bản trước của tài liệu này ghi protection được bật "ngay sau commit khởi tạo (`bootstrap`)". Điều đó **không chính xác**. Lịch sử `main` cho thấy:

| Thời điểm (UTC, 10/11/2026) | Sự kiện | Bằng chứng |
| :--- | :--- | :--- |
| 05:46:52 | `163c426` bootstrap — push trực tiếp lên `main` | CI run [42764268531](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42764268531) (`event: push`) |
| 05:57:59 | `ee426da` refactor phân tầng — **push trực tiếp lên `main`, không qua PR** | CI run [42765386828](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42765386828) (`event: push`); `GET /commits/ee426da/pulls` = `[]` |
| 05:58 – 06:04 | Bật branch protection (5 required checks, `enforce_admins: true`) | Với `enforce_admins: true` + bắt buộc PR, push `ee426da` sẽ bị từ chối — nên protection được bật **sau** `ee426da` |
| 06:04:10 | PR #1 mở — check `test` đỏ, merge bị chặn | CI run [42766034224](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766034224) |
| từ 06:06:50 | Mọi thay đổi trên `main` đều là merge commit của PR (`committer: web-flow`): PR #1 → PR #7 | `git log --first-parent main` |

Kể từ khi bật protection, **không có commit nào được push thẳng lên `main`**. Hai commit trước đó (`163c426`, `ee426da`) được giữ nguyên trong lịch sử; nhóm không force-push để xoá dấu vết.

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

- Cấu hình JSON xuất từ GitHub REST API: [`main-protection.json`](main-protection.json)
- Nhật ký thực nghiệm cổng kiểm tra chặn merge: [`gate-experiments.json`](gate-experiments.json)
