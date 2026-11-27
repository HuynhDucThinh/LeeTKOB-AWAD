# Nhật ký Cộng tác với Trợ lý AI — Cột mốc `PA#2` & `PA#3` (`AI-LOG.md`)

**Môn học:** CSC13114 — Lập trình Web Nâng cao (HK1 2026–2027)  
**Nhóm thực hiện (`23120197-23120199-23120228`):**
- **Trà Văn Sỹ** (`23120197` — TV1) — `PA#2`: Bộ quy tắc Harness (`AGENTS.md`, `CLAUDE.md`), Phân quyền 3 tầng, `.env.example`, `secrets`. `PA#3`: Admin/Classes/Labs (`T1`), Review Queue (`H1–H3`), Pass Sampler 10%, Publish & Appeals (`A1–A3`), Triển khai công khai (`A15`, `A16`).
- **Huỳnh Đức Thịnh** (`23120199` — TV3, Nhóm trưởng) — `PA#2`: Kiến trúc phân tầng Spring Boot, CI 5 jobs, Branch Protection, PR #4–#9. `PA#3`: C++ Stripper + Line Map, Syntax-Aware Hard Rules (`D1`), Groq `openai/gpt-oss-120b` + Call Logger, Guardrails `G1–G5`, Checker CLI (`MODEL_OFF=1`) + `checker/Dockerfile`, bộ `eval/` và báo cáo chi phí (`A17`, `A18`, `A19`).
- **Lê Trung Thành Đạt** (`23120228` — TV2) — `PA#2`: Hợp đồng `publish-lab.md`, `coverage.md`, `PublishService` và 32 bài test JUnit 5. `PA#3`: Hàng đợi bền vững `judge_jobs` (`Q1`), `JudgeWorker`, Submissions + SSE, và Docker C++ Sandbox 9 lớp cô lập `M1–M9`.

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


---

# Nhật ký Cộng tác với Trợ lý AI — Cột mốc `PA#3`

## A14 — Chuẩn hóa Giao diện Frontend từ Figma & Bổ sung Đa lớp Học (`feat/frontend-ui`)

- **Công cụ AI:** Figma AI (sinh giao diện từ đặc tả) + Antigravity (chuẩn hóa kiến trúc `frontend/`, tách tầng services/hooks/stores, đồng bộ đa lớp).
- **Observed (Vấn đề phát hiện khi rà soát bản sinh lần đầu):**
  1. Ở bản sinh đầu tiên từ Figma AI, màn hình của TA (`TaLabsPage`) và Student (`StudentLabsPage`) chỉ hiển thị danh sách lab phẳng và chưa có giao diện chọn/lọc theo nhiều lớp học (`ta_classes` / `student_classes`), dễ gây hiểu nhầm mỗi tài khoản chỉ thuộc 1 lớp duy nhất.
  2. Bản xuất từ Figma sử dụng cấu trúc đơn giản chưa đồng bộ hoàn toàn với quy chuẩn bảo mật token trong RAM (`authStore` + `isInitializing` hydration), Axios dual interceptor (`401` `failedQueue`), và chuẩn hóa lỗi **RFC 9457 (`ProblemDetail`)**.
- **Changed (Quyết định chỉnh sửa và chuẩn hóa):**
  - Bổ sung màn hình `MyClassesPage` (`/ta/classes`, `/student/classes`), bộ lọc `classId` trên `TaLabsPage` và `StudentLabsPage`, tự động điền sẵn `classId` khi tạo lab mới, và cập nhật `mockData.ts` cho `ta-own` và `student-1` thuộc nhiều lớp (trong khi `ta-foreign` vẫn cô lập ở lớp 20).
  - Chuẩn hóa toàn bộ `LeeTKOB-AWAD/frontend/` theo **Modular Component-Service Architecture**: dùng Zustand memory-only `authStore`, Axios `apiClient` có sẵn request/response interceptor, tách `services/*Service.ts` viết sẵn lời gọi API thật (comment lại để thành viên khác bật khi backend sẵn sàng) kèm stateful mock để bấm thử toàn bộ luồng.
  - Kiểm chứng biên dịch tĩnh và đóng gói bằng `npm run build` (`tsc -b && vite build`) thành công 0 lỗi, và `npm run sec:scan` đạt 0 secret.
