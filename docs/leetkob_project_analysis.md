# Phân Tích Toàn Diện Đồ Án **LeeTKOB** (CSC13114 · CQ2023/3)

Tài liệu này tổng hợp toàn bộ nội dung đề bài, quy định chấm điểm, luồng công việc từ `PA#1` → `PA#6`, và đánh giá hiện trạng repository `LeeTKOB-AWAD` (đang ở mốc `PA#2`) để bạn nắm trọn bức tranh trước khi bắt tay vào code `PA#3`.

---

## 1. Tổng Quan Đồ Án: LeeTKOB Là Gì?

### 1.1. Bối cảnh & Bài toán
- **Môn học:** CSC13114 — Phát triển Ứng dụng Web Nâng cao (HK1 2026–2027), lớp `2609-AWAD-23/3`.
- **Tổng trọng số đồ án:** **40% điểm môn học**, chia làm **6 cột mốc (`PA#1` → `PA#6`)**. *(Lưu ý: `SEMINAR` chiếm 7% nhưng là bài tập nhóm tách biệt, không nằm trong 40% đồ án này).*
- **Tại sao cả lớp cùng làm LeeTKOB?** Từ `PA#2` trở đi, toàn bộ các nhóm trong lớp đều xây dựng chung một sản phẩm tên **LeeTKOB** (vốn là đề xuất thắng giải ở `PA#1` của nhóm Product Owners: Lê Tấn Hiệp, Tống Dương Thái Hoà, Nguyễn Phúc Hoàng). Lý do:
  1. **Eval so sánh được:** Mọi nhóm chấm cùng một tập *held-out* của giảng viên.
  2. **Red-team có mục tiêu rõ:** Ở `PA#4`, nhóm bạn sẽ tấn công deployment của nhóm khác trên cùng một đặc tả sản phẩm.
  3. **Thi đấu về Kỹ thuật (Engineering), không thi về Ý tưởng:** Điểm số nằm ở Harness, Gates, Eval, Guardrails, Operations và khả năng giải trình khi vấn đáp.

### 1.2. Người dùng & Vấn đề cốt lõi
- **Người dùng chính:** Trợ giảng (**TA**) phụ trách lab nhập môn lập trình C++.
- **Vấn đề:** Đêm trước khi công bố điểm bài Lab Sorting, có ~80 bài nộp đạt **Accepted** (chạy đúng test case). Tuy nhiên, bài Accepted vẫn có thể vi phạm quy tắc bài tập (ví dụ: đề bắt tự cài *merge sort* nhưng sinh viên gọi `std::sort` hoặc cài *bubble sort*). TA hiện phải đọc tay từng bài (2–3 phút/bài, tốn 3–4 giờ/tuần).
- **Tính năng LLM cốt lõi (KHÔNG phải chatbot):** Đây là một bước tự động hóa trong pipeline chấm bài (Constraint Review) trước khi công bố điểm:
  1. **Hard rules chạy trước (Deterministic, Syntax-aware):** Các ràng buộc dạng danh sách cấm (`"Do not use..."` / `"Do not call..."`) phải do code phân tích cú pháp quyết định. **Không gọi LLM** cho những ràng buộc mà Hard Rule đã xử lý được.
  2. **LLM chỉ chấm Semantic Constraints:** Ví dụ *"có thật sự dùng merge sort không"*, *"có dùng đệ quy không"*, *"có giấu mảng phụ không"*. Model chỉ nhận: đề bài (`statement`), danh sách ràng buộc (`constraints`), và code đã **xóa comment + xóa danh tính sinh viên**.
  3. **Model trả về `pass` / `violation` / `unsure` kèm dòng code bằng chứng (`evidence`).** Model **tuyệt đối không** quyết định mức trừ điểm (`deduction` do TA đặt sẵn).
  4. **Guardrails + Human Gate:** Mọi `violation`, mọi `unsure`, và **10% ngẫu nhiên các `pass`** rơi vào **Review Queue** của TA. Điểm và mức trừ chỉ hiện với sinh viên sau khi TA duyệt và bấm **Publish**.

### 1.3. Chi phí của việc sai sót (Cost of Being Wrong) & Ngưỡng Eval
- Trừ oan 3/10 điểm (trong lab chiếm 30% môn) có thể đẩy sinh viên xuống rớt môn hoặc mất học bổng; nếu sai hệ thống, 80 sinh viên bị trừ oan và kéo theo 80 đơn phúc khảo. Sửa trước khi Publish tốn 0 đồng; sửa sau khi Publish làm mất uy tín và phải mở lại bảng điểm.
- **3 ngưỡng bắt buộc cho Checker (bắt đầu tính điểm ở `PA#4`):**

