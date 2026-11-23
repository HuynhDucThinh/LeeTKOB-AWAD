# Hướng Dẫn Khởi Chạy Toàn Diện Hệ Thống LeeTKOB (Database, Backend, Frontend)

Tài liệu này hướng dẫn chi tiết từng bước để thiết lập cơ sở dữ liệu, cấu hình và khởi chạy cả **Backend (Spring Boot)** và **Frontend (React + Vite)** của dự án **LeeTKOB**.

---

## 1. Yêu Cầu Môi Trường (Prerequisites)

Trước khi bắt đầu, hãy đảm bảo máy tính đã cài đặt:

- **Java JDK 21 LTS** (Khuyến nghị: Eclipse Temurin hoặc OpenJDK 21)
  - Kiểm tra: `java -version`
- **Node.js ≥ 20** và **npm**
  - Kiểm tra: `node -v` và `npm -v`
- **PostgreSQL ≥ 15** (Hoặc cài qua **Docker**)
  - Kiểm tra: `psql --version` hoặc `docker --version`

---

## 2. Bước 1: Khởi Chạy Cơ Sở Dữ Liệu (PostgreSQL)

Hệ thống sử dụng **PostgreSQL** cùng công cụ **Flyway** để tự động khởi tạo và đồng bộ hóa schema (`db/migration/V1__init_user_auth.sql`).

Bạn có thể chọn 1 trong 2 cách sau:

### Cách 1: Sử dụng Docker (Khuyến nghị - Nhanh & Độc lập)

Mở terminal và chạy lệnh sau để khởi chạy một container PostgreSQL:

```bash
docker run --name leetkob-postgres \
  -e POSTGRES_DB=leetkob \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:16-alpine
```

Kiểm tra container đang chạy:
```bash
docker ps
```

### Cách 2: Sử dụng PostgreSQL cài đặt cục bộ (Local Service)

1. Mở **pgAdmin** hoặc dòng lệnh `psql`:
   ```bash
   psql -U postgres
   ```
2. Tạo cơ sở dữ liệu `leetkob`:
   ```sql
   CREATE DATABASE leetkob;
   ```
3. Đảm bảo port `5432` đang lắng nghe và thông tin user/password khớp với cấu hình môi trường bên dưới.

> **Lưu ý về Migration**: Khi Backend khởi động, **Flyway** sẽ tự động thực thi script `V1__init_user_auth.sql` tạo schema, các bảng `users`, `oauth_accounts` và enum `oauth_provider`. Bạn **không cần** tự chạy tay file SQL.

---

## 3. Bước 2: Cấu Hình & Khởi Chạy Backend (Spring Boot)

Backend được xây dựng trên nền tảng **Spring Boot 4 / Java 21** với **Spring Data JPA, Spring Security, JWT (JJWT) và OAuth2 Client**.

### 3.1. Cấu hình Biến Môi Trường

Vào thư mục `backend/`, copy file `.env.example` thành `.env`:

```bash
cd backend
cp .env.example .env
```
*(Trên Windows PowerShell: `Copy-Item .env.example .env`)*

Nội dung file `backend/.env` mẫu:

```env
# Application Server Port
PORT=8080

# PostgreSQL Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/leetkob
DB_USERNAME=postgres
DB_PASSWORD=postgres

# JWT Configuration (Secret tối thiểu 256-bit cho HS256)
JWT_SECRET=defaultSecretKeyWhichIsAtLeast32BytesLongForHS256Algorithm!
JWT_EXPIRATION_MS=86400000

# Google OAuth2 Credentials (Điền nếu cần đăng nhập Google OAuth)
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# OAuth2 Frontend Redirect URI
OAUTH2_REDIRECT_URI=http://localhost:5173/
```

### 3.2. Khởi chạy Backend bằng Gradle Wrapper

Từ thư mục `backend/`:

- **Trên Windows (PowerShell / CMD):**
  ```powershell
  .\gradlew.bat bootRun
  ```

- **Trên macOS / Linux:**
  ```bash
  chmod +x gradlew
  ./gradlew bootRun
  ```

Khi terminal xuất hiện dòng tương tự:
```
Started LeetKobApplication in ... seconds (process running for ...)
```
Backend đã sẵn sàng hoạt động tại địa chỉ: `http://localhost:8080`.

---

## 4. Bước 3: Cấu Hình & Khởi Chạy Frontend (React + Vite)

Frontend là Single Page Application (SPA) xây dựng bằng **React 18, TypeScript, Redux Toolkit, React Router DOM và Vite**.

### 4.1. Cài đặt Dependencies

Mở một cửa sổ terminal mới và di chuyển vào thư mục `frontend/`:

```bash
cd frontend
npm install
```

### 4.2. Khởi chạy Frontend ở Chế độ Phát triển (Dev Mode)

```bash
npm run dev
```

Terminal sẽ hiển thị đường dẫn truy cập cục bộ:
```
  VITE v8.3.0  ready in ... ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
```

Mở trình duyệt và truy cập: **`http://localhost:5173`**

---

## 5. Kiểm Thử Luồng Hoạt Động (End-to-End Auth Flow)

1. **Đăng ký tài khoản (Sign Up):**
   - Truy cập giao diện tại `http://localhost:5173`.
   - Chuyển sang tab **"Đăng ký"**.
   - Nhập họ tên, email và mật khẩu (tối thiểu 6 ký tự) -> Bấm **Đăng ký**.
   - Hệ thống tạo người dùng trong PostgreSQL, mã hóa mật khẩu bằng BCrypt và tự động cấp JWT lưu vào HTTP-only cookie.
2. **Đăng nhập (Sign In):**
   - Đăng nhập với email và mật khẩu vừa đăng ký -> Chuyển hướng thành công vào trang **Welcome / Dashboard**.
3. **Đăng nhập Google OAuth2 (Tùy chọn):**
   - Nhấn nút **"Đăng nhập với Google"** để chuyển hướng qua Google Authorization Server (yêu cầu điền đúng `GOOGLE_CLIENT_ID` và `GOOGLE_CLIENT_SECRET` trong `.env`).
4. **Đăng xuất (Sign Out):**
   - Tại trang Welcome, bấm nút **"Đăng xuất"** để xóa cookie và trở về trang đăng nhập.

---

## 6. Danh Mục Cổng & Endpoint Chính

| Dịch vụ | Địa chỉ | Ghi chú |
| :--- | :--- | :--- |
| **Frontend** | `http://localhost:5173` | Giao diện người dùng React Vite |
| **Backend API** | `http://localhost:8080` | Spring Boot REST API |
| **PostgreSQL** | `localhost:5432` | Database `leetkob` |
| `POST /v1/auth/signup` | API đăng ký tài khoản | Body: `{ email, password, fullName }` |
| `POST /v1/auth/login` | API đăng nhập tài khoản | Body: `{ email, password }` |
| `GET /v1/auth/me` | Lấy thông tin user hiện tại | Xác thực qua JWT Cookie |
| `POST /v1/auth/logout` | Đăng xuất người dùng | Xóa JWT Cookie |
| `/oauth2/authorization/google` | Luồng Google OAuth2 | Spring Security OAuth2 Client |

---

## 7. Các Lệnh Hữu Ích Thường Dùng

- **Biên dịch Frontend:**
  ```bash
  cd frontend
  npm run build
  ```
- **Biên dịch Backend:**
  ```bash
  cd backend
  .\gradlew.bat build -x test
  ```
- **Dừng Docker Postgres:**
  ```bash
  docker stop leetkob-postgres
  docker rm leetkob-postgres
  ```
