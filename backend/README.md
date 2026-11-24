# Hướng dẫn Chạy & Phát triển Backend LeeTKOB (Dành cho Người mới dùng Java Spring Boot)

Tài liệu này hướng dẫn từng bước từ con số 0 để cài môi trường, bật cơ sở dữ liệu PostgreSQL bằng Docker, khởi chạy server **Spring Boot**, gọi thử API và chạy bộ kiểm tra chất lượng trước khi mở Pull Request.

---

## 1. Kiến trúc & Công nghệ đang dùng

- **Ngôn ngữ & Framework:** Java 21 LTS + Spring Boot 4.1.1
- **Công cụ build:** **Gradle Wrapper** (`gradlew.bat` trên Windows, `./gradlew` trên macOS/Linux) — **không cần cài Gradle rời**, chạy lệnh lần đầu wrapper sẽ tự tải đúng phiên bản Gradle `9.7.1`.
- **Cơ sở dữ liệu:**
  - **Khi chạy thật (`bootRun`):** **PostgreSQL 16** chạy trên Docker + **Flyway** tự động tạo bảng khi app khởi động.
  - **Khi chạy test (`npm test` / CI):** **H2 in-memory** (trong RAM) — chạy hoàn toàn độc lập, không cần bật Docker.

### Ý nghĩa từng thư mục trong `backend/src/main/java/com/leetkob/`

Nếu bạn mới làm quen với Spring Boot, cứ hình dung luồng đi của một HTTP request là:  
`Client → controllers → services → repositories → Database`

| Thư mục | Vai trò dễ hiểu | Ví dụ trong dự án |
| :--- | :--- | :--- |
| `LeetKobApplication.java` | Điểm khởi chạy duy nhất của cả server (`@SpringBootApplication`) | `main()` |
| `controllers/` | Nhận HTTP request, kiểm tra đầu vào, trả HTTP response | `AuthController`, `PublishController` |
| `services/` | Xử lý logic nghiệp vụ, giao dịch (`@Transactional`) | `AuthService`, `OAuthAccountService`, `PublishService` |
| `repositories/` | Truy vấn cơ sở dữ liệu (JPA hoặc JDBC SQL) | `UserRepository`, `LabPublishRepository` |
| `entities/` | Class Java ánh xạ trực tiếp sang bảng DB bằng JPA (`@Entity`) | `User`, `OauthAccount` |
| `dtos/` | Dữ liệu JSON gửi lên (Request) và trả về (Response) | `SignupRequest`, `LoginRequest`, `UserResponse`, `PublicationReceipt` |
| `domain/` | Luật tính toán nghiệp vụ thuần Java (không dính Spring/DB) | `MarkCalculator`, `LabRecord` |
| `mappers/` | Chuyển đổi qua lại giữa `Entity` và `DTO` (dùng MapStruct) | `UserMapper` |
| `security/` | Cấu hình Spring Security, JWT Cookie, Google OAuth2, Bearer Auth | `SecurityConfig`, `JwtUtil`, `DemoAuthAdapter` |
| `exceptions/` | Bắt lỗi tập trung và trả lỗi chuẩn (`GlobalExceptionHandler`, RFC 9457) | `GlobalExceptionHandler`, `ProblemResponseWriter` |
| `config/` | Khai báo các `@Bean` dùng chung (`Clock`, `ObjectMapper`, `AuthResolver`) | `AppConfig` |
| `enums/` | Các hằng số liệt kê | `OauthProvider` |

---

## 2. Yêu cầu Cài đặt Ban đầu (Làm 1 lần)

