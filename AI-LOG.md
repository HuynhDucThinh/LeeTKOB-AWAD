# Nhật ký Cộng tác với Trợ lý AI — Cột mốc `PA#2` (`AI-LOG.md`)

**Môn học:** CSC13114 — Lập trình Web Nâng cao (HK1 2026–2027)  
**Nhóm thực hiện:**
- **Trà Văn Sỹ** (`23120197`) — Phụ trách Bộ quy tắc Harness (`AGENTS.md`, `CLAUDE.md`), Phân quyền 3 tầng (`docs/permissions.md`, `.claude/settings.json`), Quản lý biến môi trường (`.env.example`) và Quét bí mật lịch sử Git (`secrets`).
- **Huỳnh Đức Thịnh** (`23120199` — Nhóm trưởng) — Phụ trách Kiến trúc phân tầng Backend Spring Boot 3 (Java 21), Đường ống GitHub Actions CI (`.github/workflows/ci.yml`), Cấu hình Branch Protection trên nhánh `main` và Tổng hợp hồ sơ phát hành (`PA2.md`, `SELF_ASSESSMENT_REPORT.md`).
- **Lê Trung Thành Đạt** (`23120228`) — Phụ trách Đặc tả Hợp đồng 5 phần (`docs/contract/publish-lab.md`), Bản đồ độ phủ (`docs/contract/coverage.md`), Hiện thực nghiệp vụ `PublishController`/`PublishService`/`LabPublishRepository` và Bộ 32 bài kiểm thử JUnit 5 (23 Contract Tests + 9 Unit Tests).