| Chỉ số (Metric) | Ngưỡng bắt buộc |
| :--- | :---: |
| **Wrongful deductions** (trừ oan trên bài hợp lệ) | **0** |
| **Violations caught (Recall)** (bắt đúng `violation` kèm `evidence` đúng) | **≥ 90%** |
| **Tỷ lệ `unsure`** trên tổng số verdict | **≤ 20%** |

---

## 2. Phạm Vi Sản Phẩm: 11 Tính Năng Bắt Buộc & Những Thứ Cố Tình KHÔNG Làm

### 2.1. Bảng 11 Tính năng (F1 → F11) & Các Quy tắc (Rules)

| Mã | Tính năng | Ai dùng | Yêu cầu hoạt động | Rules kiểm tra |
| :-: | :--- | :--- | :--- | :---: |
| **F1** | **Accounts, roles & classes** | Admin, TA, Student | 3 role; TA thuộc lớp, lab thuộc lớp; mỗi role chỉ thấy màn hình của mình. TA lớp khác không thấy lab/queue. | `T1` |
| **F2** | **Lab với tests & constraints** | TA | Tạo lab trong lớp mình: đề bài (`statement`), tests, và constraints (kèm mức trừ điểm TA chọn), có cả hard rule và semantic rule. | `T1` |
| **F3** | **Submit & Judge real-time** | Student | Nộp bài C++ → vào **queue** → **worker riêng** chấm trong sandbox. Trạng thái trên trang sinh viên tự đổi từ `queued` sang kết quả cuối **không cần F5/reload**. | `Q1` |
| **F4** | **The Sandbox** | Pipeline | Biên dịch & chạy code đều nằm trong sandbox, đạt đủ **9 mức tối thiểu**, mỗi mức có 1 bài attack trong repo chạy ở CI. | `M1–M9` |
| **F5** | **Hard rules first** | Pipeline | Ban-list do code syntax-aware quyết định. Không dính bẫy: hàm `sort` tự viết của sinh viên, tên hàm cấm trong comment/string, `#include <algorithm>` chỉ để dùng `std::swap`. Không gọi model cho constraint này. | `D1` |
| **F6** | **Model chấm semantic** | Pipeline | Gọi model thật (chỉ gửi statement, constraints, code đã strip comments & identity) → trả về `pass`/`violation`/`unsure` + evidence lines. Không tự đặt deduction. | `G5` |
| **F7** | **Guardrails cho output model** | Pipeline, TA | Sai schema, evidence không khớp code gốc, timeout, vượt cost cap của lab → biến thành `unsure` và TA thấy lý do. Bài prompt-injection kết thúc là `violation` hoặc `unsure`, không bao giờ `pass`. | `G1–G4` |
| **F8** | **Review Queue & Publish** | TA | Queue chứa mọi `violation`, mọi `unsure` và **10% ngẫu nhiên `pass`** (server chọn). TA duyệt xong mới bấm **Publish** — hành động duy nhất đưa deduction ra cho sinh viên. | `H1–H3` |
| **F9** | **Published marks & Appeals** | Student, TA | Sau Publish, sinh viên xem điểm & lý do trừ. Sinh viên gửi phúc khảo (appeal); TA từ chối (có lý do) hoặc chấp nhận (điểm đổi). | `A1–A3` |
| **F10** | **Checker CLI & Eval Gate** | Team | 1 lệnh CLI xuất file JSONL từ thư mục dataset, hỗ trợ `MODEL_OFF=1`. CI chạy checker trên dev set + cases tự thêm rồi chạy `eval.mjs`, là **required check trên `main`**. | — |
| **F11** | **Logs, Metrics & Traces** | Team | Với 1 bài nộp thực tế: tìm mọi log theo 1 ID; xem trace xuyên từ HTTP → queue → sandbox → hard rules → model → DB; dashboard; SLO & alert. | — |

### 2.2. Những thứ CỐ TÌNH KHÔNG LÀM (Out of Scope)
1. Ngôn ngữ lập trình thứ hai ngoài C++.
2. Dùng LLM sinh test case hoặc tự chọn mức trừ điểm.
3. Phát hiện đạo văn (plagiarism) hoặc phát hiện code do AI viết.
4. Bảng xếp hạng contest, Online IDE, Vector DB / RAG.

### 2.3. Chú ý về Product Owners' FAQ
Ở những chỗ đề bài bỏ ngỏ (ví dụ: *sinh viên có thấy evidence trước khi Publish không?*, *TA thấy gì khi verdict là `unsure`?*, *appeal bị từ chối có mở lại được không?*), hành vi sản phẩm tuân theo **FAQ chính thức của Product Owners** (trên nhóm Facebook lớp / link Classroom). Khi dựa vào FAQ nào, phải **trích dẫn mã FAQ** trong repo/báo cáo. Nếu FAQ mâu thuẫn với Brief của giảng viên, **Brief thắng**.

