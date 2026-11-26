# Kế hoạch Thực hiện Chi tiết — Thành viên 2: Lê Trung Thành Đạt (`23120228`)

> **Vai trò sở hữu (`OWNERS.md`):** Async Queue & Separate Worker (`Q1`), C++ Docker Sandbox 9 Minimums (`F4`, `M1–M9`) + 9 Attack `.cpp` trong CI, Submissions REST API.  
> **Trọng số Rubric PA#3:** **Tiêu chí 2 (20đ)** + một phần **Tiêu chí 1 (15đ)** + **Tiêu chí 7 (Tests first, one part per commit - 5đ)**.  
> **Mở rộng sang PA#4 & PA#5:** Red-team Sandbox nhóm khác (PA#4) & **Load test 80 SV đồng thời** + Traces/Metrics Worker (PA#5).

---

## 1. Quyết định Kỹ thuật & Tương tác Liên thành viên

### 1.1. Tương tác với Thành viên 3 (Checker Core)
- Worker sau khi chạy sandbox C++ ra kết quả `AC` sẽ gọi interface nội bộ:
  ```java
  public interface ConstraintCheckPipeline {
    CheckPipelineResult check(CheckRequest request);
  }
  ```
- **Cơ chế Mock:** Ở Phase 1 & 2, khi Thành viên 3 chưa hoàn tất PR 3A/3B, ta cấu hình một bean mặc định `@ConditionalOnMissingBean` là `MockConstraintCheckPipeline` trả về `verdict = "pass"` (hoặc giả lập) với 0 LLM calls.
- **Lợi ích:** Worker và các bài test hàng đợi (`Q1`) chạy độc lập 100%, không bị phụ thuộc vào tiến độ của TV3. Khi TV3 hoàn thành bean thật, Spring Boot tự động nạp thay thế mà không sửa lại 1 dòng code nào trong Worker.

### 1.2. Tương tác với Thành viên 1 (Core Platform & Labs)
- Worker đọc dữ liệu bài lab (`statementMd`, `tests`, `constraints`) thông qua `LabRepository` / `LabTestRepository`.
- Submissions API ghi nhận bài nộp của sinh viên (`POST /v1/labs/{labId}/submissions`), kiểm tra deadline và đẩy vào hàng đợi `judge_jobs`.

---

## 2. Sơ đồ Kiến trúc & Luồng Xử lý

```mermaid
flowchart TD
    subgraph Client["Sinh viên (Web UI / API Client)"]
        SUB["POST /v1/labs/{labId}/submissions<br/>(kèm Idempotency-Key, sourceCode)"]
    end

    subgraph API["Spring Boot API (LEETKOB_ROLE=api)"]
        CTRL["SubmissionController"]
        SRV["SubmissionService"]
    end

    subgraph DB["PostgreSQL 16 (Durable Queue)"]
        T_SUB[("submissions: status = queued")]
        T_JOBS[("judge_jobs: status = queued")]
        T_REV[("review_items: constraint verdicts")]
    end

    subgraph Worker["Judge Worker (LEETKOB_ROLE=worker)"]
        LOOP["JudgeWorker Loop (Adaptive Polling 250ms)<br/>SELECT ... FOR UPDATE SKIP LOCKED"]
        RUNNER["DockerSandboxRunner (M1-M9)"]
        CHECKER["ConstraintCheckPipeline<br/>(Mock ở Phase 1 -> Bean thật từ TV3)"]
        TX["Atomic Transaction Commit"]
    end

    subgraph Sandbox["Ephemeral C++ Container"]
        CPP["g++ compile & run binary<br/>--network=none, 256MB RAM, nonroot"]
    end

    SUB --> CTRL --> SRV
    SRV -->|Atomic Insert| T_SUB
    SRV -->|Atomic Insert| T_JOBS
    CTRL -->|201 Created + SubmissionSummary| SUB

    LOOP -->|1. Poll & Lock Job| T_JOBS
    LOOP -->|2. Execute Tests| RUNNER
    RUNNER -->|3. docker run --rm| CPP
    CPP -->|4. Return Result (AC/WA/CE...)| RUNNER
    RUNNER -->|5. If AC| CHECKER
    CHECKER -->|6. Verdicts & Evidence| TX
    RUNNER -->|Status & Metrics| TX
    TX -->|7. Update status=done| T_JOBS
    TX -->|7. Update judge_status| T_SUB
    TX -->|7. Insert verdicts| T_REV
```

---

## 3. Lộ trình Triển khai 3 Phase

### 🎯 PHASE 1: Async Queue & Separate Worker (`Q1` Exact-Once)
*Nhánh Git đề xuất: `feat/pa3-queue-and-worker`*