- **Rejected (Đã từ chối):**
  - Từ chối giữ bộ boilerplate Redux/Auth cũ hoặc để `localStorage` lưu access token.
  - Từ chối import trực tiếp `mockData.ts` vào trong các `pages/` hoặc `components/`; mọi truy cập dữ liệu bắt buộc đi qua tầng `services/` và `hooks/`.

---

## A15 — Triển khai Admin, Classes, Labs (`T1`) & Version theo Quy trình Tests-First (`feat/pa3-admin-classes-labs`)

- **Thành viên thực hiện:** Trà Văn Sỹ (`23120197` — TV1) · **Công cụ AI:** Antigravity.
- **Observed (Vấn đề thực tế gặp phải khi kiểm chứng trên nền PR #20 JPA refactor):**
  1. Trong quá trình triển khai sau khi kéo PR #20 (`refactor/jpa-entities-yml`), entity `LlmCallLog` ánh xạ quan hệ `@ManyToOne private Lab lab;` thay vì cột phẳng `labId`. Khi viết truy vấn tổng chi phí `sumCostUsdByLabId` bằng `WHERE l.labId = :labId`, toàn bộ 30 integration/contract tests thất bại ngay lúc khởi tạo Spring Context với `org.hibernate.query.sqm.UnknownPathException`.
  2. Entity `ClassRoom` cố tình giữ `@Id` không có `@GeneratedValue` để `DemoAccountSeeder` chèn tường minh lớp `10` và `20`; nếu thêm `@GeneratedValue(IDENTITY)` vào `ClassRoom` sẽ làm hỏng seeder khi Hibernate từ chối ID gán tay.
  3. Khi cập nhật danh sách thành viên lớp (`PUT /v1/admin/classes/{id}/members`) hoặc cập nhật test/constraints của bài lab (`PUT /v1/labs/{id}`), việc xóa và chèn lại các bản ghi có khóa chính phức hợp (`ClassMemberId`, `TaClassId`, `LabConstraintId`) trong cùng giao dịch JPA yêu cầu `flush()` ngay sau `deleteBy...` để tránh xung đột thứ tự SQL `INSERT` trước `DELETE`.
- **Changed (Quyết định triển khai & khắc phục):**
  - **Commit 1 (`test(backend)`):** Viết trước 7 bài kiểm thử tích hợp trong `AdminClassesAndLabsIntegrationTest` phủ toàn bộ RBAC (`401`/`403` RFC 9457), đồng bộ 2 bảng `class_members` ↔ `ta_classes`, cô lập lớp `T1` (`404 not-found` giống hệt lab không tồn tại cho TA/SV lớp ngoài), lọc `isSample = true` cho sinh viên, và `GET /v1/version`; xác nhận 7/7 test đỏ trước khi viết code chính.
  - **Commit 2 (`feat(backend)`):** Cài đặt `AdminController`, `ClassController`, `LabController`, `VersionController`, `AdminServiceImpl`, `LabServiceImpl` và các JPA repository; sửa JPQL `LlmCallLogRepository` sang `l.lab.id = :labId`, tính `nextId = findMaxId() + 1` trong `AdminServiceImpl.createClass` để tương thích tuyệt đối cả H2 lẫn PostgreSQL mà không sửa `ClassRoom` entity, và gọi `flush()` sau xóa khóa chính phức hợp. Bổ sung xử lý RFC 9457 cho `HttpMessageNotReadableException` (`400 invalid-json`) và `MethodArgumentTypeMismatchException` (`404 not-found`) trên `/v1/**`.
  - **Commit 3 (`feat(frontend)`):** Bật API thật trong `adminService.ts`, `classService.ts`, `labService.ts`, `versionService.ts`, giữ nguyên thư mục `mock/` cho các service của TV2/PR 1B để không làm gãy build song song.
- **Rejected (Đã từ chối):**
  - Từ chối sửa `ClassRoom` sang `@GeneratedValue(IDENTITY)` vì sẽ xung đột với `DemoAccountSeeder.ensureClass(10/20, ...)`.
  - Từ chối trả `403` khi TA lớp khác truy cập `GET/PUT /v1/labs/{id}` hoặc `POST /v1/labs` cho lớp ngoài — bắt buộc trả `404 not-found` cùng cấu trúc RFC 9457 như tài nguyên không tồn tại để chống rò rỉ thông tin (`T1`).
  - Từ chối xóa `frontend/src/services/mock/` ở PR 1A khi `submissionService.ts` (TV2) và `review/publish/appealService.ts` (PR 1B) vẫn đang cần biên dịch độc lập.

---

## A16 — Triển khai Review Queue (`H1–H3`), Pass Sampling 10% & Appeals (`A1–A3`) (`feat/pa3-review-publish-appeals`)

- **Thành viên thực hiện:** Trà Văn Sỹ (`23120197` — TV1) · **Công cụ AI:** Antigravity.
- **Observed (Phân tích rủi ro & ràng buộc thực tế khi nối Review Queue, Publish và Appeals):**
  1. Nếu một bài phúc khảo (`appeal`) đã được TA duyệt `accepted` mà bị gọi lại `PUT /v1/appeals/{id}/decision` lần nữa với `status = "accepted"`, cộng dồn `deductionTenths` lần thứ 2 vào `published_marks` sẽ làm sai lệch điểm công bố của sinh viên.
  2. Khi sinh viên gửi phúc khảo (`POST /v1/submissions/{id}/appeals`) lên bài nộp của người khác, nếu kiểm tra trạng thái Publish (`409 appeal-before-publish`) trước khi kiểm tra quyền sở hữu bài nộp thì sẽ làm lộ thông tin bài nộp của sinh viên khác có tồn tại hay đã được công bố hay chưa.
  3. Tại thời điểm hoàn thành PR 1B, `submissionService.ts` và `useSubmissionStream.ts` của TV2 vẫn đang dùng `mockStore` (chưa merge PR 2A), nên nếu xóa thư mục `frontend/src/services/mock/` ngay sẽ làm gãy `tsc -b` của Frontend.
- **Changed (Quyết định triển khai theo quy trình Tests-first):**
  - **Commit 1 (`test(backend)`):** Viết trước `ReviewQueueAndAppealsIntegrationTest` kiểm chứng đầy đủ `T1` (`404` giống hệt lab không tồn tại cho TA lớp ngoài), `H2–H3` (`Publish` bị chặn `409 reviews-pending` cho đến khi toàn bộ item `in_queue = TRUE` kể cả `sampled_pass` đều có quyết định, trong khi item `in_queue = FALSE` không chặn), và `A1–A3` (`409 appeal-before-publish`, `404` khi appeal bài SV khác, `409 appeal-already-exists`, `accepted` cập nhật nguyên tử `published_marks` và lưu `decidedBy`/`decidedAt`/`decisionReason`). Xác nhận đỏ 2/2 trước khi cài đặt.
  - **Commit 2 (`feat(backend)`):** Cài đặt `ReviewQueueSampler` (hỗ trợ seed cố định kèm `ReviewQueueSamplerUnitTest` xác minh tỷ lệ lấy mẫu ~10% cho `pass` và 100% cho `violation`/`unsure`), `ReviewQueueController`/`ReviewQueueServiceImpl`, `AppealController`/`AppealServiceImpl`. Trong `AppealServiceImpl`, kiểm tra sở hữu `submission` (`404`) trước `publicationRepository.existsById` (`409`), và kiểm tra cờ `wasAlreadyAccepted` để tránh cộng trùng `deductionTenths` khi gọi lặp lại. Giữ nguyên 100% 23 Contract Tests của `PublishEndpointContractTest`.
  - **Commit 3 (`feat(frontend)`):** Bật API thật trong `reviewService.ts`, `publishService.ts`, `appealService.ts` và giữ nguyên chữ ký tham số tùy chọn (`_studentId`) để không phá vỡ các hook/page hiện có.
- **Rejected (Đã từ chối):**
  - Từ chối xóa thư mục `frontend/src/services/mock/` trước khi PR 2A của TV2 nối xong `submissionService.ts` để đảm bảo `frontend/ npm run build` luôn xanh khi làm song song.
  - Từ chối dùng `Math.random()` trực tiếp không thể tiêm seed trong logic lấy mẫu `10% pass`, vì không kiểm thử tất định được trong CI.


---

## A17 — Triển khai Stripper + Line Map, Syntax-Aware Hard Rules (`D1`) & Vá Tích hợp `JudgeWorker` (`feat/pa3-3a-hard-rules-and-worker-fix`)

- **Thành viên thực hiện:** Huỳnh Đức Thịnh (`23120199` — TV3) · **Công cụ AI:** Antigravity.
- **Observed (Các lỗi thiết kế & bug edge-case phát hiện qua tự kiểm toán code):**
  1. **Bug ẩn danh PII nuốt từ khóa C++:** Bản nháp đầu của bộ ẩn danh PII dùng `Pattern.quote(token)` không có biên từ — nếu sinh viên có tên ngắn như `"An"` hoặc `"in"` (`length >= 2`), bộ lọc biến `int main()` thành `int m[REDACTED]()` và `std::scanf` thành `std::sc[REDACTED]f` trước khi đưa sang LLM.
  2. **Lặp code quét chuỗi/comment & bug nested namespace `myalgo::std::sort`:** Bản nháp đầu nhân bản toàn bộ logic quét raw string C++11 (`R"delim(...)delim"`), string literal và comment nối dòng `\` giữa `CppCommentPiiStripper` và `CppLexer` (nguy cơ lệch line map), đồng thời `BanListHardRuleChecker` chỉ kiểm tra `tokens.get(i - 2) == "std"` mà không kiểm tra `i - 3 == "::"` nên phạt oan `myalgo::std::sort`, và thiếu nhánh `::qsort` ở global scope.
  3. **Rủi ro vi phạm Syllabus Rule trong `JudgeWorker` & `SubmissionServiceImpl`:** Sau khi kéo PR #23, phát hiện `JudgeWorker` đang tự động trừ `deduction` vào `submissions.score_tenths` ngay khi checker trả `violation` (chưa qua TA duyệt), gây trừ điểm 2 lần khi TA duyệt ở `ReviewQueueServiceImpl`; đồng thời `JudgeWorker` chưa lưu `llm_call_logs`, chưa gọi `ReviewQueueSampler` (`H1`), và `SubmissionServiceImpl` đọc nhầm `verdict` thay vì `decision` của TA sau Publish.
- **Changed (Quyết định triển khai & khắc phục):**
  - **Tầng Lexical & Stripper (`checker.lexical`, `checker.strip`):** Tách `CppSpanScanner` và `CppSourceNormalizer` dùng chung 100% giữa Stripper và Lexer để bảo toàn ánh xạ dòng `1:1`; thêm biên chữ-số `(?<![A-Za-z0-9])...(?![A-Za-z0-9])` trong `StudentPiiRedactor` để che sạch email/MSSV/tên mà không làm hỏng từ khóa C++, kèm `StripperLineMapUnitTest`.
  - **Tầng Hard Rules (`checker.hard`):** Tách rõ `BanConstraintParser`, `CppLexer`, `CppScopeAnalyzer` và bảng quyết định `QualificationKind` (`STD_NAMESPACE`, `OTHER_NAMESPACE`, `GLOBAL_SCOPE`, `UNQUALIFIED`) trong `BanListHardRuleChecker`; bổ sung `HardRuleTrapsUnitTest` phủ đầy đủ `std::sort`, `using namespace std`, `namespace alias = ::std`, `#define`, `<vector>`, `::qsort`, và mọi bẫy (comment, string, raw string, `<algorithm>` cho `std::swap`, hàm `sort` tự viết, `myalgo::sort`, `myalgo::std::sort`).
  - **Vá tích hợp `JudgeWorker` & Dọn code thừa:** Gỡ bỏ tự trừ điểm trước TA trong `JudgeWorker` (chỉ lưu điểm chức năng), lưu `llm_call_logs`, tính `remainingBudgetUsd` thật, nối `MODEL_OFF` và `ReviewQueueSampler` 10%, sửa `SubmissionServiceImpl` đọc `published_marks` và `decision = "violation"`, kèm `JudgeWorkerIntegrationPolicyUnitTest` và `scripts/test-pr3a-e2e.mjs`. Xóa bỏ `BadRequestException` (dead code) và cụm boilerplate OAuth2/OIDC không thuộc đề bài.
- **Rejected (Đã từ chối):**
  - Từ chối kiểm tra ban-list bằng regex/substring thô trên mã nguồn gốc (vì sẽ phạt oan comment và string literal).
  - Từ chối để `JudgeWorker` tự trừ `deductionTenths` vào điểm bài nộp trước khi TA xác nhận trong Review Queue (vi phạm *Syllabus Rule* của PA#3).
  - Từ chối giữ các giá trị mật khẩu mặc định (`:postgres`) trong `src/main/resources/application.yml` hoặc token mẫu trong `backend/.env.example`.

---

## A18 — Triển khai Groq `LlmClient`, Bộ 5 Guardrails (`G1–G5`) & `DefaultConstraintCheckPipeline` (`feat/pa3-llm-and-guardrails`)

- **Thành viên thực hiện:** Huỳnh Đức Thịnh (`23120199` — TV3) · **Công cụ AI:** Antigravity.
- **Observed (Các ràng buộc kỹ thuật & rủi ro thực tế khi tích hợp LLM thật và Guardrails):**
  1. **Xung đột Spring AI BOM & khả năng chạy độc lập ở CLI:** Dự án đang chạy trên Spring Boot `4.1.1`; việc kéo thêm `spring-ai-openai-spring-boot-starter` gây rủi ro lệch dependency Milestone/Snapshot, đồng thời làm vướng tầng auto-configuration khi muốn chạy trực tiếp `CheckerCliMain` đóng gói JAR nhỏ ở PR 3C hoặc bắt nguyên vẹn chuỗi JSON lỗi cú pháp để kích hoạt guardrail `G1` (`schema_invalid`).
  2. **Rủi ro fail-closed lệch vai trò container (`api` vs `worker`):** Theo kiến trúc phân tách tối thiểu đặc quyền (`docker-compose.prod.yml`), chỉ tiến trình `worker` (hoặc `all`) mới giữ `MODEL_API_KEY`, còn tiến trình `api` và chế độ offline `MODEL_OFF=1` không được phép yêu cầu `MODEL_API_KEY`. Nếu ép kiểm tra `MODEL_API_KEY` mù quáng cho mọi role ngay lúc tạo bean Spring sẽ làm sập container `api` và toàn bộ `@SpringBootTest` (`leetkob.role=api`).
  3. **Rò rỉ ngân sách giữa các constraint trong cùng 1 bài nộp (`G4`), yêu cầu từ khóa `"JSON"` & thay đổi danh mục model thực tế trên Groq:** Nếu một bài nộp có nhiều ràng buộc `semantic` mà chỉ kiểm tra `remainingBudgetUsd` một lần ở đầu hàm `check(...)`, các lời gọi tiếp theo vẫn tiếp tục tiêu tiền vượt `cost_cap_usd` của bài lab. Ngoài ra, khi chạy E2E trực tiếp với Groq, tên model cũ `llama-3.3-70b-versatile` trả về HTTP `404 (invalid_request_error:model_not_found)` (được guardrail `provider_error` hạ an toàn về `unsure` mà không làm sập worker); truy vấn `/openai/v1/models` cho thấy Groq đã chuyển sang `openai/gpt-oss-120b` (reasoning model cần `max_tokens = 2048` để phần suy luận không cắt cụt JSON đầu ra).
- **Changed (Quyết định triển khai & khắc phục):**
  - **Tầng LLM (`checker.llm`):** Cài đặt giao diện `LlmClient` thuần Java, `GroqLlmClient` bằng `java.net.http.HttpClient` chuẩn Java 21 + Jackson (mặc định `openai/gpt-oss-120b`, `max_tokens = 2048`, timeout kết nối/request cứng cho `G3`, trích xuất `provider_request_id` thật theo thứ tự ưu tiên header `x-request-id` → `x_groq.id` → `id`, bóc tách mã lỗi an toàn không bao giờ log API key), `PromptBuilder` (cô lập `<student_code>` chống prompt injection, chỉ nạp code đã strip comment/PII kèm số dòng 1-based và nêu rõ `"JSON"`), `LlmCostCalculator` (tính `cost_usd` chính xác 6 chữ số thập phân từ token thực tế) và `LlmCallLogger` (`call-log.jsonl`).
  - **Tầng Guardrails (`checker.guardrail`) & Pipeline (`DefaultConstraintCheckPipeline`):** Cài đặt `ResponseSchemaValidator` (`G1` bật `FAIL_ON_TRAILING_TOKENS`, bắt buộc khớp duy nhất `constraintId`, từ chối verdict lạ; `G5` bỏ qua hoàn toàn mọi trường `deduction`/`score` do model tự bịa), `EvidenceVerifier` (`G2` bắt buộc `violation` có bằng chứng, đối chiếu số dòng 1-based và nội dung trực tiếp trên `strippedCode` để chặn trích dẫn nhầm dòng hoặc trích comment đã xóa), và `DefaultConstraintCheckPipeline` (ưu tiên `BanListHardRuleChecker` trước với `0` LLM call, tôn trọng `MODEL_OFF`, trừ lùi `runningRemainingBudget` sau từng lời gọi để chặn `G4` ngay giữa bài nộp).
  - **Cấu hình fail-closed theo role, Nối nốt Frontend & Kiểm thử:** Nối `GroqLlmClient` + `DefaultConstraintCheckPipeline` trong `AppConfig` (chỉ fail-closed bắt buộc `MODEL_API_KEY` khi `LEETKOB_ROLE` là `worker`/`all` và `MODEL_OFF=false`), gỡ bỏ `MockConstraintCheckPipeline`, cập nhật fixture `'Do not use \`goto\`.'` trong `JudgeWorkerExactOnceTest` (giữ nguyên 100% assertion), và bổ sung 2 bộ test `GuardrailsFakeModelUnitTest` + `GroqLlmClientUnitTest` + kịch bản `scripts/test-pr3b-e2e.mjs`. Đồng thời phát hiện `frontend/src/services/submissionService.ts` còn chạy `mockStore` từ PR trước nên đã nối thẳng vào `/v1/labs/{id}/submissions`, luồng SSE kèm Bearer JWT (`useSubmissionStream.ts`), hiển thị chi phí lẻ 4 chữ số thập phân (`formatUsd`) và xóa hẳn thư mục `frontend/src/services/mock/`.
- **Rejected (Đã từ chối):**
  - Từ chối thêm dependency nặng `Spring AI` vào `build.gradle.kts` khi chỉ cần gọi duy nhất endpoint `/openai/v1/chat/completions` với kiểm soát chặt raw JSON cho `G1` và CLI độc lập.
  - Từ chối gửi gộp các constraint `hard` ban-list vào LLM (bắt buộc xử lý bằng `BanListHardRuleChecker` với `0` LLM call).
  - Từ chối nới lỏng `ResponseSchemaValidator` để tự bóc tách văn bản rác quanh JSON hoặc chấp nhận `violation` không có dòng bằng chứng khớp mã nguồn đã strip.
- **Ghi chú minh bạch về quy trình commit trên [PR #24](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/24) & [PR #26](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/26) (Criterion 7):** Trong quá trình phát triển phần `checker`, nhóm viết và chạy kiểm chứng cả mã nguồn `src/main` lẫn các bài test JUnit (`StripperLineMapUnitTest`, `HardRuleTrapsUnitTest`, `GuardrailsFakeModelUnitTest`, `GroqLlmClientUnitTest`) trên working tree cục bộ trước khi tách staging thành 3 commit theo cụm logic (`d194455`, `447cf5f`, `e9d8e3f` có thời điểm commit liền nhau). Nhóm ghi nhận trung thực thực tế này trong `SELF_ASSESSMENT_REPORT.md` (tự chấm 3/5 ở Tiêu chí 7) và tuyệt đối không viết lại lịch sử timestamp hay nới lỏng bất kỳ bài test nào.

---

## A19 — Triển khai Checker CLI (`MODEL_OFF=1`), `checker/Dockerfile`, Bộ `eval/` & Lần chạy Groq Thật trên Dev Set (`feat/pa3-checker-cli-and-eval`)

- **Thành viên thực hiện:** Huỳnh Đức Thịnh (`23120199` — TV3) · **Công cụ AI:** Antigravity.
- **Observed (Các vấn đề kỹ thuật thực tế phát hiện khi đối chiếu bộ `awad-leetkob-eval` và chạy kiểm thử):**
  1. **Yêu cầu khớp nguyên văn dòng `.cpp` gốc của `eval.mjs`:** Trong `eval/dev/eval.mjs` (hàm `evidenceHits`), một dự đoán `violation` chỉ được tính là *caught* khi `e.text.trim() === src[e.line - 1].trim()` trên file `.cpp` **gốc** (chưa xóa comment cuối dòng). Nếu giữ nguyên đoạn con (substring) đã xóa comment do LLM trích xuất thì sẽ bị tính `bad-evidence` dù chỉ đúng số dòng.
  2. **Bẫy biến cục bộ trùng tên container chuẩn (`int* array = new int[4];`) và namespace nhiều tầng (`std::ranges::*`):** Khi viết trước bài test `CheckerCliMainUnitTest.multiSegmentNamespacesMemberCallsKeywordsAndVariableTrapsBehaveAccurately`, phát hiện dòng khai báo `int* array = new int[4];` không bị bắt nhầm nhưng các dòng sử dụng tiếp theo (`array[0] = 7;`, `delete[] array;`) lại bị tưởng nhầm là `std::array` khi trong file có `using namespace std;` vì `CppScopeAnalyzer` chưa lưu tên biến cục bộ vào tập che khuất (`shadowed symbols`).
  3. **Giới hạn `--pids-limit=64` của Docker chấm bài & lỗi mạng thoáng qua khi chạy 47 bài liên tiếp:** Trên máy nhiều nhân CPU, OpenJDK 21 mặc định sinh nhiều luồng GC/JIT dễ chạm trần `--pids-limit=64` khi giảng viên chạy `docker run`. Ngoài ra, khi gọi 47 lượt liên tiếp tới Groq `openai/gpt-oss-120b`, có 3 lượt gặp lỗi mạng I/O thoáng qua và 1 lượt chạm ngưỡng timeout 25s (`G3`).
- **Changed (Quyết định triển khai & số liệu chi phí thực tế):**
  - **Dữ liệu `eval/dev/` & `eval/custom-cases/`:** Khóa `eval/dev/** -text` trong `.gitattributes` để bảo toàn tuyệt đối SHA-256 (`eval.mjs`: `c836edd8713521bb...`, `labels.jsonl`: `262cda7b0ef5612e...`); xây dựng bài toán độc lập `power-fast` gồm 6 lời giải C++17 `Accepted` (`s01`–`s06`, phủ đủ 5 tag `trap`, `injection`, `disguised`, `obvious`, `style`, đã biên dịch kiểm chứng bằng `g++ -std=c++17 -O2 -Wall -Wextra -Werror`).
  - **Mở rộng tổng quát Hard Rules & Ánh xạ dòng gốc:** Mở rộng `BanConstraintParser`, `CppScopeAnalyzer`, `BanListHardRuleChecker` hỗ trợ đường dẫn namespace nhiều tầng (`std::ranges::*`, `boost::integer::*`), lời gọi hàm thành viên không tiền tố (`.rbegin()`, `.rend()`), từ khóa vòng lặp (`for`, `while`, `do`, `goto`), và ghi nhớ biến cục bộ kiểu nguyên thủy vào `userDefinedVariableNames`; đồng thời trong `DefaultConstraintCheckPipeline` ánh xạ dòng bằng chứng đã qua `G2` ngược về dòng `.cpp` gốc. Kết quả chạy offline `--network none -e MODEL_OFF=1` đạt **`0` wrongful deductions** và quyết định đúng **`55/55` (`100%`)** cặp ban-list trên `eval/dev/` mà không cần đọc trước bất kỳ file `.cpp` nào của dev set.
  - **Đóng gói `CheckerCliMain` + `checker/Dockerfile` & Chạy Groq `openai/gpt-oss-120b` thật:** Thêm `.dockerignore` chặn `**/.env`, bổ sung mẫu quét khóa Groq (`\bgsk_[A-Za-z0-9]{20,}\b`) vào `scripts/secret-scan.mjs`, cấu hình `-XX:+UseSerialGC -XX:ActiveProcessorCount=2 -XX:MaxRAMPercentage=75.0` cho container non-root `10001:10001`, và bổ sung cơ chế retry có backoff cho cả HTTP `429`/`503` lẫn lỗi mạng `IOException` thoáng qua trong `GroqLlmClient` (riêng `HttpTimeoutException` vẫn ngắt ngay lập tức không retry để bảo toàn trần thời gian `G3`, kiểm chứng qua `GroqLlmClientUnitTest.retriesTransientIoExceptionAndFailsClosedWhenExhausted`). Lần chạy thật trên `eval/dev/` (`eval/runs/pa3-dev-run/`) đạt **`PASS` (`exit code = 0`)**: **`0` wrongful deductions**, **`94.6%` (`35/37`) recall**, **`3.9%` (`4/102`) unsure** (4 ca lỗi mạng/timeout tự hạ an toàn về `unsure`).
  - **Bài toán chi phí cho 1 Lab 80 bài nộp (Groq `openai/gpt-oss-120b`: \$0.15/1M prompt, \$0.60/1M completion):** Toàn bộ 47 bài dev set tốn `43,567` prompt tokens + `11,945` completion tokens = **\$0.013703** cho 43 lời gọi hoàn tất (`0` lời gọi cho 55 cặp hard-rule), tương đương **\$0.000319 / lời gọi semantic**. Do đó 1 lab 80 bài nộp tốn **$80 \times 1 \times \$0.000319 = \$0.025520$** (~2,6 cent USD cho lab 1 hard + 1 semantic) hoặc **\$0.051040** (~5,1 cent USD cho lab 1 hard + 2 semantic). Đồng bộ thêm UI Frontend (`TaLabFormPage`, `TaReviewQueuePage`, `StatusBadge`) để hiển thị rõ nguồn định tuyến Hard Rule / Groq LLM / Guardrail G1–G5.
- **Rejected (Đã từ chối):**
  - Từ chối đọc `labels.jsonl` bên trong `DatasetLoader` hoặc đưa tên bài toán dev set (`sort-merge`, `binary-search`, ...) vào luật cứng hay prompt.
  - Từ chối chạy lại nhiều lần hoặc sửa tay `predictions.jsonl` để xóa 4 ca `unsure` do lỗi mạng/timeout tự nhiên sau khi bổ sung I/O retry — giữ nguyên vẹn bộ kết quả thực tế (`94.6%` recall, `3.9%` unsure, `PASS`) làm bằng chứng trung thực cho cả LLM lẫn Guardrail `G3`/`provider_error`.
- **Ghi chú minh bạch về quy trình commit trên nhánh `feat/pa3-checker-cli-and-eval` (Criterion 7):** Nhánh PR 3C được thực hiện theo từng đợt công việc thực tế theo thời gian (`5df64d0` thêm `eval/dev` + `eval/custom-cases`; `dfc6532` cài đặt mở rộng hard rules + `CheckerCliMain` + `checker/Dockerfile` + `CheckerCliMainUnitTest` trong cùng commit; `29d6a38` lưu kết quả chạy Groq thật `pa3-dev-run` + `test-pr3c-e2e.mjs`; `f956fa7` đồng bộ giao diện Frontend; sau đó bổ sung commit retry I/O + quét khóa `gsk_` và hồ sơ tự đánh giá `PA#3`). Mọi commit đều giữ nguyên dấu thời gian thực lúc thực hiện và không chỉnh sửa lùi về quá khứ.
