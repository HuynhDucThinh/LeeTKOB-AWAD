# Hướng dẫn Cài đặt & Chạy Dự án cho Thành viên Mới (Onboarding Guide)

Tài liệu này hướng dẫn từng bước để người mới tham gia dự án có thể **khởi chạy đầy đủ Frontend + Backend + PostgreSQL + Docker Sandbox** trên máy cá nhân.

---

## 1. Yêu cầu Phần mềm (Prerequisites)

Trước khi bắt đầu, hãy đảm bảo máy tính đã cài đặt:
- **Java 21 LTS** (JDK 21 — kiểm tra bằng `java -version`).
- **Node.js ≥ 22** (kiểm tra bằng `node -v`).
- **Docker Desktop** (Windows/macOS) hoặc **Docker Engine + Docker Compose** (Linux).
- **Git** (trên Windows nên dùng **Git Bash** để chạy các lệnh shell đồng nhất).

---

## 2. Cấu hình Biến Môi trường (`.env`)

1. Tại thư mục gốc (và thư mục `backend/`), tạo file `.env` dựa trên danh sách biến trong [`.env.example`](../.env.example).
   > **Lưu ý:** Tuyệt đối không commit file `.env` lên Git (đã được chặn sẵn trong `.gitignore`).

2. Điền cấu hình chuẩn cho môi trường **Local Dev** vào `backend/.env`:
   ```env
   SERVER_HOST=127.0.0.1
   SERVER_PORT=8080

   DATABASE_URL=jdbc:postgresql://localhost:5432/leetkob
   DB_USERNAME=postgres
   DB_PASSWORD=postgres

   # Bắt buộc >= 32 ký tự (nếu thiếu hoặc ngắn hơn 32 ký tự, Backend sẽ không khởi động)
   DEMO_AUTH_TOKEN=local-dev-demo-auth-token-at-least-32-chars-long
   JWT_SECRET=local-dev-jwt-secret-key-at-least-32-chars-long
   JWT_EXPIRATION_MS=86400000

   CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
   OAUTH2_REDIRECT_URI=http://localhost:5173/
   BACKEND_URL=http://127.0.0.1:8080

   # Chạy gộp API + Worker trong 1 tiến trình cho tiện dev local
   LEETKOB_ROLE=all

   # Đặt true để chạy Docker Sandbox thật; đặt false nếu máy chưa bật Docker (dùng Mock)
   LEETKOB_SANDBOX_ENABLED=true
   SANDBOX_IMAGE=leetkob-sandbox:latest

   MODEL_OFF=true
   ```

---

## 3. Khởi động Database & Tự động Build Image Docker Sandbox

Mở **Docker Desktop**, sau đó chạy **1 lệnh duy nhất** tại thư mục gốc dự án:

```bash
# Tự động bật PostgreSQL 16 (cổng 5432) VÀ build image leetkob-sandbox:latest từ ./sandbox
docker compose up -d --build
```

*(Service `sandbox-image` trong `docker-compose.yml` sẽ build sẵn image `leetkob-sandbox:latest` rồi tự thoát ngay, không tốn RAM chạy nền. Nếu bạn không sửa `sandbox/Dockerfile`, những lần sau chỉ cần chạy `docker compose up -d`).*

Kiểm tra nhanh image đã có chưa:
```bash
docker images | grep leetkob-sandbox
```

---

## 4. Khởi động Backend & Frontend

Mở **2 cửa sổ Terminal** riêng biệt tại thư mục gốc dự án:

- **Terminal 1 — Khởi động Backend (Spring Boot + Flyway + JudgeWorker):**
  ```bash
  # Trên Windows (Git Bash / CMD):
  ./backend/gradlew.bat -p backend bootRun

  # Trên macOS / Linux:
  ./backend/gradlew -p backend bootRun
  ```
  - Khi khởi động xong, Backend lắng nghe tại `http://127.0.0.1:8080`.
  - Flyway sẽ tự động chạy các migration SQL (`V1` $\rightarrow$ `V4`) tạo đầy đủ bảng trong PostgreSQL.
  - Kiểm tra log có dòng: `JudgeWorker background loop enabled for role=all, sandboxRunner=DockerSandboxRunner`.

- **Terminal 2 — Khởi động Frontend (React + Vite):**
  ```bash
  cd frontend
  npm install
  npm run dev
  ```
  - Mở trình duyệt truy cập: **`http://localhost:5173`**.

---

## 5. Nạp Dữ liệu Mẫu & Kiểm tra Luồng Chấm bài

Nếu database mới chưa có bài Lab mẫu, bạn có thể nạp nhanh bộ dữ liệu demo (`Lab 9001 - Cộng 2 số A + B` kèm 3 test cases):

```bash
docker exec -i leetkob-postgres psql -U postgres -d leetkob < Personal_doc/seed_demo_lab.sql
```

- **Theo dõi container Sandbox chạy thực tế:** Mở thêm 1 terminal chạy `docker events --filter "image=leetkob-sandbox:latest"`.
- Khi sinh viên đăng nhập trên Web (`http://localhost:5173`) hoặc gọi API `POST /v1/labs/9001/submissions`, bạn sẽ thấy:
  1. Container `leetkob-sbx-...` được tạo ra, biên dịch `g++` và tự hủy (`--rm`).
  2. Giao diện Web nhận trạng thái real-time qua SSE (`queued` $\rightarrow$ `running` $\rightarrow$ `AC`) mà không cần F5.

---

## 6. Bộ Lệnh Kiểm tra Chất lượng Trước khi Tạo Pull Request

Bắt buộc chạy xanh các lệnh sau tại thư mục gốc trước khi commit/push:

```bash
npm run lint          # Kiểm tra quy tắc Checkstyle Java
npm run format:check  # Kiểm tra định dạng Google Java Format (Spotless)
npm run typecheck     # Biên dịch toàn bộ main + test
npm test              # Chạy toàn bộ Unit Tests và HTTP Contract Tests
npm run sec:scan      # Quét lộ lọt secret trên toàn bộ lịch sử Git
```

- Tự động sửa lỗi format Java (nếu `format:check` báo đỏ):
  ```bash
  npm run format
  ```
- Chạy riêng bộ kiểm thử bảo mật Docker Sandbox (`M1–M9`, yêu cầu Docker đang bật và đã build `leetkob-sandbox:latest`):
  ```bash
  ./backend/gradlew.bat -p backend test -DincludeTags=sandbox
  ```