#### Bước 1.1: Tạo Mock Infrastructure
- Tạo `com.leetkob.checker.MockConstraintCheckPipeline`: Trả về `verdict = "pass"`, `evidence = []`, `callLogs = []`.
- Tạo `com.leetkob.sandbox.MockSandboxRunner`: Dùng cho Unit Tests không cần môi trường Docker thật (giả lập trả về `AC`, `WA`, `TLE`, `CE` theo input source code).

#### Bước 1.2: Viết Tests First (`JudgeWorkerExactOnceTest.java`)
- Viết test trong `backend/src/test/java/com/leetkob/worker/JudgeWorkerExactOnceTest.java`:
  1. **Kịch bản Q1 (Exact-Once):** Tắt worker $\rightarrow$ Nộp bài vào DB $\rightarrow$ Bật worker $\rightarrow$ Verify bài nộp được xử lý **chính xác 1 lần**, không sinh thêm verdict trùng lặp khi chạy lại.
  2. **Kịch bản Idempotency:** Gửi cùng một `Idempotency-Key` hai lần $\rightarrow$ DB không sinh ra 2 submission hay 2 job trùng lặp, trả về bản ghi cũ.

#### Bước 1.3: Cài đặt Entity & Repository cho Hàng đợi
- Tạo `JudgeJobEntity.java` tương ứng với bảng `judge_jobs`:
  - `id`, `submission_id`, `status` (`queued`, `running`, `done`, `failed`), `attempts`, `locked_by`, `locked_at`, `trace_parent`, `created_at`, `finished_at`.
- Tạo `JudgeJobRepository.java` với Native Query khóa dòng:
  ```sql
  SELECT * FROM judge_jobs 
  WHERE status = 'queued' 
  ORDER BY id ASC 
  FOR UPDATE SKIP LOCKED 
  LIMIT 1
  ```

#### Bước 1.4: Cài đặt Tiến trình JudgeWorker
- Tạo `JudgeWorker.java`:
  - Điều khiển kích hoạt qua cấu hình `@ConditionalOnProperty(name = "leetkob.role", havingValue = "worker")`.
  - Vòng lặp Adaptive Polling: Nếu bốc được job thì xử lý ngay và tìm job tiếp theo; nếu hết job thì sleep 250ms.
  - Xử lý job trong `@Transactional`: Đổi `status = 'running'` $\rightarrow$ Gọi `SandboxRunner` $\rightarrow$ Nếu `AC` gọi `ConstraintCheckPipeline` $\rightarrow$ Ghi kết quả vào `submissions` và `review_items` $\rightarrow$ Đổi `job.status = 'done'`.

#### Bước 1.5: Cài đặt Submissions REST API
- `POST /v1/labs/{labId}/submissions`: Kiểm tra quyền sinh viên, kiểm tra deadline, lưu submission + job trong 1 transaction, trả `201 Created` kèm `SubmissionSummary`.
- `GET /v1/labs/{labId}/submissions`: Sinh viên chỉ xem bài của mình; TA xem toàn bộ bài của lớp.
- `GET /v1/submissions/{id}`: Trả chi tiết bài nộp (lưu ý ẩn `publishedScoreTenths` và `publishedDeductions` nếu lab chưa Publish).

---

### 🛡️ PHASE 2: C++ Docker Sandbox 9 Minimums (`M1–M9`)
*Nhánh Git đề xuất: `feat/pa3-cpp-sandbox`*

#### Bước 2.1: Bộ 9 File C++ Attack Chuyên dụng
Tạo trong `backend/src/test/resources/sandbox-attacks/`:
1. `m1_network.cpp`: Mở socket TCP ra `1.1.1.1:80` hoặc phân giải DNS $\rightarrow$ Yêu cầu: Bị chặn hoàn toàn (`connect()` fail).
2. `m2_cpu_limit.cpp`: Vòng lặp tính toán CPU vô hạn `while(true)` $\rightarrow$ Yêu cầu: Bị ngắt bởi `SIGXCPU` / CPU limit (`TLE`).
3. `m3_wall_clock.cpp`: Lệnh `sleep(60)` không tốn CPU $\rightarrow$ Yêu cầu: Tiến trình Host kill container đúng hạn wall-clock (ví dụ 3s) và trả về `TLE`.
4. `m4_memory_oom.cpp`: Cấp phát 1 GiB RAM dùng con trỏ `volatile` ghi và đọc lại từng trang $\rightarrow$ Yêu cầu: Bị OOM Kill bởi giới hạn 256MB (`MLE`).
5. `m5_fork_bomb.cpp`: Vòng lặp `fork()` liên tục $\rightarrow$ Yêu cầu: Bị từ chối `EAGAIN` bởi cờ `--pids-limit=32`, không làm nghẽn host.
6. `m6_output_limit.cpp`: Vòng lặp in 50 MB ra `stdout` $\rightarrow$ Yêu cầu: Cắt output ở 1 MB và ngắt tiến trình.
7. `m7_non_root.cpp`: Thử kiểm tra `geteuid() == 0` và ghi file `/etc/pwn` $\rightarrow$ Yêu cầu: Thất bại vì user UID 10001 không có quyền root.
8. `m8_docker_socket.cpp`: Thử mở `/var/run/docker.sock` hoặc đọc file hệ thống host $\rightarrow$ Yêu cầu: Không tìm thấy đường dẫn hoặc socket.
9. `m9_no_secrets.cpp`: Thử đọc `getenv("MODEL_API_KEY")` hoặc `#include "/.env"` $\rightarrow$ Yêu cầu: Trả về `nullptr` hoặc lỗi biên dịch.