**Công cụ AI đã sử dụng theo từng phần việc:**
- **Claude Code (cấu hình qua `AGENTS.md`, `CLAUDE.md`, `.claude/settings.json`):** Hỗ trợ phân tích yêu cầu đề bài, soạn thảo khung hợp đồng RFC 9457 (`docs/contract/publish-lab.md`), khởi tạo mã nguồn Spring Boot (`163c426`), tách kiến trúc phân tầng (`ee426da`) và thực hiện 2 thí nghiệm phá vỡ hợp đồng trên [PR #1](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1) và [PR #2](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2).
- **Antigravity (AI Coding Assistant) — [PR #4](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/4) → [PR #9](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/9):** Rà soát độc lập toàn bộ repository theo rubric `PA#2`, phát hiện 3 lỗi thực tế do bản khởi tạo của trợ lý trước để lọt (`A09`–`A11`), viết Unit Test bắt lỗi trước (tạo CI run đỏ thật trên PR #4), sửa mã nguồn `src/main/`, loại bỏ toàn bộ test hooks khỏi code chính, chuyển khóa đồng bộ từ JVM sang khóa dòng DB (`SELECT … FOR UPDATE`), và chuẩn hóa hồ sơ bằng chứng.

---

## A01 — Xác định Ranh giới Cột mốc `PA#2` trong Phiên Thiết kế Khởi tạo (`163c426`)

- **Observed (Quan sát khi đưa đề bài LeeTKOB cho AI):** Tài liệu đề bài mô tả toàn bộ hệ thống LeeTKOB (giao diện TA/sinh viên, hàng đợi chấm C++ trong sandbox, bộ kiểm tra ràng buộc bằng LLM và công bố điểm). Khi yêu cầu trợ lý AI đề xuất cấu trúc dự án cho `PA#2`, AI có xu hướng tạo sẵn cả khung thư mục cho UI và client gọi mô hình LLM dù chưa dùng tới ở cột mốc này.
- **Changed (Quyết định của nhóm khi tạo commit khởi tạo [`163c426`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/commit/163c426)):** Đối chiếu Mục 7 của đề bài và Rubric `PA#2`, nhóm chỉ đạo trợ lý AI thu gọn dự án về một khối Backend **Spring Boot 3.4.3 (Java 21)** duy nhất, tập trung toàn bộ độ sâu vào ranh giới công bố điểm bài lab (`POST /v1/labs/{labId}/publish`).
- **Rejected (Đã từ chối):** Từ chối đưa thư mục giao diện chưa chấm điểm hoặc tích hợp SDK gọi mô hình LLM ở `PA#2`. Thiết lập chính sách ngân sách mô hình bằng **USD 0** tại `PA#2` trong `README.md` và `docs/permissions.md`.

---

## A02 — Chuẩn hóa 4 Lệnh Kiểm tra Đa nền tảng Không Phụ thuộc Dịch vụ Rời (`163c426`)

- **Observed (Quan sát khi thiết kế cách chạy kiểm tra cục bộ và trên CI):** Để đáp ứng yêu cầu *"Một bản clone mới làm theo README phải chạy xanh"*, nếu bộ kiểm thử yêu cầu người chấm phải cài Maven toàn cục hoặc bật máy chủ PostgreSQL / Docker rời thì sẽ dễ thất bại trên môi trường chấm bài.
- **Changed (Đã áp dụng ngay từ [`163c426`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/commit/163c426)):** Nhúng trực tiếp **Apache Maven Wrapper 3.3.2 (`mvnw` / `mvnw.cmd`)** vào `backend/`, cấu hình H2 Database chạy ở chế độ tương thích PostgreSQL (`MODE=PostgreSQL`) với đầy đủ ràng buộc khóa ngoại và giao dịch SQL thật, đồng thời tạo trình điều phối mỏng `scripts/mvnw.mjs` để 4 lệnh `npm run lint`, `npm run format:check`, `npm run typecheck`, `npm test` hoạt động giống hệt nhau trên Windows, macOS và Linux CI.
- **Rejected (Đã từ chối):** Từ chối dùng cờ `-DskipTests` hoặc `--passWithNoTests` trong cấu hình Maven; ngược lại, bật `<failIfNoTests>true</failIfNoTests>` trong `maven-surefire-plugin` để đảm bảo nếu không có bài test nào chạy thì CI bắt buộc phải báo đỏ.

---

## A03 — Khẳng định Trạng thái Cơ sở Dữ liệu Không Tác dụng Phụ trên Cả 3 Bảng (`163c426`)

- **Observed (Quan sát khi thiết kế hàm kiểm tra `assertNoPublication` cho Contract Tests):** Đối với điều khoản hợp đồng *"Mọi yêu cầu bị từ chối (`4xx`/`5xx`) đều không được để lại tác dụng phụ trong cơ sở dữ liệu"*, nếu bài test chỉ kiểm tra mã trạng thái HTTP hoặc chỉ đếm bảng `publications` thì có thể bỏ lọt lỗi ghi nhầm dữ liệu vào bảng điểm `published_marks` hoặc bảng nhật ký `publication_audit`, và nếu phụ thuộc vào kiểu trả về trung gian của `queryForMap` thì dễ lệch kiểu số nguyên giữa các driver.
- **Changed (Đã áp dụng trong `PublishEndpointContractTest.java`):** Thiết kế hàm trợ giúp `assertNoPublication()` truy vấn trực tiếp giá trị vô hướng `SELECT COUNT(*) FROM <table>` ép kiểu tường minh về `Integer.class` và kiểm tra bằng `0` trên **cả 3 bảng** `publications`, `published_marks` và `publication_audit` sau mọi trường hợp từ chối. Ghi nhận quy tắc này vào Mục 3 của `AGENTS.md` (`AI-LOG A03`).
- **Rejected (Đã từ chối):** Từ chối bỏ qua bước kiểm tra bảng `published_marks` và `publication_audit` sau mỗi lần từ chối (`4xx`/`5xx`).

---

## A04 — Chặn Lỗ hổng Phân tích Cú pháp JSON Dư Ký tự Rác Phía Sau (`163c426`)

- **Observed (Quan sát khi cài đặt kiểm tra JSON cho Điều khoản §2):** Hàm `objectMapper.readTree(text)` mặc định của thư viện Jackson dừng đọc ngay sau dấu đóng `}` đầu tiên, nên chấp nhận cả chuỗi có ký tự rác phía sau như `'{"a":1} trailing'` là hợp lệ thay vì trả về `400 invalid-json`.
- **Changed (Đã áp dụng trong `PublishController.java`):** Bật cờ `DeserializationFeature.FAIL_ON_TRAILING_TOKENS` trên `ObjectMapper` khi đọc request body và kiểm tra tường minh các chuỗi số học phi chuẩn (`NaN`, `Infinity`), kèm bài kiểm thử `malformedOrEmptyJsonReturns400InvalidJson`.
- **Rejected (Đã từ chối):** Từ chối chỉ kiểm tra JSON rỗng hoặc thiếu dấu ngoặc mà bỏ qua trường hợp dư token phía sau JSON hợp lệ.

---

## A05 — Thiết lập Cổng CI 5 Jobs & Quét Bí mật Toàn Lịch sử bằng Gitleaks (`163c426`)

- **Observed (Quan sát khi cấu hình `.github/workflows/ci.yml`):** Mặc định `actions/checkout@v4` chỉ tải 1 commit cuối (`fetch-depth: 1`), khiến công cụ quét bí mật không thể kiểm tra toàn bộ lịch sử Git. Ngoài ra, nếu gộp chung các lệnh kiểm tra vào 1 job duy nhất thì Branch Protection trên GitHub không tách bạch được trạng thái từng cổng kiểm tra trên Pull Request.
- **Changed (Đã áp dụng trong `.github/workflows/ci.yml`):** Kích hoạt workflow trên cả `pull_request`, `push` vào `main` và tag `pa*`; tách thành **5 jobs độc lập** (`lint`, `format-check`, `typecheck`, `test`, `secrets`); đặt `permissions: { contents: read }` và `persist-credentials: false`. Ở job `secrets`, thiết lập `fetch-depth: 0` để tải toàn bộ lịch sử Git, tải bản phát hành chính thức của **Gitleaks 8.30.1** có kiểm tra mã băm `SHA-256` trước khi quét toàn bộ lịch sử (`--log-opts="--all"`).
- **Rejected (Đã từ chối):** Từ chối sử dụng `continue-on-error: true` trên bất kỳ bước nào và từ chối việc chỉ quét cây thư mục hiện tại (`HEAD`) mà bỏ qua lịch sử các commit cũ.

---

## A06 — Thí nghiệm Cố tình Phá vỡ Hợp đồng #1: Sai Mã Trạng thái khi Replay ([PR #1](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1))

- **Observed (Thực nghiệm trên [PR #1](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1)):** Sau khi bật Branch Protection trên nhánh `main` (bắt buộc pass đủ 5 checks và cấm chủ repo bypass), nhóm tạo nhánh `exp/replay-status-break` và đẩy commit [`ffd17cf`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1/commits/ffd17cf29d81a48386057c59d36914e79c8c513f) chỉ sửa `PublishController.java` để khi gọi công bố lặp lại (`result.replayed() == true`) thì trả về mã `201 Created` thay vì `200 OK` theo Điều khoản §4.3 của hợp đồng. Toàn bộ file trong `backend/src/test/` và `docs/contract/` được giữ nguyên không chạm tới.
- **Changed (Kết quả & Khắc phục):** Trên [PR #1](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1) ([Run #42766034224](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766034224)), 4 cổng `lint`, `format-check`, `typecheck` và `secrets` đều báo xanh, nhưng cổng **`test` báo đỏ** tại đúng 2 bài kiểm thử Idempotency (`repeatedPostIs200AndByteIdenticalAcrossColleagueAndClock` và `concurrentRequestsConvergeOnSinglePublicationAndAudit`). GitHub chuyển trạng thái PR sang `Blocked` và từ chối yêu cầu merge của chủ repo với lỗi `HTTP 405: Required status check "test" is failing.` Sau đó, nhóm đẩy commit [`53e8b6d`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1/commits/53e8b6dc2ca1e593547dfe95ac5a84c137109791) chỉ sửa lại mã trạng thái trong `PublishController.java` về `200 OK`; cả 5 cổng chuyển sang xanh tại [Run #42766200407](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766200407) và PR #1 mới được phép merge (`0c35d1b`). Quy tắc cấm thay đổi trạng thái replay trong `AGENTS.md` dẫn chiếu trực tiếp tới thí nghiệm `AI-LOG A06` này.
- **Rejected (Đã từ chối):** Từ chối sửa kỳ vọng trong bài test sang `201` hoặc tắt tạm thời quyền bảo vệ nhánh `main` để ép merge.

---

## A07 — Thí nghiệm Cố tình Phá vỡ Hợp đồng #2: Bỏ Kiểm tra Quyền Sở hữu Lớp của TA ([PR #2](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2))

- **Observed (Thực nghiệm trên [PR #2](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2)):** Để kiểm chứng độc lập quy tắc cô lập dữ liệu giữa các lớp học (Điều khoản §§1, 4), nhóm mở [PR #2](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2) trên nhánh `exp/ownership-break` với commit [`6605bfe`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2/commits/6605bfe5924ac6b79c123b3c1812e19248675a9d) chỉ sửa câu truy vấn SQL trong `LabPublishRepository.findOwnedLab()` (bỏ phép kết `JOIN ta_classes` và điều kiện `AND t.ta_id = ?`, khiến bất kỳ trợ giảng nào đã đăng nhập cũng truy cập được bài lab của lớp khác).
- **Changed (Kết quả & Khắc phục):** Cổng **`test` trên GitHub Actions lập tức báo đỏ** tại [Run #42766403260](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766403260) ở 3 bài kiểm thử quyền sở hữu (`foreignTaAndMissingLabReturnIdentical404BeforeBodyParse`, `publishedReceiptDoesNotBypassRevokedOrForeignOwnership`, và `ownershipIsRecheckedInsideWriteTransaction`), đồng thời chặn yêu cầu merge với `HTTP 405`. Đáng chú ý, ở bài test đầu tiên, khi TA lớp ngoài gửi kèm dữ liệu `text/plain` không hợp lệ, việc thiếu kiểm tra quyền sở hữu ở Bước 3 đã khiến yêu cầu rơi xuống Bước 4 và trả về lỗi `415` thay vì `404` không tiết lộ thông tin. Sau đó, nhóm đẩy commit [`58d8bcb`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2/commits/58d8bcb3c590318c1ea4645651d7a35a8a8621c6) khôi phục `JOIN ta_classes` và `AND t.ta_id = ?` trong `LabPublishRepository.java`, đưa cả 5 cổng về xanh tại [Run #42766615499](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766615499) trước khi merge PR #2 (`2bfc345`). Quy tắc trong `AGENTS.md` dẫn chiếu trực tiếp tới `AI-LOG A07`.
- **Rejected (Đã từ chối):** Từ chối việc chỉ kiểm tra vai trò `role == "ta"` mà bỏ qua bảng phân công lớp `ta_classes`, và từ chối việc đọc/phân tích JSON body trước khi xác nhận quyền sở hữu lớp học.

---

## A08 — Tái Cấu trúc Kiến trúc Phân tầng Spring Boot (`ee426da`)

- **Observed (Quan sát sau commit khởi tạo [`163c426`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/commit/163c426)):** Ở commit đầu tiên `163c426`, mã nguồn gom chung Controller, Service, câu lệnh SQL và DTO vào cùng một package phẳng `vn.edu.hcmus.leetkob.publish`, khiến ranh giới giữa các tầng chưa rõ ràng.
- **Changed (Đã chỉnh sửa tại commit [`ee426da`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/commit/ee426da)):** Tách bạch kiến trúc phân tầng chuẩn Spring Boot (`controller/`, `service/`, `repository/`, `dto/`, `domain/`, `exception/`, `security/`, `config/`), đưa toàn bộ câu lệnh SQL sang `LabPublishRepository` và tách `ProblemResponseWriter` xử lý tập trung phản hồi lỗi RFC 9457.
- **Rejected (Đã từ chối):** Từ chối giữ cấu trúc một package phẳng khi dự án bắt đầu mở rộng sang nhiều lớp nghiệp vụ; đồng thời thống nhất hoàn tất bước phân tầng nền tảng này trước khi chính thức bật khóa bảo vệ nhánh `main` (xem quy trình tại `A12`).

> **Ghi chú về quy trình thực hiện A06/A07:** Theo quy trình thiết kế có chủ đích của nhóm, kịch bản kiểm thử đột biến (mutation test plan) cho `A06` và `A07` được soạn thảo trước trong bước khởi tạo `163c426` (05:46 UTC) để định hình rõ các trường hợp biên cần chứng minh, sau đó được thực thi kiểm chứng thực tế trên [PR #1](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1) (06:04) và [PR #2](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2) (06:07) và ghi nhận mã commit/CI run thực tế tại `646661d`. Kết quả chạy CI thực tế khớp hoàn toàn với kịch bản thiết kế: run [42766034224](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766034224) đỏ đúng 2 test Idempotency, và run [42766403260](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766403260) đỏ đúng 3 test quyền sở hữu lớp. Các mục từ `A09` trở đi ghi nhận quá trình rà soát chéo và cải tiến liên tục qua từng PR.

---

## A09 — Lỗi thật: Xác thực Fail-open trên Máy chủ Chạy thật ([PR #4](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/4))

- **Observed:** Khi rà soát repo theo rubric, phát hiện `AppConfig` đặt mặc định `@Value("${leetkob.auth.mode:test}")`. Chạy `npm start` không cấu hình gì thì máy chủ dùng bảng định danh cứng của bộ test, nên `Authorization: Bearer own` (hoặc `Bearer admin`) xác thực được và **ai cũng publish được lab 1**. README demo cũng hỏng: đặt `DEMO_AUTH_TOKEN` mà không có `leetkob.auth.mode=demo` thì token demo nhận `401`. Ngoài ra `.env.example` liệt kê `SERVER_HOST` và `DATABASE_URL` nhưng không dòng code nào đọc hai biến này. Đây là code do trợ lý AI sinh ra ở `163c426`, và chưa có test nào phủ đường cấu hình production.
- **Changed:** Commit [`13e1428`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/4/commits/13e1428) **chỉ thêm** `AppConfigUnitTest` (3 test): CI đỏ thật tại run [42769824380](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42769824380), và `gh pr merge 4` bị từ chối (`the base branch policy prohibits the merge`). Commit [`f70f2fe`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/4/commits/f70f2fe) xoá mode `test` và `TEST_IDENTITIES` khỏi `src/main`: auth luôn đi qua `DemoAuthAdapter`, thiếu hoặc yếu token thì **không khởi động**. Đồng thời nối `SERVER_HOST`, `SERVER_PORT`, `DATABASE_URL` vào `application.properties`. Run xanh [42769997105](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42769997105), merge `9307283`.
- **Rejected:** Từ chối phương án "giữ mode `test` nhưng đổi mặc định thành `demo`", vì vẫn để lại một công tắc trong code chính có thể bật backdoor. Từ chối sửa contract test hiện có (chúng tự đăng ký `AuthResolver` bằng `@Primary`, không cần map trong `src/main`).

---

## A10 — Lỗi thật: Bước CI Quét Secret Không Bao giờ Fail ([PR #5](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/5))

- **Observed:** `scripts/secret-scan.mjs` (do trợ lý AI viết, chạy trong job `secrets`) bọc phần quét lịch sử trong `try { } catch { }` rỗng. Tái hiện bằng cách chạy script ở thư mục không phải repo git: in `fatal: not a git repository`, rồi vẫn in **`Secret scan passed`** và thoát `0`.
- **Changed:** Commit [`cc3bcb3`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/5/commits/cc3bcb3) trên [PR #5](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/5): bỏ `catch {}`; mọi lỗi `git` → exit `2`; từ chối shallow clone; in số commit đã quét (log CI giờ có con số để đếm). Sau sửa, chạy ngoài repo cho `exit=2`; trong repo cho `0 secrets found across N commits`. Run xanh [42770277333](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42770277333), merge `87001ba`.
- **Rejected:** Từ chối giữ `catch` kèm `console.warn`, vì cảnh báo trong log xanh không chặn được merge.

---

## A11 — Contract §2 (503) Lệch Code, và Hook Test Nằm trong `src/main` ([PR #6](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/6))

- **Observed:** Contract hứa `503 publication-unavailable` khi "cơ sở dữ liệu đang bận khóa ghi", nhưng `PublishService` dùng một `ReentrantLock` của JVM dùng chung cho mọi lab: publish lab A làm lab B nhận 503, còn nhiều instance thì khóa vô nghĩa. `PublishService` còn chứa `setSimulateAuditFailure`, `setBeforeTransactionalAccessHook`, `getWriteLock` và chuỗi `"secret-fixture /private.db"`, tức là test đang kiểm tra những nhánh code chỉ tồn tại vì test. Khi chuyển sang khóa DB, lần chạy đầu test 503 nhận `500`: HikariCP đóng connection sau `SQLTimeoutException`, rollback lỗi nên Spring ném `TransactionSystemException` che mất lỗi gốc.
- **Changed:** Mỗi transaction publish lấy `SELECT … FOR UPDATE` trên dòng `labs` của chính lab đó (`LOCK_TIMEOUT=1000`). Timeout → 503 (dò cả `TransactionSystemException.getApplicationException()`). Gỡ toàn bộ hook khỏi `src/main`. Ba test đổi **cách dựng tình huống** (Mockito spy, constraint SQL thật, connection khác giữ khóa dòng thật) và **không xoá hay nới assertion nào**. Thêm assertion `publications`/`published_marks` vào test thu hồi quyền, thêm test `lockOnOneLabDoesNotBlockPublishingAnotherLab`. Tổng **32 test**. Commit [`d191b83`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/6/commits/d191b83), run xanh [42770818101](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42770818101), merge `32af0a3`.
- **Rejected:** Từ chối sửa contract cho khớp khóa JVM (đó là hạ chuẩn để hợp thức hóa code sai). Từ chối tăng `LOCK_TIMEOUT` lên vài giây chỉ để test chạy "an toàn" hơn, vì contract đã nói rõ 503 kèm `Retry-After: 1`.

---

## A12 — Quy trình Khởi tạo Nền tảng Trước khi Khóa Nhánh `main`

- **Observed:** Trong quá trình thiết lập repository, GitHub yêu cầu các workflow (`lint`, `format-check`, `typecheck`, `test`, `secrets`) phải chạy thành công ít nhất một lần thì mới xuất hiện trong danh sách *Required status checks* của Branch Protection (đúng như lưu ý ở Slide 18 bài giảng *Quality Gates*).
- **Changed:** Nhóm thực hiện quy trình 2 bước: đẩy commit khởi tạo (`163c426`, run [42764268531](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42764268531)) và commit hoàn thiện cấu trúc phân tầng (`ee426da`, run [42765386828](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42765386828)) để kích hoạt đủ 5 jobs trên GitHub Actions và ổn định khung dự án; ngay sau đó khóa nhánh `main` với đủ 5 required checks và `enforce_admins: true`. Kể từ khi bật bảo vệ nhánh (trước PR #1), 100% thay đổi đều đi qua Pull Request (`committer: web-flow`).
- **Rejected:** Từ chối bật Branch Protection khi chưa có đủ 5 status checks hoặc dùng lệnh `git push --force` để gộp lịch sử khởi tạo ban đầu.

## A13 — Quy trình Chuyển đổi Cơ chế Dựng Tình huống Test (Test Harness Refactoring) trên [PR #6](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/6)

- **Observed:** Khi loại bỏ các hàm hook phục vụ test khỏi `PublishService` (`src/main`) trên [PR #6](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/6) để làm sạch mã nguồn chính, các lệnh gọi hook cũ trong `PublishEndpointContractTest.java` không còn tồn tại nên cần chuyển sang cơ chế dựng tình huống bằng tài nguyên thật trong cùng đơn vị thay đổi để đảm bảo mã nguồn biên dịch (`typecheck`) thành công.
- **Changed:** Theo quy trình tái cấu trúc (refactoring) của nhóm trên commit [`d191b83`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/6/commits/d191b83), phần khởi tạo tình huống (test setup) của 3 bài test được chuyển sang dùng Mockito spy, ràng buộc `ALTER TABLE ... ADD CONSTRAINT` thật và kết nối JDBC giữ khóa dòng thật (`SELECT ... FOR UPDATE`). Toàn bộ các câu lệnh kiểm tra kết quả (`assertEquals`, `assertProblem`, `assertNoPublication`) của hợp đồng được giữ nguyên tuyệt đối, không xóa hay nới lỏng bất kỳ điều kiện nào, đồng thời bổ sung thêm bài test cô lập khóa giữa các bài lab (`lockOnOneLabDoesNotBlockPublishingAnotherLab`).
- **Rejected:** Từ chối việc xóa bỏ hoặc hạ thấp bất kỳ câu lệnh `assert` nào của bộ kiểm thử hợp đồng khi thực hiện tái cấu trúc tầng dịch vụ.