---

## 3. Quy Định Chung & Cơ Chế Chấm Điểm Cực Kỳ Quan Trọng

### 3.1. Quy cách nộp bài (Áp dụng cho PA#3, PA#4, PA#5)
- **Repository:** Bắt buộc **public** (để giảng viên và đội red-team đọc). Gắn tag `pa3`, `pa4`, `pa5` lên commit nộp và push tag.
- **File Zip:** Nén toàn bộ repo **bao gồm cả thư mục `.git`**, đặt tên theo MSSV tăng dần + điểm tự chấm:
  `<MSSV1>-<MSSV2>-<MSSV3>_<total>.zip` (ví dụ nhóm bạn: `23120197-23120199-23120228_90.zip`).
- **Tài khoản nộp:** Nộp từ email `<StudentID>@student.hcmus.edu.vn`.
- **Các file bắt buộc ở root:**
  - `SELF_ASSESSMENT_REPORT.md`: từng dòng theo tiêu chí rubric, điểm tự nhận, **bằng chứng cụ thể** (file, commit, PR, CI run), và mục ngắn *What we did not manage*. Tổng điểm phải khớp tên file zip.
  - `PA3.md` / `PA4.md` / `PA5.md`: chứa các link/thông tin rubric yêu cầu.
  - `AI-LOG.md`: dùng chung 1 file suốt học kỳ, ghi nhận thực chất `Observed / Changed / Rejected` cho từng phần của mỗi milestone.
- **Lịch sử Commit:** Các milestone chấm tiêu chí *"one commit per part, tests no later than code"*. **Nghiêm cấm** viết xong hết rồi mới `git rebase`/chia lại commit — giảng viên kiểm tra timestamp và đối chiếu với `AI-LOG.md`.
- **Phạt nộp trễ:** ≤ 2 giờ: `-20%`; 2–24 giờ: `-50%`; > 24 giờ: **0 điểm** (áp dụng sau Honesty Adjustment).

### 3.2. Honesty Adjustment (Điều chỉnh trung thực)
So sánh `Gap = Điểm tự chấm - Điểm thực tế`:
- Trong khoảng `±10`: Không trừ.
- `+11` đến `+20`: **-3 điểm**.
- `+21` đến `+30`: **-6 điểm**.
- Hơn `+30`: **-10 điểm**.
- Tự chấm thấp hơn thực tế từ `-21` trở xuống: **-3 điểm**.
- Thiếu `SELF_ASSESSMENT_REPORT.md`: **-10 điểm**.
- *Lưu ý:* Tiêu chí nào tự nhận điểm mà không ghi dòng bằng chứng thì bị tính là "claimed and not done".

