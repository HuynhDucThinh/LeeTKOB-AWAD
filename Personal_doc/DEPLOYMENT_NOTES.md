# Ghi chú Triển khai & Biến Môi trường (`DEPLOYMENT_NOTES.md`)

Tài liệu này ghi chú các cấu hình quan trọng giữa môi trường **Local Dev** và môi trường **Deploy (Production / Staging)** để tránh cấu hình sai khi đưa hệ thống lên server thật.

---

## 1. Phân tách vai trò tiến trình (`LEETKOB_ROLE`)

> [!IMPORTANT]
> Giá trị mặc định `LEETKOB_ROLE=all` **CHỈ DÙNG CHO MÔI TRƯỜNG DEV LOCAL** (chạy 1 tiến trình `bootRun` duy nhất cho tiện phát triển và debug).
> **Khi deploy thật (Production / Docker Compose / Cloud), BẮT BUỘC phải tách riêng API và Worker!**

| Môi trường | Giá trị `LEETKOB_ROLE` | Ý nghĩa & Hành vi |
| :--- | :--- | :--- |
| **Local Dev** | `all` *(mặc định)* | Chạy gộp HTTP REST API + vòng lặp `JudgeWorker` trong cùng 1 JVM. Tiện lợi khi code trên máy cá nhân, không cần bật 2 terminal/container. |
| **Deploy — API Node** | `api` | Chỉ mở HTTP REST API phục vụ người dùng (nộp bài, xem điểm, auth). **Không** chạy `JudgeWorker` loop, không tốn CPU/RAM để chấm bài. |
| **Deploy — Worker Node** | `worker` | Chỉ chạy `JudgeWorker` nền để bốc job từ PostgreSQL (`FOR UPDATE SKIP LOCKED`) và gọi Docker Sandbox chấm C++. Có thể scale ngang nhiều Worker (`replica >= 2`) mà không bị đụng cổng HTTP. |

### Tại sao không dùng `LEETKOB_ROLE=all` khi Deploy?
1. **Cô lập tài nguyên (Resource Isolation):** Khi hàng chục sinh viên nộp bài cùng lúc, Worker gọi `g++` và Docker container sẽ ngốn nhiều CPU/RAM. Nếu chạy chung tiến trình với API (`all`), trang web sẽ bị giật/timeout theo.
2. **Scale độc lập:** Khi quá tải chấm bài, ta chỉ cần nhân bản thêm container `LEETKOB_ROLE=worker` mà không cần nhân bản API server.
3. **Bảo mật (Least Privilege):** Chỉ container `worker` mới cần quyền giao tiếp với Docker engine để mở Sandbox chấm code C++; container `api` hoàn toàn không cần quyền này.

---

## 2. Các biến môi trường cần đổi khi Deploy

Tất cả các giá trị mặc định `localhost` / `127.0.0.1` trong code đều đã được tách ra thành biến môi trường (khai báo trong `.env.example`):

| Biến môi trường | Mặc định (Dev Local) | Khi Deploy cần đặt thành |
| :--- | :--- | :--- |
| `LEETKOB_ROLE` | `all` | `api` (cho API container) / `worker` (cho Worker container) |
| `LEETKOB_SANDBOX_ENABLED` | `false` (dùng `MockSandboxRunner`) | **`true`** trên Worker node để dùng `DockerSandboxRunner` thật (`M1-M9`) |
| `SANDBOX_IMAGE` | `leetkob-sandbox:latest` | Tên/tag Docker image sandbox đã build hoặc đẩy lên registry |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | Domain thật của Frontend, ví dụ `https://leetkob.example.com` (nhiều domain cách nhau bằng dấu `,`) |
| `OAUTH2_REDIRECT_URI` | `http://localhost:5173/` | URL chuyển hướng sau khi đăng nhập Google OAuth2 thành công, ví dụ `https://leetkob.example.com/` |
| `BACKEND_URL` | `http://127.0.0.1:8080` | URL Backend dùng cho Vite proxy (nếu chạy qua Vite preview/dev) |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/leetkob` | Chuỗi JDBC trỏ tới PostgreSQL server/container thật (ví dụ `jdbc:postgresql://postgres:5432/leetkob`) |
| `DEMO_AUTH_TOKEN` | *(bắt buộc >= 32 ký tự)* | Chuỗi ngẫu nhiên mạnh trên server (fail-closed nếu thiếu hoặc yếu) |
| `JWT_SECRET` | *(bắt buộc >= 32 ký tự)* | Khóa ký JWT mạnh, tuyệt đối không dùng giá trị mẫu |
