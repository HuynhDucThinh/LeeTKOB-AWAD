# Bàn giao Cột mốc `PA#3` — Dự án LeeTKOB (`PA3.md`)

- **Môn học:** CSC13114 — Lập trình Web Nâng cao
- **Mã nhóm:** `23120197-23120199-23120228` (`Trà Văn Sỹ` · `Huỳnh Đức Thịnh` · `Lê Trung Thành Đạt`)
- **Repository công khai:** <https://github.com/HuynhDucThinh/LeeTKOB-AWAD>

---

## 1. Địa chỉ Triển khai & Tài khoản Kiểm thử (5 tài khoản chuẩn Rubric)

- **Deployment URL:** *(điền URL triển khai công khai trước khi gắn tag `pa3`)*
- **Screen Recording (≤ 5 phút):** *(điền link video quay luồng Criterion 1 trên deployment trước khi gắn tag `pa3`)*
- **CI Run cho tag `pa3` (bao gồm `sandbox-tests`):** *(điền link GitHub Actions run của tag `pa3`)*

Tất cả 5 tài khoản dưới đây được khởi tạo tự động bởi `DemoAccountSeeder` và chỉ chứa dữ liệu mẫu (hạn mức chi tiêu LLM mỗi lab được khóa cứng bởi guardrail `G4`):

| Vai trò | Email đăng nhập | Mật khẩu | Lớp được phân công |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin@leetkob.edu` | `Admin@123` | Quản trị toàn hệ thống |
| **TA 1** *(lớp 10)* | `ta1@leetkob.edu` | `Ta1@1234` | `CSC13114-A` (`id = 10`) |
| **TA 2** *(lớp 20 — kiểm tra cô lập `T1`)* | `ta2@leetkob.edu` | `Ta2@1234` | `CSC13114-B` (`id = 20`) |
| **Student 1** *(lớp của TA 1)* | `student1@leetkob.edu` | `Student1@123` | `CSC13114-A` (`id = 10`) |
| **Student 2** *(lớp của TA 1)* | `student2@leetkob.edu` | `Student2@123` | `CSC13114-A` (`id = 10`) |

---

## 2. Lệnh Build Docker, Chạy Checker CLI & Công tắc `MODEL_OFF=1`

### Build Checker Docker Image

```sh
docker build -f checker/Dockerfile -t leetkob-checker:pa3 .
```

### Chạy Checker với `MODEL_OFF=1` (Không mạng, chấm bằng `eval.mjs`)

```sh
docker run --rm --network none --memory=512m --pids-limit=64 -e MODEL_OFF=1 \
  -v "$(pwd)/eval/dev:/dataset:ro" -v "$(pwd)/tmp-out:/out" \
  leetkob-checker:pa3 /dataset /out/predictions.jsonl

node eval/dev/eval.mjs --data eval/dev --predictions tmp-out/predictions.jsonl
```

*(Trên Windows PowerShell, thay `$(pwd)` bằng `${PWD}`).*

### Chạy Checker trực tiếp bằng JAR

```sh
node scripts/gradlew.mjs --no-daemon checkerCliJar
MODEL_OFF=1 java -jar backend/build/libs/checker-cli.jar eval/dev tmp-out/predictions.jsonl
```

---

## 3. Thư mục Kết quả Chạy Mô hình Thật trên Dev Set (`eval/runs/pa3-dev-run/`)

- **Mô hình:** Groq `openai/gpt-oss-120b` (`$0.15 / 1M` prompt tokens, `$0.60 / 1M` completion tokens).
- **Đường dẫn thư mục kết quả đã commit:** [`eval/runs/pa3-dev-run/`](eval/runs/pa3-dev-run/)
  - File dự đoán (chấm lại được trực tiếp bằng `node eval/dev/eval.mjs --data eval/dev --predictions eval/runs/pa3-dev-run/predictions.jsonl`): [`eval/runs/pa3-dev-run/predictions.jsonl`](eval/runs/pa3-dev-run/predictions.jsonl)
  - Báo cáo nguyên văn của `eval.mjs`: [`eval/runs/pa3-dev-run/eval-report.txt`](eval/runs/pa3-dev-run/eval-report.txt)
  - Nhật ký kiểm toán lời gọi kèm `provider_request_id` thật của Groq: [`eval/runs/pa3-dev-run/call-log.jsonl`](eval/runs/pa3-dev-run/call-log.jsonl)
  - Tóm tắt JSON & chi phí: [`eval/runs/pa3-dev-run/summary.json`](eval/runs/pa3-dev-run/summary.json), [`eval/runs/pa3-dev-run/run-meta.json`](eval/runs/pa3-dev-run/run-meta.json)
- **Kết quả đạt được trên `eval/dev/` (47 bài, 102 cặp):**
  - `wrongful deductions`: **`0`** (`ok`, ngưỡng `= 0`)
  - `recall`: **`94.6% (35/37)`** (`ok`, ngưỡng `>= 90.0%`)
  - `unsure rate`: **`3.9% (4/102)`** (`ok`, ngưỡng `<= 20.0%`)
  - Trạng thái: **`PASS`** (`exit code = 0`)
  - Tổng chi phí 47 bài: **`$0.013703`** (`0` lời gọi cho 55 cặp hard-rule; trung bình **`$0.000319`** / lời gọi semantic → **~$0.0255** cho 1 lab 80 bài có 1 semantic constraint, hoặc **~$0.0510** cho 1 lab 80 bài có 2 semantic constraints).
- **Bộ test mở rộng của nhóm (6 bài C++17 `Accepted`, 12 cặp, đủ tag `trap` & `injection`):** [`eval/custom-cases/`](eval/custom-cases/).