#### Bước 2.2: Viết Tests First (`DockerSandboxSecurityTest.java`)
- Tạo test kiểm thử integration với Docker: `@Tag("sandbox")`.
- Assert từng file attack trên khi chạy qua `DockerSandboxRunner` đều bị bắt chính xác theo từng mã lỗi tương ứng.

#### Bước 2.3: Xây dựng Image Docker Sandbox
- Tạo file `sandbox/Dockerfile`:
  - Base: `debian:bookworm-slim`.
  - Cài đặt: Chỉ cài `g++`, không cài `curl`, `wget`, `netcat`, hay python.
  - Tạo user không đặc quyền: `useradd -u 10001 sandbox`.
  - Thư mục làm việc: `/work`.

#### Bước 2.4: Cài đặt `DockerSandboxRunner.java`
- Thực thi lệnh Docker qua `ProcessBuilder`:
  ```bash
  docker run --rm \
    --network=none \
    --memory=256m --memory-swap=256m \
    --pids-limit=32 \
    --read-only --tmpfs /work:rw,noexec=0,size=64m \
    --user 10001:10001 --cap-drop=ALL --security-opt=no-new-privileges \
    leetkob-sandbox:latest ...
  ```
- Quản lý timeout wall-clock trên tiến trình Java để kill container nếu chạy quá hạn.
- Đọc exit code, thời gian thực thi, dung lượng RAM sử dụng và stderr để phân loại: `AC`, `WA`, `TLE`, `MLE`, `RE`, `CE`.

#### Bước 2.5: Tích hợp vào Worker
- Đăng ký `DockerSandboxRunner` làm Spring Bean triển khai `SandboxRunner` khi profile không phải test nội bộ.

---

### ⚙️ PHASE 3: CI Workflow cho Sandbox Security Tests
- Cập nhật `.github/workflows/ci.yml`:
  - Step 1: Build image `leetkob-sandbox:latest` từ `sandbox/Dockerfile`.
  - Step 2: Chạy Gradle test lọc tag: `./gradlew test -DincludeTags=sandbox`.
  - Đảm bảo 9 attack tests chạy xanh trên GitHub Actions để lấy trọn vẹn 20 điểm Tiêu chí 2 ở tag `pa3`.

---

### 📡 PHASE 4 (Giai đoạn bổ sung sau): Real-time SSE Stream
- Sau khi Phase 1 và Phase 2 đã chạy ổn định và đạt điểm kiểm thử, ta sẽ bổ sung:
  - `GET /v1/submissions/{id}/events` (`text/event-stream`).
  - `SubmissionEventBroadcaster` phát sự kiện real-time mỗi khi trạng thái bài nộp thay đổi (`queued` $\rightarrow$ `running` $\rightarrow$ `AC`/`WA`/`CE`).
  - Nối hook `useSubmissionStream.ts` trên Frontend.

---

## 4. Danh sách Quy ước Commit (Tests first, one part per commit)

| Giai đoạn | Mã Commit | Nội dung cam kết |
| :--- | :--- | :--- |
| **Phase 1** | `test(queue)` | Thêm tests first `JudgeWorkerExactOnceTest` và kịch bản idempotency |
| **Phase 1** | `feat(worker)` | Cài đặt `JudgeJobRepository` (FOR UPDATE SKIP LOCKED) và `JudgeWorker` |
| **Phase 1** | `feat(submission)` | Cài đặt `SubmissionController` REST API (POST/GET) |
| **Phase 2** | `test(sandbox)` | Thêm 9 file attack `.cpp` và `DockerSandboxSecurityTest` |
| **Phase 2** | `feat(sandbox)` | Tạo `sandbox/Dockerfile` và cài đặt `DockerSandboxRunner` (M1-M9) |
| **Phase 3** | `ci(sandbox)` | Thêm job chạy 9 sandbox security tests trên GitHub Actions |
