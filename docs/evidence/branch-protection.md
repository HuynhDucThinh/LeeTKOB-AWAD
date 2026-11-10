# Cấu hình Bảo vệ Nhánh `main` (`docs/evidence/branch-protection.md`)

Quy tắc bảo vệ nhánh `main` trên repository công khai [`HuynhDucThinh/LeeTKOB-AWAD`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD) được kích hoạt ngay sau commit khởi tạo (`bootstrap`).

## Các quy tắc bắt buộc được áp dụng trên `refs/heads/main`

1. **Require a pull request before merging:** Bắt buộc mọi thay đổi phải đi qua Pull Request (không ai được phép `git push origin main` trực tiếp).
2. **Require status checks to pass before merging:** Bắt buộc nhánh phải cập nhật mới nhất với `main` và vượt qua đầy đủ **5 cổng kiểm tra tự động** từ GitHub Actions:
   - `lint` (`npm run lint`)
   - `format-check` (`npm run format:check`)
   - `typecheck` (`npm run typecheck`)
   - `test` (`npm test`)
   - `secrets` (Gitleaks `8.30.1` + `npm run sec:scan`)
3. **Do not allow bypassing the above settings (Include administrators):** Kể cả chủ sở hữu repository (`HuynhDucThinh`) cũng bị chặn hợp nhất nếu bất kỳ cổng kiểm tra nào trong 5 cổng trên báo đỏ.

## File bằng chứng đi kèm

- Cấu hình JSON xuất từ GitHub REST API: [`main-protection.json`](main-protection.json)
- Nhật ký thực nghiệm cổng kiểm tra chặn merge: [`gate-experiments.json`](gate-experiments.json)

