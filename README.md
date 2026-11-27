# LeeTKOB — Cột mốc `PA#3`: C++ Sandbox, Hard Rules, Semantic LLM Checker & Human Review Gate

**Môn học:** CSC13114 — Lập trình Web Nâng cao (Năm học 2026–2027)  
**Nhóm thực hiện (`23120197-23120199-23120228`):**
- **Trà Văn Sỹ** — MSSV: `23120197` (Admin/Classes/Labs `T1`, Review Queue `H1–H3`, Pass Sampler 10%, Publish & Appeals `A1–A3`)
- **Huỳnh Đức Thịnh** — MSSV: `23120199` (C++ Stripper + Line Map, Syntax-Aware Hard Rules `D1`, Groq LLM + Call Logger, Guardrails `G1–G5`, Checker CLI `MODEL_OFF=1` + `checker/Dockerfile` + Eval)
- **Lê Trung Thành Đạt** — MSSV: `23120228` (Durable Queue `Q1`, `JudgeWorker`, Submissions API + SSE, C++ Docker Sandbox `M1–M9`)

**LeeTKOB** là nền tảng chấm bài C++ và hỗ trợ trợ giảng (TA) kiểm duyệt ràng buộc cú pháp & thuật toán trước khi công bố điểm chính thức:
1. Sinh viên nộp bài C++ → đẩy vào hàng đợi bền vững `judge_jobs` (`FOR UPDATE SKIP LOCKED`) → tiến trình `JudgeWorker` biên dịch và chạy test trong **Docker C++ Sandbox cô lập 9 lớp (`M1–M9`)**.
2. Bài đạt `AC` đi qua **bộ xóa comment + ẩn danh PII bảo toàn số dòng 1-based**, xét **Syntax-Aware Hard Rules** trước (`0` lời gọi LLM cho các lệnh cấm `"Do not use"` / `"Do not call"`), sau đó gọi **Groq `openai/gpt-oss-120b`** kèm **5 hàng rào bảo vệ (`G1–G5`)** cho các ràng buộc ngữ nghĩa (`semantic`).
3. **Tuân thủ tuyệt đối Syllabus Rule:** Không có bất kỳ kết luận nào của máy tự động trừ điểm sinh viên. Mọi kết luận (`violation`, `unsure`, cùng `10%` mẫu `pass` kiểm định) chỉ đi vào **TA Review Queue**; trước khi công bố điểm, sinh viên chỉ thấy trạng thái chấm chức năng (`AC`/`WA`/`TLE`/`MLE`/`RE`/`CE`, `testCasesPassed`/`testCasesTotal`) và hoàn toàn không thấy `constraint_verdicts`, `deduction` hay `evidence`, đồng thời bị chặn gửi phúc khảo (`409 appeal-before-publish`) cho tới khi TA phụ trách lớp (`FAQ-01`) duyệt hết hàng đợi (`FAQ-09`) qua thời hạn nộp bài (`FAQ-12`) và bấm **Publish Marks** (theo [FAQ chính thức của nhóm Product Owners — `FAQ-01`, `FAQ-02`, `FAQ-09`, `FAQ-12`](https://github.com/tkob-team/LeeTKOB-FAQ/blob/main/FAQ.md)).

**Tài liệu trọng tâm trong Repository:**
- Tóm tắt bàn giao `PA#3` (tài khoản mẫu, lệnh CLI/Docker, đường dẫn báo cáo eval): [`PA3.md`](PA3.md)
- Hướng dẫn khởi chạy Database, Backend, Sandbox & Frontend: [`docs/README_RUN.md`](docs/README_RUN.md)
- Quy tắc làm việc cho AI Agent & Phân quyền 3 tầng: [`AGENTS.md`](AGENTS.md), [`CLAUDE.md`](CLAUDE.md), [`docs/permissions.md`](docs/permissions.md)
- Hợp đồng API công bố điểm (`POST /v1/labs/{labId}/publish`): [`docs/contract/publish-lab.md`](docs/contract/publish-lab.md)
- Nhật ký cộng tác với AI (`A01`–`A19`): [`AI-LOG.md`](AI-LOG.md)

---

## 1. Năm Lệnh Kiểm tra Chất lượng Chuẩn hóa (Local & GitHub Actions CI)

Chạy từ thư mục gốc (yêu cầu **JDK 21 LTS** và **Node.js ≥ 22**; sử dụng Gradle Wrapper đóng gói sẵn trong `backend/`):

```sh
npm run lint          # Checkstyle (main + test, maxWarnings = 0)
npm run format:check  # Spotless Google Java Format (chỉ đọc)
npm run typecheck     # Biên dịch tĩnh toàn bộ main + test
npm test              # Toàn bộ 100 Unit Tests & HTTP Contract Tests (H2 PostgreSQL mode, 0 gọi mạng)
npm run sec:scan      # Quét lộ lọt bí mật trên toàn bộ lịch sử Git (kết hợp Gitleaks trên CI)
```

*(Hoặc chạy gộp cả 5 cổng bằng 1 lệnh duy nhất: `npm run check`).*

---

## 2. Checker CLI (`MODEL_OFF=1`), `checker/Dockerfile` & Chấm Điểm Tự động (`eval.mjs`)

Bộ kiểm tra ràng buộc dòng lệnh (`com.leetkob.checker.cli.CheckerCliMain`) dùng chung **100% cùng một `DefaultConstraintCheckPipeline`** với `JudgeWorker`, đọc trực tiếp `problems/<id>/statement.md`, `problems/<id>/constraints.json` và `solutions/<id>/*.cpp` mà không cần bất kỳ cấu hình riêng theo tên bài nào.

### 2.1. Build Docker Image cho Checker CLI

```sh
docker build -f checker/Dockerfile -t leetkob-checker:pa3 .
```

### 2.2. Chạy Offline với Công tắc `MODEL_OFF=1` (Không mạng, Chỉ chạy Hard Rules)

Khi `MODEL_OFF=1`, CLI **không khởi tạo `LlmClient` và không mở bất kỳ kết nối mạng nào**, chấm tất định toàn bộ ràng buộc ban-list và ghi `unsure` cho mọi ràng buộc `semantic`:

```sh
# Linux / macOS
docker run --rm --network none --memory=512m --pids-limit=64 -e MODEL_OFF=1 \
  -v "$(pwd)/eval/dev:/dataset:ro" -v "$(pwd)/tmp-out:/out" \
  leetkob-checker:pa3 /dataset /out/predictions.jsonl

node eval/dev/eval.mjs --data eval/dev --predictions tmp-out/predictions.jsonl
```

```powershell
# Windows PowerShell
docker run --rm --network none --memory=512m --pids-limit=64 -e MODEL_OFF=1 `
  -v "${PWD}\eval\dev:/dataset:ro" -v "${PWD}\tmp-out:/out" `
  leetkob-checker:pa3 /dataset /out/predictions.jsonl

node eval/dev/eval.mjs --data eval/dev --predictions tmp-out/predictions.jsonl
```

- **Kết quả kiểm chứng `MODEL_OFF=1` trên `eval/dev/` (102 cặp):** **`0` wrongful deductions** (`0/65`, bao gồm `0/14` trên toàn bộ nhóm bẫy `trap`), quyết định chính xác **`55/55` (`100%`)** cặp ban-list, `47/47` cặp semantic trả `unsure`, `eval.mjs` thoát mã `1` (không bao giờ trả `2`). *(Lưu ý trên Linux: image mặc định chạy quyền non-root `USER 10001:10001`; nếu thư mục `tmp-out` trên host chỉ cho phép UID hiện tại ghi, thêm `--user "$(id -u):$(id -g)"` vào lệnh `docker run`).*

### 2.3. Chạy CLI trực tiếp bằng JAR (Offline hoặc gọi Groq thật)

```powershell
# Build fat JAR tại backend/build/libs/checker-cli.jar
node scripts/gradlew.mjs --no-daemon checkerCliJar

# Chạy với model thật (API key chỉ truyền qua biến môi trường, không lưu vào repo)
$env:MODEL_OFF="0"
$env:MODEL_PROVIDER="groq"
$env:MODEL_NAME="openai/gpt-oss-120b"
$env:MODEL_API_KEY="<your-groq-api-key>"
$env:CHECKER_BUDGET_USD="0.50"

java -jar backend/build/libs/checker-cli.jar `
  --data eval/dev `
  --predictions eval/runs/pa3-dev-run/predictions.jsonl `
  --call-log eval/runs/pa3-dev-run/call-log.jsonl

node eval/dev/eval.mjs --data eval/dev --predictions eval/runs/pa3-dev-run/predictions.jsonl
```

---

## 3. Kết quả Đánh giá Thực tế trên `eval/dev/` & Bài toán Chi phí cho 1 Lab 80 Bài nộp

### 3.1. Bộ dữ liệu & Kết quả Run thật đã Commit (`eval/runs/pa3-dev-run/`)

- **Bộ đề gốc nguyên bản:** [`eval/dev/`](eval/dev/) (`eval.mjs` SHA-256 `c836edd8713521bb...`, `labels.jsonl` SHA-256 `262cda7b0ef5612e...`).
- **Bộ đề mở rộng của nhóm (6 bài C++17 `Accepted`, 12 cặp, đủ 5 tag `trap`, `injection`, `disguised`, `obvious`, `style`):** [`eval/custom-cases/`](eval/custom-cases/).
- **Bằng chứng lần chạy Groq thật (`openai/gpt-oss-120b`):**
  - Dự đoán: [`eval/runs/pa3-dev-run/predictions.jsonl`](eval/runs/pa3-dev-run/predictions.jsonl)
  - Báo cáo `eval.mjs` nguyên văn: [`eval/runs/pa3-dev-run/eval-report.txt`](eval/runs/pa3-dev-run/eval-report.txt)
  - Nhật ký kiểm toán từng lời gọi (có `provider_request_id` thật `req_...`, prompt đã xóa comment/PII, `0` lời gọi cho hard-rule): [`eval/runs/pa3-dev-run/call-log.jsonl`](eval/runs/pa3-dev-run/call-log.jsonl)
  - Tóm tắt số liệu JSON: [`eval/runs/pa3-dev-run/run-meta.json`](eval/runs/pa3-dev-run/run-meta.json)

| Chỉ số trên `eval/dev/` (47 bài, 102 cặp) | Chế độ `MODEL_OFF=1` | Chế độ Groq `openai/gpt-oss-120b` thật | Ngưỡng chuẩn |
| :--- | :---: | :---: | :---: |
| **Wrongful deductions** | **`0`** | **`0`** | `= 0` |
| **Recall** | `40.5% (15/37)` *(100% của 15 vi phạm hard-rule)* | **`94.6% (35/37)`** | `>= 90.0%` |
| **Unsure rate** | `46.1% (47/102)` *(47 cặp semantic)* | **`3.9% (4/102)`** | `<= 20.0%` |
| **Kết luận `eval.mjs`** | Exit `1` | **`PASS` (Exit `0`)** | Exit `0` |

### 3.2. Mô hình sử dụng & Phép tính Chi phí cho 1 Lab gồm 80 Bài nộp

- **Nhà cung cấp & Mô hình:** **Groq** — **`openai/gpt-oss-120b`** (`temperature = 0`, `response_format = json_object`).
- **Đơn giá niêm yết của Groq cho `openai/gpt-oss-120b`:**
  - Input (prompt): **\$0.15 / 1,000,000 tokens**
  - Output (completion): **\$0.60 / 1,000,000 tokens**
- **Số liệu thực đo từ [`call-log.jsonl`](eval/runs/pa3-dev-run/call-log.jsonl) trên `eval/dev/`:**
  - Tổng `55` cặp ràng buộc ban-list (`hard_rule`): **`0` lời gọi LLM → \$0.000000**.
  - Tổng `43` lời gọi semantic hoàn tất (cùng `4` lần gặp lỗi mạng/timeout tạm thời được guardrail hạ an toàn về `unsure` tốn \$0):
    - Tổng prompt tokens: `43,567` tokens (trung bình **`1,013` prompt tokens / lời gọi**)
    - Tổng completion tokens: `11,945` tokens (trung bình **`278` completion tokens / lời gọi**)
    - Tổng chi phí toàn bộ lần chạy `eval/dev/`: **`\$0.013703`**
    - Chi phí trung bình cho **1 lời gọi ràng buộc semantic**:
      $$\frac{\$0.013703}{43} \approx \$0.000319 \text{ / lời gọi semantic}$$
- **Phép tính chi phí cho 1 Bài Lab tiêu chuẩn gồm 80 bài nộp `Accepted`:**
  1. **Lab tiêu chuẩn có 1 ràng buộc Hard-rule + 1 ràng buộc Semantic** (như `sort-merge`, `binary-search`, `reverse-words`, `gcd-euclid`):
     - Ràng buộc Hard-rule: $80 \times \$0 = \$0.000000$
     - Ràng buộc Semantic: $80 \times 1 \times \$0.000319 = \mathbf{\$0.025520}$ (**~2,6 cent USD / bài lab**)
  2. **Lab có 1 ràng buộc Hard-rule + 2 ràng buộc Semantic** (như `dynamic-array`):
     - $80 \times 2 \times \$0.000319 = \mathbf{\$0.051040}$ (**~5,1 cent USD / bài lab**)
  - Trần ngân sách cứng trên mỗi bài lab (`labs.cost_cap_usd`, guardrail `G4`) được đặt mặc định ở mức **\$1.50 – \$5.00 / lab** (và hạ xuống **\$0.50 / lab** trên môi trường demo công khai để bảo vệ hạn mức tài khoản Groq).

---

## 4. Kịch bản Kiểm thử End-to-End Tự động (`scripts/`)

```sh
node scripts/test-pr3a-e2e.mjs   # E2E Stripper + Hard Rules + Syllabus Rule + Publish + Appeal
node scripts/test-pr3b-e2e.mjs   # E2E Real Groq + Prompt Injection Defense + Guardrails G1-G5
node scripts/test-pr3c-e2e.mjs   # E2E SHA-256 + Offline Docker MODEL_OFF=1 + Re-score pa3-dev-run
```
