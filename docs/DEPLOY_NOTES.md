# Ghi chú Triển khai Hệ thống (Deployment Notes) — LeeTKOB

Tài liệu này tổng hợp các lưu ý kỹ thuật **bắt buộc phải thay đổi** khi chuyển hệ thống từ môi trường **Local Dev** sang môi trường **Deploy (Staging / Production)**.

---

## 1. Phân tách Vai trò Tiến trình (`LEETKOB_ROLE`)

> [!IMPORTANT]
> Giá trị mặc định `LEETKOB_ROLE=all` **CHỈ DÙNG CHO LOCAL DEV** (gộp HTTP API + `JudgeWorker` trong cùng 1 JVM để dễ debug).
> **Khi deploy thật, BẮT BUỘC phải tách thành 2 container / 2 JVM riêng biệt:** `api` và `worker`.

| Môi trường | Giá trị `LEETKOB_ROLE` | Hành vi & Tài nguyên |
| :--- | :--- | :--- |
| **Local Dev** | `all` *(mặc định)* | Mở cả HTTP API và vòng lặp nền `JudgeWorker` trong 1 JVM. |
| **Deploy — API Container** | `api` | Chỉ phục vụ HTTP REST API + SSE stream. **Không** chạy `JudgeWorker`, không cần quyền Docker. |
| **Deploy — Worker Container** | `worker` | Chỉ chạy `JudgeWorker` nền (`judge-worker-loop`) để bốc job và mở Docker Sandbox chấm C++. Có thể nhân bản nhiều container (`replicas >= 2`) để scale ngang. |

---

## 2. Cơ chế Giao tiếp giữa 2 Container Tách Rời (`api` ↔ `worker`)

Khi tách thành 2 JVM độc lập, `api` và `worker` không dùng chung bộ nhớ RAM, nên **PostgreSQL** là cầu nối trung tâm cho cả hai luồng:

1. **Luồng Hàng đợi Chấm bài (API $\rightarrow$ Worker):**
   - `api` lưu bài nộp (`submissions`) và tạo job (`judge_jobs` với `status = 'queued'`) trong cùng 1 transaction.
   - `worker` dùng cơ chế Adaptive Polling (250ms nếu rỗng) chạy câu lệnh SQL khóa dòng:
     ```sql
     SELECT * FROM judge_jobs
     WHERE status = 'queued'
     ORDER BY id ASC
     FOR UPDATE SKIP LOCKED
     LIMIT 1
     ```
   - Đảm bảo **Exact-Once (Q1)**: nhiều Worker chạy song song cũng không bao giờ bốc trùng 1 bài nộp.
   - `JudgeWorker` tách thành 2 transaction ngắn (`claimNextJob` commit `running` $\rightarrow$ chạy Docker ngoài transaction $\rightarrow$ `completeJob` commit kết quả cuối), giúp không giữ DB connection pool trong lúc Docker đang biên dịch C++.

2. **Luồng Thông báo Real-time SSE (Worker $\rightarrow$ API $\rightarrow$ Trình duyệt):**
   - Endpoint `GET /v1/submissions/{id}/events` (`SubmissionEventBroadcaster`) hỗ trợ tự động cả 2 chế độ:
     - **Chế độ 1 JVM (`all`):** `JudgeWorker` gọi thẳng `broadcaster.publish(event)` $\rightarrow$ đẩy ngay ở `0ms`.
     - **Chế độ 2 JVM (`api` + `worker`):** `api` chạy luồng nền `sse-db-poller` quét DB mỗi **`350ms`** (**chỉ query đối với các `submissionId` đang có trình duyệt mở kết nối SSE**; nếu không có ai xem thì 0 query). Khi thấy `judgeStatus` thay đổi (`queued` $\rightarrow$ `running` $\rightarrow$ `AC`/`WA`/`CE`...), `api` tự đẩy SSE qua `SseEmitter` và đóng stream khi đạt trạng thái cuối.

---

## 3. Chuyển đổi từ Mock sang Chạy Thật (Real Sandbox & Checker)

| Thành phần | Chế độ Test / Dev | Khi Deploy Production |
| :--- | :--- | :--- |
| **C++ Sandbox (`SandboxRunner`)** | `LEETKOB_SANDBOX_ENABLED=false` $\rightarrow$ Dùng `MockSandboxRunner` (giả lập kết quả không cần Docker). | **Bắt buộc đặt `LEETKOB_SANDBOX_ENABLED=true`** trên container `worker` để kích hoạt `DockerSandboxRunner` (M1–M9). |
| **Sandbox Image** | Tự động build từ `sandbox/Dockerfile` qua service `sandbox-image` trong `docker-compose.yml` (`docker compose up -d --build`). | Đảm bảo image `leetkob-sandbox:latest` được build sẵn trên host VPS (qua `docker compose build sandbox-image` hoặc pull từ Registry `SANDBOX_IMAGE`). |
| **Docker Socket** | Chỉ dùng trên máy dev có Docker Desktop. | Chỉ mount `/var/run/docker.sock:/var/run/docker.sock` vào container **`worker`**. **Tuyệt đối KHÔNG** mount vào container `api` hay vào bên trong container sandbox chạy code sinh viên (`M8`). |
| **Constraint Checker** | `MockConstraintCheckPipeline` (0 LLM calls, mặc định `pass`). | Khi Thành viên 3 hoàn tất bean `realConstraintCheckPipeline`, Spring tự động thay thế `MockConstraintCheckPipeline` qua `@ConditionalOnMissingBean`. |

---

## 4. Các Biến Môi trường URL & Bảo mật (Không hardcode `localhost`)

Tất cả các cấu hình phụ thuộc tên miền đều đã tách ra biến môi trường (liệt kê đầy đủ trong `.env.example`):

| Biến môi trường | Mặc định (Local Dev) | Bắt buộc đổi khi Deploy |
| :--- | :--- | :--- |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | Domain thật của Frontend, ví dụ `https://leetkob.example.com` (nhiều domain phân tách bằng dấu `,`). |
| `OAUTH2_REDIRECT_URI` | `http://localhost:5173/` | URL trang chủ Frontend sau khi đăng nhập Google OAuth2. |
| `BACKEND_URL` | `http://127.0.0.1:8080` | URL Backend cho Vite proxy (nếu chạy qua Vite dev/preview). |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/leetkob` | Chuỗi JDBC trỏ tới PostgreSQL service/server thật (ví dụ `jdbc:postgresql://postgres:5432/leetkob`). |
| `DEMO_AUTH_TOKEN` | *(Không có — fail-closed)* | Chuỗi ngẫu nhiên **>= 32 ký tự**. Thiếu hoặc yếu server sẽ từ chối khởi động. |
| `JWT_SECRET` | *(Không có — fail-closed)* | Chuỗi ngẫu nhiên **>= 32 ký tự**. Tuyệt đối không dùng khóa mẫu. |