### 3.3. "Syllabus Rule" & Các bẫy AI hay mắc (Catching Your Assistant)
- **Syllabus Rule (PA#3):** Nếu verdict của model chạy thẳng tới điểm sinh viên mà không qua TA, **hoặc** dùng output model mà không có schema check và timeout → **Tiêu chí 4, 5, 6 của PA#3 bị 0 điểm**.
- **Các lỗi AI hay làm khiến mất điểm nặng:**
  1. Sửa file `eval.mjs`, `labels.jsonl` hoặc file `.cpp` của dev set (giảng viên check SHA-256).
  2. Hardcode tên bài dev-set (vd: `sort-merge/s05`) vào prompt hoặc hard rules.
  3. Sửa/nới lỏng test hoặc đổi nhãn case tự thêm trong cùng commit làm CI xanh.
  4. Test memory limit 1 GiB nhưng không đọc lại vùng nhớ đã cấp phát (compiler tối ưu xóa mất allocation).
  5. Gắn `-v /var/run/docker.sock`, `--privileged`, `--env-file` hoặc `-e MODEL_API_KEY` vào lệnh chạy sandbox.
  6. Call log "real run" nhưng không có `request_id` từ provider (tức là mock).
  7. Eval gate đọc file `predictions.jsonl` commit sẵn trong repo thay vì chạy checker tạo mới ngay trong CI.
  8. Guardrail test vẫn pass ngay cả khi xóa code guardrail đi.
  9. Ẩn nút Publish trên UI nhưng API endpoint không chặn khi gọi trực tiếp bằng `curl`.

---

## 4. Full Flow Chi Tiết Từ PA#1 → PA#6

| Milestone | Trọng số | Hạn nộp | Mục tiêu cốt lõi |
| :--- | :---: | :--- | :--- |
| **PA#1** | 4% | 07/10/2026 *(Đã qua)* | Proposal & Planning |
| **PA#2** | 4% | 11/11/2026 *(Đã qua)* | Harness & Quality Gate |
| **PA#3** | **7%** | **25/11/2026 (22:00)** *(Sắp tới)* | Prototype với LLM feature chạy thật trên Public Deployment |
| **PA#4** | **7%** | **02/12/2026 (22:00)** *(Cách PA#3 8 ngày)* | Eval Gate CI, Held-out Run, 5 Guardrails, Human Gate & Red-team |
| **PA#5** | **3%** | **16/12/2026 (22:00)** | Final Build, Public HTTPS, Appeals, Observability, Load Test 80 bài & Post-mortem |
| **PA#6** | **15%** | **24/12 & 31/12/2026** *(OWNERS.md: 21/12)* | Final Demo (40đ) & Vấn đáp cá nhân (60đ) |

---

### Giai đoạn 1: `PA#1` — Proposal and Planning (4% · Đã xong)
- Đề xuất sản phẩm ban đầu của nhóm (2 trang + repo link + `SELF_ASSESSMENT_REPORT.md`). Điểm giữ nguyên; từ `PA#2` trở đi toàn lớp chuyển sang làm **LeeTKOB**.

---

### Giai đoạn 2: `PA#2` — Harness and Quality Gate (4% · Đã xong)
- Thiết lập nền móng kỹ thuật trước khi đưa LLM vào:
  - File quy tắc cho AI (`AGENTS.md` / `CLAUDE.md`) gồm Stack, Commands, Conventions, Never.
  - Phân quyền 3 tầng & bảo mật (`.env.example`, không secret trong git history, spending cap = 0 ở PA#2).
  - Một lệnh thống nhất local & CI (`lint`, `format:check`, `typecheck`, `test`).
  - Branch protection trên `main` chặn merge khi CI đỏ (có minh chứng PR bị chặn thật).
  - Đặc tả hợp đồng RFC 9457 cho 1 endpoint (`POST /v1/labs/{labId}/publish`) và bộ Contract Tests "biết cắn".

---

### Giai đoạn 3: `PA#3` — Prototype with the LLM Feature Running (7% · Hạn: Thứ Tư 25/11/2026, 22:00)
Đây là cột mốc **đưa toàn bộ pipeline vào chạy thật lần đầu tiên** trên **Public Deployment** (và phải giữ deployment này sống liên tục từ 25/11 đến khi có điểm `PA#4` để team khác Red-team).

**8 Tiêu chí chấm điểm (100 điểm):**
1. **The flow, end to end, on your deployment (15đ):**
   - Giảng viên dùng tài khoản trong `PA3.md`: đăng nhập TA tạo lab `sort-merge` (đề, tests, 2 constraints + deductions) → 1 Student nộp 3 bài (compliant, violation, injection) → trạng thái chuyển `queued` → `judged` → mở Review Queue thấy verdict, evidence lines, deduction. TA lớp khác không thấy lab/queue này.
2. **Queue and sandbox (20đ):**
   - Nộp bài vào **Queue**, một **Worker tách biệt** lấy ra chấm (chứng minh: tắt worker → nộp bài → bật worker → bài được chấm **đúng 1 lần**).
   - Biên dịch + chạy code C++ đều trong Sandbox, đạt đủ **9 minimums**:
     - `M1`: No network.
     - `M2`: CPU time limit.
     - `M3`: Wall-clock limit (kill cả container, không chỉ process).
     - `M4`: Memory limit (gồm swap; code test phải đọc lại memory đã cấp phát).
     - `M5`: Process limit (chống fork bomb).
     - `M6`: Output size limit.
     - `M7`: Non-root user.
     - `M8`: No host mounts, no Docker socket.
     - `M9`: No secrets (API key không có trong env, file, hay thứ gì `#include` được).
   - Mỗi minimum phải có **1 bài attack submission** trong repo chạy ở CI (trên tag `pa3`) và bị chặn đúng cách.
3. **Hard rules first (15đ):**
   - Constraint bắt đầu bằng `"Do not use"` hoặc `"Do not call"` (tên cấm nằm trong backticks `` `...` ``) phải do code **syntax-aware** quyết định (không phải regex/text search thô).
   - Vượt qua các bẫy (`trap`): hàm `sort` tự viết trong namespace, tên cấm trong comment/string, `#include <algorithm>` chỉ để dùng `std::swap`.
   - Không gọi model cho constraint này (call log chứng minh). Chạy với `MODEL_OFF=1` đạt **0 wrongful deductions** trên dev set.
4. **The model judges the semantic constraints (20đ):**
   - Gọi model thật cho semantic constraints. Chỉ gửi statement, constraints, code đã **xóa comment & identity** (call log chứng minh prompt sạch).
   - Trả về `pass`/`violation`/`unsure` + evidence lines (không đặt deduction).
   - Commit sẵn **1 lần chạy thật trên dev set** gồm: file predictions JSONL, báo cáo `eval.mjs`, và **call log** (có `request_id` của provider, model, tokens, thời gian, chi phí mỗi call).
   - `README.md` và `AI-LOG.md` ghi rõ tên model và tính toán chi phí cho 1 lab 80 bài.
5. **First limits, and the review queue (10đ):**
   - Ở `PA#3` chấm 2 guardrail đầu: **Schema validation** (sai schema → `unsure`) và **Timeout** (hết giờ → `unsure`), mỗi cái có test dùng **fake model, không mạng**.
   - Verdict chỉ rơi vào Review Queue, không tự động thành điểm sinh viên. Trích dẫn FAQ về việc sinh viên thấy gì trước khi Publish.
6. **The checker command and a first eval (10đ):**
   - 1 lệnh CLI nhận thư mục dataset → xuất file JSONL predictions. Có `Dockerfile` đóng gói checker này.
   - Hỗ trợ `MODEL_OFF=1` (chỉ chạy hard rules, trả `unsure` cho mọi semantic constraint, không mở kết nối mạng). Giảng viên sẽ chạy `docker run --network none` với `MODEL_OFF=1` trên bản dev set của họ.
   - Dev set trong repo phải **giống từng byte** bản đã công bố. Tạo thư mục riêng chứa **ít nhất 5 case tự viết** (có label & tag, ít nhất 1 `trap` và 1 `injection`).
   - *(Lưu ý: `PA#3` chưa ép ngưỡng pass eval, chỉ cần chạy ra kết quả trung thực).*
7. **Tests first, one part per commit (5đ):**
   - Test cho hard rules (gồm traps) và 2 limits nằm ở commit trước (hoặc cùng commit) với code. Mỗi commit làm 1 phần riêng.
8. **AI-LOG.md (5đ):**
   - Ghi cụ thể tool, phần việc, `Changed` / `Rejected` thật.

**File cần nộp kèm ở `PA#3`:** Tag `pa3`, `PA3.md` (URL deployment, test accounts gồm 1 admin, 2 TA khác lớp, 2 student lớp TA 1; link CI run của tag `pa3`; lệnh checker + Docker build + `MODEL_OFF=1`; thư mục dev-set run đã commit; link video quay màn hình ≤ 5 phút làm fallback), `SELF_ASSESSMENT_REPORT.md`, zip cả `.git`.

---

### Giai đoạn 4: `PA#4` — Eval, Guardrails, Human Gate & Red-Team (7% · Hạn: Thứ Tư 02/12/2026, 22:00)
*Chú ý: Chỉ cách `PA#3` đúng 8 ngày!*

**6 Tiêu chí chấm điểm (100 điểm):**
1. **The eval gates CI (20đ):**
   - Job CI chạy **trực tiếp checker với model thật** trên dev set + **≥ 10 case tự thêm** (gồm `trap`, `injection`, `disguised`), rồi chạy `eval.mjs`. Khác exit code 0 là fail. Job này phải là **Required check trên `main`**.
   - Minh chứng 1 PR bị Eval Gate chặn đỏ rồi sửa **checker** (không sửa data) để xanh.
   - `PA4.md` báo cáo **3 lần chạy lặp lại của cùng 1 commit** (số cặp đổi verdict, min/max recall, min/max unsure, chi phí 1 lần chạy CI), và cách xử lý secret cho PR từ fork.
2. **The held-out run (20đ):**
   - Giảng viên chạy Docker image checker của nhóm bạn **1 lần** trên tập **held-out** (dùng `marking.env` bạn đính kèm ngoài file zip).
   - Chấm theo 3 ngưỡng: **0 wrongful deductions**, **Recall ≥ 90%** (≥95%: 20đ, 92–95%: 18–19đ, 90–92%: 17đ), **Unsure ≤ 20%**.
3. **The guardrails of section 3 (20đ):**
   - Đủ **5 guardrails**, mỗi cái có test dùng fake model & không mạng, mỗi cái + test nằm ở 1 commit riêng:
     1. **Schema** (JSON lỗi, nằm trong code fence/prose, verdict lạ, thiếu/trùng constraint → `unsure`).
     2. **Evidence** (dòng bịa, đúng text sai số dòng, dòng nằm trong comment đã strip → `unsure`; nếu strip comment phải map lại số dòng về code gốc).
     3. **Timeout** (hết giờ → `unsure`).
     4. **Cost cap per lab** (đạt trần chi phí → các constraint còn lại thành `unsure`, TA thấy lý do).
     5. **No deduction from model** (output model có trường deduction hay chỉ dẫn trừ điểm đều bị phớt lờ; chỉ dùng mức trừ của TA).
   - Bài injection trên deployment không bao giờ ra `pass`.
4. **The human gate (15đ):**
   - Server tự rút ngẫu nhiên **10% `pass` (làm tròn lên)** + mọi `violation` + mọi `unsure` vào Review Queue (có test fixed seed).
   - API từ chối `Publish` khi còn bất kỳ item nào chưa duyệt, và từ chối Student / TA lớp khác (giảng viên test bằng direct request).
   - `PA4.md` có mục **"Where we stop it"** trả lời 3 câu hỏi (Ai trả giá khi sai? Làm sao biết sai? Chặn ở đâu?) bằng số liệu thật, và giải thích **tại sao đặt gate ở Publish chứ không đặt ở mọi verdict** (kèm chi phí của các pass không được sample).
5. **Red-team report on another team (20đ):**
   - Giảng viên chỉ định 1 team mục tiêu sau hạn `PA#3`. Tấn công theo **Rules of Engagement** (~1 req/s, 1 resource attack mỗi loại, trigger cost cap tối đa 1 lần, không tấn công hosting provider).
   - Viết báo cáo tại `docs/red-team/<target-student-ids>.md`: **≥ 8 attacks thuộc ≥ 5 categories** (sandbox, model key/data, prompt injection, guardrails, human gate/auth), tái lập được hoàn toàn, có ít nhất 1 attack tìm ra từ việc **đọc code** của team bạn.
6. **AI-LOG.md (5đ).**

**Nộp kèm:** `PA4.md`, `marking.env` (**đính kèm bên cạnh file zip trên Classroom, KHÔNG bỏ vào zip, KHÔNG commit vào git**).

---

### Giai đoạn 5: `PA#5` — Final Build, Running and Observed (3% · Hạn: Thứ Tư 16/12/2026, 22:00)
*Marking window: 17/12 – 23/12/2026.*

**6 Tiêu chí chấm điểm (100 điểm):**
1. **A public deployment of `pa5` (10đ):** Chạy qua **HTTPS**, có endpoint `/version` hoặc footer hiển thị commit hash của `pa5`. `PA5.md` ghi rõ cách deploy, nơi chứa config, cách rollback.
2. **Publishing, appeals and real-time results (20đ):**
   - Giảng viên tự walkthrough 6 bước: tạo lab → 2 student nộp bài (trạng thái đổi **không cần reload**) → student 2 thử appeal trước khi Publish (bị chặn) → TA duyệt queue & Publish → student xem điểm → student 2 appeal → TA reject (có lý do), rồi mở lại/accept nếu FAQ cho phép → student 1 thử appeal bài của student 2 (bị chặn).
3. **Logs, metrics and traces (20đ):**
   - Log có cấu trúc (`level`, `time`, `service`, `id`). Tuyệt đối **không log API key hay tên/MSSV trong prompt** (vi phạm bị khống chế ≤ 4đ).
   - Distributed Trace từ HTTP request → Queue → Sandbox worker → Hard rules → từng Model call (hiện model, tokens, cost) → DB.
   - Dashboard: request rate & errors, queue depth & oldest job age, judge time, model calls (count, latency, errors, timeouts), % unsure, model spend vs cap.
   - **1 SLO** (vd: 95% bài nộp có kết quả trong 60s) và **1 Alert đã từng nổ (fired) ít nhất 1 lần** khớp thời gian với logs.
4. **Load test: 80 submissions at once (20đ):**
   - Script trong `load/` + `Dockerfile` (chỉ nhận base URL & credentials). Dùng **80 tài khoản student khác nhau**, gửi 80 bài vào cùng 1 lab trong **≤ 10 giây** (vẫn bật đủ 9 sandbox minimums).
   - Giảng viên chạy lại script này từ máy họ trong 15 phút: yêu cầu **80/80 ra kết quả cuối, 0 trùng lặp, 0 lỗi 5xx**.
   - Viết `load/RESULT.md`: output script, p50/p95, queue drain time, chi phí model, ảnh chụp dashboard lúc chạy, **thứ gì bão hòa (saturate) đầu tiên** và **link commit đã sửa vì nó**.
5. **One incident post-mortem (20đ):**
   - Viết tại `docs/postmortem/<YYYY-MM-DD>-<slug>.md` về 1 sự cố thật (hoặc fault injection có chủ đích nhưng phải ghi rõ ở dòng đầu). Đủ 7 mục: Summary, Impact, Timeline (có timezone + evidence khớp log/git), Detection, Root cause (cơ chế thật, không đổ thừa "lỗi con người/AI"), Fix (commit + kiểm chứng), Follow-up (owner + status, có 1 action cải thiện detection).
6. **AI-LOG.md (10đ):** Có chi phí thực tế của 1 lab đọc từ metrics.

**Nộp kèm:** `PA5.md`, `load/RESULT.md`, video walkthrough ≤ 10 phút (quay sau khi tag `pa5`), và `ACCESS.md` (**bỏ vào zip nhưng KHÔNG commit vào git**, thêm vào `.gitignore` — chứa tài khoản test & quyền read-only vào observability).

---

### Giai đoạn 6: `PA#6` — Final Demo & Individual Oral (15% · 24/12 & 31/12/2026)
- **Trước slot:** Commit `OWNERS.md` (bảng ai làm phần nào + bảng **Beyond the brief** kèm test kiểm chứng) và nộp link commit trước **22:00 Thứ Hai 21/12/2026**. Giữ deployment `pa5` sống tới khi thi xong.
- **Cấu trúc thi (~23 phút/nhóm 3 người):**
  - **1 phút:** Check version `pa5` trên deployment & `git log -1` trên laptop.
  - **8 phút:** Demo main flow (tạo lab mới ngay tại chỗ) + các tính năng còn lại + F5 kiểm tra bền vững dữ liệu & Network tab.
  - **2 phút:** Build checks (giảng viên chọn 2 Rules từ bảng F1–F11 để thử fail case + 1 check phân quyền).
  - **~4 phút/người:** Vấn đáp cá nhân. **Trợ lý AI phải đóng hoàn toàn**, nhưng được mở repo, terminal, git log, tests, dashboard.
- **Thang điểm (100đ/người):**
  - **40đ chung cả nhóm (Final Build):** B1 (11 features × 2đ = 22đ), B2 (version `pa5` + reload + Network tab = 4đ), B3 (2 failure rules = 6đ), B4 (1 role/ownership check = 4đ), B5 (Beyond the brief có test = 4đ).
  - **60đ cá nhân (Explanation):** Kiểm tra 4 mảng: (A) Đọc hiểu code của mình, (B) Lý do quyết định thiết kế & đánh đổi, (C) Cách chứng minh code chạy đúng & truy vết incident, (D) Chạy thử Checker CLI trên 1 file C++ lạ & giải thích tại chỗ.

---

## 5. Hiện Trạng Repository `LeeTKOB-AWAD` (Sau `PA#2`)

### 5.1. Thông tin nhóm & Tech Stack hiện tại
- **Thành viên:** Trà Văn Sỹ (`23120197`), Huỳnh Đức Thịnh (`23120199` — Nhóm trưởng), Lê Trung Thành Đạt (`23120228`).
- **Backend (`backend/`):** Java 21 LTS, Spring Boot 3.4.3 (`spring-boot-starter-web`, `jdbc`, `validation`), Maven Wrapper (`mvnw` / `mvnw.cmd`), H2 Database (`MODE=PostgreSQL`).
- **Tooling gốc:** Node.js ≥ 22 điều phối lệnh qua `package.json` và `scripts/mvnw.mjs`.

### 5.2. Những gì ĐÃ CÓ và làm rất chỉn chu ở `PA#2`
1. **Harness & Permissions hoàn chỉnh:**
   - `AGENTS.md`, `CLAUDE.md`, `docs/permissions.md`, `.claude/settings.json` phân rõ 3 tầng `Allowed` / `Ask first` / `Never`, truy vết rõ về các sự cố `A01`–`A13` trong `AI-LOG.md`.
   - `.env.example` khớp 100% các biến đọc trong `application.properties` / `AppConfig.java` (`SERVER_HOST`, `SERVER_PORT`, `DATABASE_URL`, `DEMO_AUTH_TOKEN`).
2. **Quality Gate & CI chặn merge thật:**
   - 4 lệnh thống nhất local & CI: `npm run lint` (Checkstyle), `npm run format:check` (Spotless), `npm run typecheck` (`test-compile`), `npm test` (JUnit 5 với `failIfNoTests=true`).
   - `.github/workflows/ci.yml` chạy 5 jobs độc lập (`lint`, `format-check`, `typecheck`, `test`, `secrets` dùng `sec:scan` + Gitleaks 8.30.1).
   - Nhánh `main` đã bật Branch Protection (`enforce_admins: true`, bắt buộc 5 checks), có bằng chứng PR #1, #2, #4 bị chặn đỏ rồi sửa xanh.
3. **Nền tảng DB & Endpoint `POST /v1/labs/{labId}/publish`:**
   - `schema.sql` đã có sẵn 7 bảng nền: `labs`, `ta_classes`, `counted_submissions`, `review_items`, `publications`, `published_marks`, `publication_audit`.
   - `PublishController` → `PublishService` → `LabPublishRepository`: xử lý đúng RFC 9457 (`application/problem+json`), 13 mã lỗi, khóa dòng DB (`SELECT ... FOR UPDATE`), kiểm tra quyền sở hữu `ta_classes` trước khi đọc body, và Idempotency vĩnh viễn.
   - Đã có **32 bài test xanh** (23 Contract Tests + 9 Unit Tests).

### 5.3. Khoảng trống cần xây dựng cho `PA#3` (và chuẩn bị cho `PA#4`)
Ở `PA#2`, nhóm chủ động giữ phạm vi tối giản nên các thành phần sau **chưa có** và là việc phải làm ngay ở `PA#3`:

| Hạng mục còn thiếu | Chi tiết cần làm cho `PA#3` |
| :--- | :--- |
| **1. Auth & Quản lý Class/Lab/Test** | Thay `DemoAuthAdapter` bằng hệ thống tài khoản 3 roles (`admin`, `ta`, `student`), phân công `ta_classes`, CRUD Lab (`statement`, `tests`, `constraints` + `deduction`). |
| **2. Frontend / UI** | Hiện chưa có UI. Cần xây màn hình cho Admin, TA (tạo lab, xem Review Queue với verdict + evidence + deduction, nút Publish) và Student (nộp C++, xem trạng thái real-time). |
| **3. Queue & Separate Worker** | Hiện chưa có hàng đợi bất đồng bộ. Cần tách **Worker service/process riêng** (ví dụ DB-backed queue, Redis/RabbitMQ, hoặc Spring worker tách biệt) đảm bảo tắt worker nộp bài rồi bật lại vẫn chấm **đúng 1 lần**. |
| **4. Sandbox C++ (9 Minimums)** | Hiện chưa có sandbox. Cần dựng container Docker (hoặc self-hosted Judge0) để compile & run C++ với đủ 9 ràng buộc `M1–M9`, kèm **9 file attack `.cpp`** chạy được trong CI. |
| **5. Hard Rules (Syntax-aware)** | Viết bộ phân tích cú pháp C++ (ví dụ: Tree-sitter C++, Clang AST, hoặc lexer/parser C++ chuẩn — *tránh regex thô*) xử lý `"Do not use"` / `"Do not call"` và vượt qua các bẫy (`sort` tự viết, comment/string, `<algorithm>` cho `std::swap`). |
| **6. LLM Integration & Call Log** | Chọn provider & model (tính chi phí cho 80 bài), viết bước **strip comments + student identity**, gọi model thật trả về JSON (`pass`/`violation`/`unsure` + `evidence`), lưu call log đầy đủ `request_id`, tokens, latency, cost. |
| **7. Guardrails bước đầu (PA#3)** | Schema validation + Timeout (cả hai chuyển thành `unsure`), có Unit Test dùng fake model, không mạng. *(Sau đó PA#4 sẽ thêm Evidence matching, Cost cap, No-deduction).* |
| **8. Checker CLI + `MODEL_OFF=1` + Dev Set** | Đóng gói CLI + `Dockerfile` đọc folder dataset (`statement.md`, `constraints.json`, các file `.cpp`) → xuất JSONL predictions. Tải **dev set** & `eval.mjs` chính thức về (giữ nguyên SHA-256) + viết **≥ 5 cases riêng**. Chạy 1 lần real-model dev run và commit kết quả. |
| **9. Public Deployment** | Deploy toàn bộ hệ thống lên server public (HTTPS) và giữ chạy xuyên suốt từ hạn `PA#3` (25/11) qua kỳ chấm `PA#4`. |

---

## 6. Lời Khuyên Chiến Lược Khi Bắt Tay Vào Code `PA#3`

1. **Giữ vững quy tắc Harness đã thiết lập ở `PA#2`:** Mọi thay đổi lên `main` tiếp tục đi qua PR; viết test trước hoặc cùng commit với code cho từng phần (Sandbox, Queue, Hard Rules, Model Call, Limits) — **tuyệt đối không gộp 1 cục commit cuối giờ**.
2. **Thiết kế Checker dùng chung:** Nên tách logic **Hard Rules + Comment/Identity Stripper + LLM Client + Guardrails** thành một module/thư viện dùng chung giữa **Backend Worker** (chấm bài nộp trên web) và **Checker CLI** (chấm dataset folder cho `eval.mjs`). Làm vậy vừa đỡ trùng code, vừa đảm bảo những gì `eval.mjs` đo trên CI phản ánh đúng 100% hành vi trên web.
3. **Nhìn trước sang `PA#4` (chỉ có 8 ngày sau `PA#3`):** Khi làm `PA#3`, hãy cấu trúc sẵn chỗ cho 3 guardrail còn lại (`Evidence line matching` nhớ map lại số dòng sau khi strip comment, `Cost cap per lab`, `No deduction from model`) và thiết kế Review Queue sao cho `PA#4` chỉ việc bật thêm random 10% `pass` sampling và API chặn Publish.
4. **Cập nhật `AI-LOG.md` song song lúc làm:** Mỗi lần trợ lý AI viết sai (ví dụ regex dính comment, quên map line number, cấu hình Docker sandbox hở socket/env), hãy ghi lại ngay thành 1 entry `Observed / Changed / Rejected` để không bị quên bằng chứng.