1. **Java JDK 21 LTS** (bắt buộc đúng Java 21, không dùng Java 8/17):
   - Tải tại [Adoptium Eclipse Temurin 21](https://adoptium.net/) hoặc Oracle JDK 21.
   - Mở terminal kiểm tra:
     ```powershell
     java -version
     ```
     Kết quả hiển thị `21.x.x` là đạt.
2. **Docker Desktop** (để chạy PostgreSQL mà không cần cài đặt phức tạp):
   - Tải tại [docker.com](https://www.docker.com/products/docker-desktop/) và mở ứng dụng **Docker Desktop** cho tới khi trạng thái báo *Engine running*.
3. **Node.js ≥ 22** (dùng để chạy thống nhất các lệnh kiểm tra `npm run lint`, `npm test`... ở thư mục gốc):
   - Kiểm tra: `node -v`.
4. **IDE khuyên dùng:** IntelliJ IDEA (Community hoặc Ultimate) hoặc VS Code cài thêm gói *Extension Pack for Java*.

---

## 3. Bước 1 — Bật PostgreSQL bằng Docker

Mở terminal tại **thư mục gốc của repository** (nơi có file `docker-compose.yml`) và chạy:

```powershell
docker compose up -d
```

Kiểm tra container đã lên trạng thái `healthy`:

```powershell
docker compose ps
```

### Kết nối xem database bằng DBeaver (tùy chọn)
Tạo kết nối mới chọn **PostgreSQL** và điền:
- **Host:** `localhost`
- **Port:** `5432`
- **Database:** `leetkob`
- **Username:** `postgres`
- **Password:** `postgres`

### Hoặc xem nhanh các bảng bằng dòng lệnh `psql`
```powershell
docker exec -it leetkob-postgres psql -U postgres -d leetkob -c "\dt"
```

> **Lưu ý về Flyway:** Bạn **không cần** chạy tay file SQL nào. Khi khởi chạy Backend ở Bước 2, Flyway sẽ tự động đọc thư mục `backend/src/main/resources/db/migration/` (`V1__init_user_auth.sql`, `V2__init_lab_publish.sql`, `V3__add_roles_and_classes.sql`) và tạo toàn bộ các bảng vào schema `public`.

### Tài khoản mẫu để test & Script nạp dữ liệu mẫu (`npm run db:seed`)

Khi Backend khởi động lần đầu (`bootRun`), `DemoAccountSeeder` sẽ tự động tạo sẵn **2 lớp học (Class 10, Class 20)** và **5 tài khoản test chuẩn PA#3** dưới đây:

| Email | Mật khẩu | Vai trò (`role`) | Lớp (`classIds`) | Mục đích test |
| :--- | :--- | :---: | :---: | :--- |
| `admin@leetkob.edu` | `Admin@123` | `admin` | `[]` | Quản trị lớp học & tài khoản |
| `ta1@leetkob.edu` | `Ta1@1234` | `ta` | `[10]` | TA phụ trách Class 10 (tạo lab, duyệt Review Queue) |
| `ta2@leetkob.edu` | `Ta2@1234` | `ta` | `[20]` | TA phụ trách Class 20 (kiểm chứng cô lập lớp → `404`) |
| `student1@leetkob.edu` | `Student1@123` | `student` | `[10]` | Sinh viên 1 học Class 10 |
| `student2@leetkob.edu` | `Student2@123` | `student` | `[10]` | Sinh viên 2 học Class 10 |

Để nạp thêm toàn bộ **dữ liệu mẫu** (gồm cả 5 tài khoản, 2 lớp học, bài Lab 1 & Lab 2, `ta_classes`, bài nộp `counted_submissions` và `review_items`) vào PostgreSQL Docker bất kỳ lúc nào (chạy lại nhiều lần vẫn an toàn, không trùng dữ liệu):

```powershell
npm run db:seed
```

*(Script `scripts/seed-db.mjs` thực thi file `backend/src/main/resources/db/seed/seed_sample_data.sql` trực tiếp vào container `leetkob-postgres`. Hãy chạy sau khi đã bật `docker compose up -d` và chạy Backend ít nhất 1 lần để Flyway tạo bảng.)*

---

## 4. Bước 2 — Khởi chạy Backend Spring Boot

Backend áp dụng cơ chế xác thực **fail-closed**, nên bắt buộc phải cung cấp biến môi trường `DEMO_AUTH_TOKEN` dài **tối thiểu 32 ký tự** (nếu thiếu hoặc quá ngắn, Spring Boot sẽ chủ động từ chối khởi động).

### Trên Windows PowerShell *(khuyên dùng — gõ từng dòng)*

```powershell
cd backend
$env:DEMO_AUTH_TOKEN = "demo-local-token-0123456789abcdef"
.\gradlew.bat bootRun
```

*(Hoặc chạy từ thư mục gốc bằng npm: đặt `$env:DEMO_AUTH_TOKEN = "demo-local-token-0123456789abcdef"` rồi chạy `npm start`.)*

### Trên macOS / Linux / Git Bash

```bash
cd backend
DEMO_AUTH_TOKEN="demo-local-token-0123456789abcdef" ./gradlew bootRun
```

### Dấu hiệu khởi chạy thành công
Khi terminal hiện các dòng sau nghĩa là cả Flyway lẫn server đều đã sẵn sàng tại **`http://127.0.0.1:8080`**:

```text
Database: jdbc:postgresql://localhost:5432/leetkob (PostgreSQL 16.x)
Started LeetKobApplication in ... seconds
```

Nhấn **`Ctrl + C`** trong terminal để dừng server.

### Các biến môi trường có thể tùy chỉnh (tất cả đều có giá trị mặc định trừ `DEMO_AUTH_TOKEN`)

| Biến môi trường | Giá trị mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `DEMO_AUTH_TOKEN` | *(Bắt buộc, ≥ 32 ký tự)* | Token demo cho endpoint publish PA#2 |
| `SERVER_HOST` | `127.0.0.1` | Địa chỉ lắng nghe HTTP |
| `SERVER_PORT` | `8080` | Cổng HTTP của Backend |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/leetkob` | Chuỗi kết nối JDBC PostgreSQL |
| `DB_USERNAME` | `postgres` | Tài khoản PostgreSQL |
| `DB_PASSWORD` | `postgres` | Mật khẩu PostgreSQL |

---

## 5. Bước 3 — Gọi thử API để kiểm tra

Giữ nguyên cửa sổ Backend đang chạy, mở **một cửa sổ PowerShell mới** và chạy thử:

### 5.1. Tạo tài khoản mới (`POST /api/auth/signup`)
```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://127.0.0.1:8080/api/auth/signup" `
  -ContentType "application/json" `
  -SessionVariable session `
  -Body '{"email":"testuser@hcmus.edu.vn","password":"password123","fullName":"Nguyen Van A"}' | ConvertTo-Json -Depth 5
```
Thành công sẽ trả về HTTP `201 Created`, tự lưu JWT cookie `access_token` vào `$session` và tạo dòng mới trong bảng `public.users`.

### 5.2. Xem thông tin user đang đăng nhập (`GET /api/auth/me`)
```powershell
Invoke-RestMethod -Method Get `
  -Uri "http://127.0.0.1:8080/api/auth/me" `
  -WebSession $session | ConvertTo-Json -Depth 5
```

### 5.3. Đăng nhập lại (`POST /api/auth/login`)
```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://127.0.0.1:8080/api/auth/login" `
  -ContentType "application/json" `
  -SessionVariable session `
  -Body '{"email":"testuser@hcmus.edu.vn","password":"password123"}' | ConvertTo-Json -Depth 5
```

---

## 6. Chạy Test & Kiểm tra Chất lượng Code trước khi mở Pull Request

Nhánh `main` có **5 cổng CI bắt buộc** (`lint`, `format-check`, `typecheck`, `test`, `secrets`). Trước khi `git push`, hãy chạy từ **thư mục gốc dự án**:

```powershell
npm run format        # Tự động format toàn bộ code Java theo Google Java Format
npm run lint          # Kiểm tra quy tắc Checkstyle (cấm import sao .*, bỏ ngoặc nhọn...)
npm run format:check  # Kiểm tra định dạng (read-only, giống hệt trên CI)
npm run typecheck     # Biên dịch toàn bộ main + test
npm test              # Chạy toàn bộ Unit Tests & Contract Tests (dùng H2, không cần Docker)
npm run sec:scan      # Quét lộ secret trong code và lịch sử Git
```

> **Mẹo:** Khi chạy `npm test`, Spring Boot tự nạp cấu hình H2 trong `backend/src/test/resources/config/application.properties` nên bạn không cần bật Docker và không ảnh hưởng dữ liệu trong PostgreSQL thật.

---

## 7. Các lỗi thường gặp ở người mới & Cách xử lý nhanh

| Lỗi gặp phải | Nguyên nhân | Cách khắc phục |
| :--- | :--- | :--- |
| App dừng ngay lúc khởi động, báo lỗi `DEMO_AUTH_TOKEN` | Chưa đặt biến `DEMO_AUTH_TOKEN` hoặc ngắn hơn 32 ký tự | Chạy `$env:DEMO_AUTH_TOKEN = "demo-local-token-0123456789abcdef"` trước khi `bootRun` |
| `Connection to localhost:5432 refused` | Chưa mở Docker Desktop hoặc chưa bật container PostgreSQL | Mở Docker Desktop, sau đó chạy `docker compose up -d` |
| `Web server failed to start. Port 8080 was already in use` | Đang có một tiến trình khác chiếm cổng `8080` | Tắt terminal cũ đang chạy backend, hoặc đổi cổng bằng `$env:SERVER_PORT = "8081"` |
| `The token '&&' is not a valid statement separator` | Windows PowerShell 5.1 không hỗ trợ toán tử `&&` | Chạy từng lệnh trên mỗi dòng riêng, hoặc dùng dấu `;` thay cho `&&` |
| `lint` báo `[AvoidStarImport]` | Code có dòng `import xxx.*;` (Checkstyle cấm) | Đổi thành import từng class cụ thể (trong IntelliJ: bật *Use single class import*) |
| `format:check` báo đỏ | Code chưa đúng chuẩn Google Java Format (2 spaces) | Chạy `npm run format` ở thư mục gốc để Spotless tự căn chỉnh lại |
