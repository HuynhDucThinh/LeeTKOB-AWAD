# Nhật ký Cộng tác với Trợ lý AI — Cột mốc `PA#2` (`AI-LOG.md`)

**Môn học:** CSC13114 — Lập trình Web Nâng cao (HK1 2026–2027)  
**Nhóm thực hiện:**
- **Trà Văn Sỹ** (`23120197`) — Phụ trách Bộ quy tắc Harness (`AGENTS.md`, `CLAUDE.md`), Phân quyền 3 tầng (`docs/permissions.md`, `.claude/settings.json`), Quản lý biến môi trường (`.env.example`) và Quét bí mật lịch sử Git (`secrets`).
- **Huỳnh Đức Thịnh** (`23120199` — Nhóm trưởng) — Phụ trách Kiến trúc phân tầng Backend Spring Boot 3 (Java 21), Đường ống GitHub Actions CI (`.github/workflows/ci.yml`), Cấu hình Branch Protection trên nhánh `main` và Tổng hợp hồ sơ phát hành (`PA2.md`, `SELF_ASSESSMENT_REPORT.md`).
- **Lê Trung Thành Đạt** (`23120228`) — Phụ trách Đặc tả Hợp đồng 5 phần (`docs/contract/publish-lab.md`), Bản đồ độ phủ (`docs/contract/coverage.md`), Hiện thực nghiệp vụ `PublishController`/`PublishService`/`LabPublishRepository` và Bộ 28 bài kiểm thử JUnit 5.

**Công cụ AI đã sử dụng:**
- **Claude Code / Cursor (cấu hình qua `AGENTS.md`, `CLAUDE.md`, `.claude/settings.json`):** Hỗ trợ phân tích yêu cầu, soạn thảo khung hợp đồng RFC 9457, viết mã nguồn Spring Boot phân tầng và bộ kiểm thử hợp đồng.
- **GitHub Copilot / Codex:** Hỗ trợ rà soát chéo (Second Reader review) trên các Pull Request kiểm chứng lỗi hợp đồng (Deliberate Break) và kiểm tra cấu hình GitHub Actions.
- **Antigravity (agent, model Claude Opus) — PR #4 → #8:** Chấm thử repo theo rubric `PA#2`, đối chiếu lịch sử git và CI qua GitHub API, phát hiện các lỗi A09–A11; viết test trước (đỏ), sửa `src/main`, viết lại phần setup của 3 contract test, cập nhật tài liệu bằng chứng. Mọi PR đều đi qua 5 required checks; agent mở và merge PR theo yêu cầu trực tiếp của nhóm trưởng, không dùng quyền bypass.

---

## A01 — Phân tích Yêu cầu & Xác định Ranh giới Cột mốc `PA#2` (Cả nhóm)

- **Observed (Quan sát ban đầu):** Khi đưa toàn bộ tài liệu đề bài LeeTKOB cho trợ lý AI với yêu cầu *"khởi tạo dự án LeeTKOB cho PA#2"*, bản đề xuất đầu tiên của AI cố gắng dựng cùng lúc cả khung giao diện Frontend (Next.js), dịch vụ chấm code C++ và client gọi API LLM, làm phát sinh hơn 140 gói thư viện ngoài và yêu cầu khóa API mô hình ngay từ đầu.
- **Changed (Đã chỉnh sửa):** Nhóm đối chiếu kỹ Mục 7 của đề bài lớp và Rubric `PA#2`: cột mốc `PA#2` chỉ đánh giá bộ khung Harness, cổng chất lượng CI và hợp đồng của 1 endpoint. Do đó, nhóm thu gọn kiến trúc về một khối Backend **Spring Boot 3.4 (Java 21)** duy nhất, tập trung toàn bộ độ sâu vào endpoint `POST /v1/labs/{labId}/publish` (cổng duyệt con người trước khi công bố điểm).
- **Rejected (Đã từ chối):** Từ chối đưa thư mục giao diện UI chưa chấm điểm và từ chối tích hợp SDK gọi mô hình LLM ở `PA#2`. Thiết lập chính sách ngân sách mô hình bằng **USD 0** tại `PA#2` trong `README.md` và `docs/permissions.md`.

---

## A02 — Chuẩn hóa 4 Lệnh Kiểm tra Đa nền tảng & Loại bỏ Phụ thuộc Docker Rời (Huỳnh Đức Thịnh)

- **Observed (Quan sát ban đầu):** Bản nháp cấu hình kiểm thử tích hợp do AI gợi ý ban đầu sử dụng Testcontainers để khởi chạy PostgreSQL trong Docker và gọi lệnh `mvn` trực tiếp. Khi chạy thử trên máy Windows của thành viên chưa cài `mvn` toàn cục hoặc khi tắt Docker Desktop, lệnh `test` lập tức thất bại do lỗi môi trường, vi phạm yêu cầu *"Một bản clone mới làm theo README phải chạy xanh"*.
- **Changed (Đã chỉnh sửa):** Nhúng trực tiếp **Apache Maven Wrapper 3.3.2 (`mvnw` / `mvnw.cmd`)** vào `backend/`, cấu hình H2 Database chạy ở chế độ tương thích PostgreSQL (`MODE=PostgreSQL`) với đầy đủ ràng buộc khóa ngoại và giao dịch SQL thật, đồng thời tạo trình điều phối mỏng `scripts/mvnw.mjs` để 4 lệnh `npm run lint`, `npm run format:check`, `npm run typecheck`, `npm test` hoạt động giống hệt nhau trên cả Windows, macOS và Linux CI.
- **Rejected (Đã từ chối):** Từ chối việc dùng cờ `-DskipTests` hoặc `--passWithNoTests` trong cấu hình Maven; ngược lại, bật `<failIfNoTests>true</failIfNoTests>` trong `maven-surefire-plugin` để đảm bảo nếu không có bài test nào chạy thì CI bắt buộc phải báo đỏ.

---

## A03 — Sửa Lỗi Khẳng định Trạng thái Cơ sở Dữ liệu trong Bộ Kiểm thử (Lê Trung Thành Đạt)

- **Observed (Quan sát ban đầu):** Ở lần chạy cục bộ đầu tiên của bộ Contract Tests, các khẳng định về mã trạng thái HTTP (`401`, `403`, `404`, `409`) đều đúng nhưng hàm kiểm tra không tác dụng phụ (`assertNoPublication`) lại báo lỗi giả do bản nháp của AI so sánh trực tiếp đối tượng bản ghi trả về từ `queryForMap` của H2 (vốn khác kiểu chữ hoa/thường của tên cột và kiểu `Long`/`Integer` giữa các phiên bản driver).
- **Changed (Đã chỉnh sửa):** Tách một chỉnh sửa riêng cho hàm trợ giúp kiểm thử `assertNoPublication()`: sử dụng truy vấn giá trị vô hướng `SELECT COUNT(*) FROM <table>` ép kiểu tường minh về `Integer.class` và kiểm tra bằng `0` trên cả 3 bảng `publications`, `published_marks` và `publication_audit`. Ghi nhận quy tắc này vào Mục 3 của `AGENTS.md` (`AI-LOG A03`).
- **Rejected (Đã từ chối):** Từ chối gợi ý của AI về việc bỏ bớt bước kiểm tra bảng `published_marks` và `publication_audit` sau mỗi lần từ chối (`4xx`/`5xx`), vì một lỗi thực thi hoàn toàn có thể ghi nhầm dữ liệu xuống bảng phụ trước khi ném ngoại lệ HTTP.

---

## A04 — Chặn Lỗ hổng Phân tích Cú pháp JSON Dư Ký tự Rác Phía Sau (Lê Trung Thành Đạt)

- **Observed (Quan sát ban đầu):** Khi viết bài kiểm thử `malformedOrEmptyJsonReturns400InvalidJson` cho Điều khoản §2 của hợp đồng, nhóm phát hiện hàm `objectMapper.readTree(text)` mặc định của thư viện Jackson chấp nhận chuỗi `'{"a":1} trailing'` là hợp lệ (vì Jackson dừng đọc ngay sau dấu `}` đầu tiên), khiến bài kiểm thử mong đợi mã `400 invalid-json` bị thất bại (nhận về `200 OK`).
- **Changed (Đã chỉnh sửa):** Bật cờ `DeserializationFeature.FAIL_ON_TRAILING_TOKENS` trên `ObjectMapper` khi đọc request body trong `PublishController.java` và kiểm tra tường minh các chuỗi số học phi chuẩn (`NaN`, `Infinity`).
- **Rejected (Đã từ chối):** Từ chối việc xóa mẫu dữ liệu `'{"a":1} trailing'` khỏi bài test để làm test xanh nhanh, tuân thủ đúng nguyên tắc trong `AGENTS.md`: hợp đồng đúng thì chỉ được sửa code trong `src/main/`, không bao giờ làm yếu bài test.

---

## A05 — Thiết lập Cổng CI 5 Jobs & Quét Bí mật Toàn Lịch sử bằng Gitleaks (Trà Văn Sỹ & Huỳnh Đức Thịnh)

- **Observed (Quan sát ban đầu):** Bản nháp `.github/workflows/ci.yml` đầu tiên do AI sinh ra chỉ chạy khi `push` vào `main`, dùng `fetch-depth: 1` (chỉ tải 1 commit cuối cùng) và gộp chung mọi lệnh vào một bước duy nhất nên trên giao diện Pull Request không tách bạch được trạng thái của từng cổng kiểm tra.
- **Changed (Đã chỉnh sửa):** Thiết kế lại `.github/workflows/ci.yml` kích hoạt trên cả `pull_request`, `push` vào `main` và tag `pa*`; tách thành **5 jobs độc lập** (`lint`, `format-check`, `typecheck`, `test`, `secrets`); đặt `permissions: { contents: read }` và `persist-credentials: false`. Ở job `secrets`, thiết lập `fetch-depth: 0` để tải toàn bộ lịch sử Git, tải bản phát hành chính thức của **Gitleaks 8.30.1** có kiểm tra mã băm `SHA-256` trước khi quét toàn bộ lịch sử (`--log-opts="--all"`).
- **Rejected (Đã từ chối):** Từ chối sử dụng `continue-on-error: true` trên bất kỳ bước nào và từ chối việc chỉ quét cây thư mục hiện tại (`HEAD`) mà bỏ qua lịch sử các commit cũ.

---

## A06 — Thí nghiệm Cố tình Phá vỡ Hợp đồng #1: Sai Mã Trạng thái khi Replay (Huỳnh Đức Thịnh & Lê Trung Thành Đạt)

- **Observed (Thực nghiệm trên [PR #1](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1)):** Sau khi bật Branch Protection trên nhánh `main` (bắt buộc pass đủ 5 checks và cấm chủ repo bypass), nhóm tạo nhánh `exp/replay-status-break` và đẩy commit [`ffd17cf`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1/commits/ffd17cf29d81a48386057c59d36914e79c8c513f) chỉ sửa `PublishController.java` để khi gọi công bố lặp lại (`result.replayed() == true`) thì trả về mã `201 Created` thay vì `200 OK` theo Điều khoản §4.3 của hợp đồng. Toàn bộ file trong `backend/src/test/` và `docs/contract/` được giữ nguyên không chạm tới.
- **Changed (Kết quả & Khắc phục):** Trên [PR #1](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1) ([Run #42766034224](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766034224)), 4 cổng `lint`, `format-check`, `typecheck` và `secrets` đều báo xanh, nhưng cổng **`test` báo đỏ** tại đúng 2 bài kiểm thử Idempotency (`repeatedPostIs200AndByteIdenticalAcrossColleagueAndClock` và `concurrentRequestsConvergeOnSinglePublicationAndAudit`). GitHub chuyển trạng thái PR sang `Blocked` và từ chối yêu cầu merge của chủ repo với lỗi `HTTP 405: Required status check "test" is failing.` Sau đó, nhóm đẩy commit [`53e8b6d`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/1/commits/53e8b6dc2ca1e593547dfe95ac5a84c137109791) chỉ sửa lại mã trạng thái trong `PublishController.java` về `200 OK`; cả 5 cổng chuyển sang xanh tại [Run #42766200407](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766200407) và PR #1 mới được phép merge (`0c35d1b`). Quy tắc cấm thay đổi trạng thái replay trong `AGENTS.md` dẫn chiếu trực tiếp tới thí nghiệm `AI-LOG A06` này.
- **Rejected (Đã từ chối):** Từ chối sửa kỳ vọng trong bài test sang `201` hoặc tắt tạm thời quyền bảo vệ nhánh `main` để ép merge.

---

## A07 — Thí nghiệm Cố tình Phá vỡ Hợp đồng #2: Bỏ Kiểm tra Quyền Sở hữu Lớp của TA (Lê Trung Thành Đạt & Trà Văn Sỹ)

- **Observed (Thực nghiệm trên [PR #2](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2)):** Để kiểm chứng độc lập quy tắc cô lập dữ liệu giữa các lớp học (Điều khoản §§1, 4), nhóm mở [PR #2](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2) trên nhánh `exp/ownership-break` với commit [`6605bfe`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2/commits/6605bfe5924ac6b79c123b3c1812e19248675a9d) chỉ sửa câu truy vấn SQL trong `LabPublishRepository.findOwnedLab()` (bỏ phép kết `JOIN ta_classes` và điều kiện `AND t.ta_id = ?`, khiến bất kỳ trợ giảng nào đã đăng nhập cũng truy cập được bài lab của lớp khác).
- **Changed (Kết quả & Khắc phục):** Cổng **`test` trên GitHub Actions lập tức báo đỏ** tại [Run #42766403260](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766403260) ở 3 bài kiểm thử quyền sở hữu (`foreignTaAndMissingLabReturnIdentical404BeforeBodyParse`, `publishedReceiptDoesNotBypassRevokedOrForeignOwnership`, và `ownershipIsRecheckedInsideWriteTransaction`), đồng thời chặn yêu cầu merge với `HTTP 405`. Đáng chú ý, ở bài test đầu tiên, khi TA lớp ngoài gửi kèm dữ liệu `text/plain` không hợp lệ, việc thiếu kiểm tra quyền sở hữu ở Bước 3 đã khiến yêu cầu rơi xuống Bước 4 và trả về lỗi `415` thay vì `404` không tiết lộ thông tin. Sau đó, nhóm đẩy commit [`58d8bcb`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/2/commits/58d8bcb3c590318c1ea4645651d7a35a8a8621c6) khôi phục `JOIN ta_classes` và `AND t.ta_id = ?` trong `LabPublishRepository.java`, đưa cả 5 cổng về xanh tại [Run #42766615499](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42766615499) trước khi merge PR #2 (`2bfc345`). Quy tắc trong `AGENTS.md` dẫn chiếu trực tiếp tới `AI-LOG A07`.
- **Rejected (Đã từ chối):** Từ chối việc chỉ kiểm tra vai trò `role == "ta"` mà bỏ qua bảng phân công lớp `ta_classes`, và từ chối việc đọc/phân tích JSON body trước khi xác nhận quyền sở hữu lớp học.

---

## A08 — Tái Cấu trúc Kiến trúc Phân tầng Spring Boot & Đóng gói Hồ sơ Phát hành `pa2` (Cả nhóm)

- **Observed (Quan sát khi rà soát kiến trúc):** Ban đầu mã nguồn gom chung Controller, Service, câu lệnh SQL và DTO vào cùng package `publish/`, khiến `PublishService` vừa xử lý nghiệp vụ giao dịch vừa chứa chuỗi SQL thuần và `PublishController` vừa kiểm tra request vừa trực tiếp đóng gói lỗi RFC 9457.
- **Changed (Đã chỉnh sửa):** Tách bạch kiến trúc phân tầng chuẩn Spring Boot (`controller/`, `service/`, `repository/`, `dto/`, `domain/`, `exception/`, `security/`, `config/`), đưa toàn bộ câu lệnh SQL sang `LabPublishRepository` và tách `ProblemResponseWriter` xử lý tập trung phản hồi lỗi RFC 9457. Toàn bộ lịch sử Git, đường dẫn PR bị chặn đỏ, commit sửa xanh và CI của tag `pa2` được kiểm tra và tổng hợp vào `PA2.md` cùng `SELF_ASSESSMENT_REPORT.md`.
- **Rejected (Đã từ chối):** Từ chối lưu trữ bất kỳ giá trị `DEMO_AUTH_TOKEN` thật nào vào trong `.env.example` hoặc mã nguồn cấu hình của dự án.

---

## A09 — Lỗi thật: Xác thực Fail-open trên Máy chủ Chạy thật ([PR #4](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/4))

- **Observed:** Khi rà soát repo theo rubric, phát hiện `AppConfig` đặt mặc định `@Value("${leetkob.auth.mode:test}")`. Chạy `npm start` không cấu hình gì thì máy chủ dùng bảng định danh cứng của bộ test, nên `Authorization: Bearer own` (hoặc `Bearer admin`) xác thực được và **ai cũng publish được lab 1**. README demo cũng hỏng: đặt `DEMO_AUTH_TOKEN` mà không có `leetkob.auth.mode=demo` thì token demo nhận `401`. Ngoài ra `.env.example` liệt kê `SERVER_HOST` và `DATABASE_URL` nhưng không dòng code nào đọc hai biến này. Đây là code do trợ lý AI sinh ra ở `163c426`, và chưa có test nào phủ đường cấu hình production.
- **Changed:** Commit [`13e1428`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/4/commits/13e1428) **chỉ thêm** `AppConfigUnitTest` (3 test): CI đỏ thật tại run [42769824380](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42769824380), và `gh pr merge 4` bị từ chối (`the base branch policy prohibits the merge`). Commit [`f70f2fe`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/4/commits/f70f2fe) xoá mode `test` và `TEST_IDENTITIES` khỏi `src/main`: auth luôn đi qua `DemoAuthAdapter`, thiếu hoặc yếu token thì **không khởi động**. Đồng thời nối `SERVER_HOST`, `SERVER_PORT`, `DATABASE_URL` vào `application.properties`. Run xanh [42769997105](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42769997105), merge `9307283`.
- **Rejected:** Từ chối phương án "giữ mode `test` nhưng đổi mặc định thành `demo`", vì vẫn để lại một công tắc trong code chính có thể bật backdoor. Từ chối sửa contract test hiện có (chúng tự đăng ký `AuthResolver` bằng `@Primary`, không cần map trong `src/main`).

## A10 — Lỗi thật: Bước CI Quét Secret Không Bao giờ Fail ([PR #5](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/5))

- **Observed:** `scripts/secret-scan.mjs` (do trợ lý AI viết, chạy trong job `secrets`) bọc phần quét lịch sử trong `try { } catch { }` rỗng. Tái hiện bằng cách chạy script ở thư mục không phải repo git: in `fatal: not a git repository`, rồi vẫn in **`Secret scan passed`** và thoát `0`.
- **Changed:** Commit [`cc3bcb3`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/5/commits/cc3bcb3) trên [PR #5](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/5): bỏ `catch {}`; mọi lỗi `git` → exit `2`; từ chối shallow clone; in số commit đã quét (log CI giờ có con số để đếm). Sau sửa, chạy ngoài repo cho `exit=2`; trong repo cho `0 secrets found across 12 commits`. Run xanh [42770277333](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42770277333).
- **Rejected:** Từ chối giữ `catch` kèm `console.warn`, vì cảnh báo trong log xanh không chặn được merge.

## A11 — Contract §2 (503) Lệch Code, và Hook Test Nằm trong `src/main` ([PR #6](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/6))

- **Observed:** Contract hứa `503 publication-unavailable` khi "cơ sở dữ liệu đang bận khóa ghi", nhưng `PublishService` dùng một `ReentrantLock` của JVM dùng chung cho mọi lab: publish lab A làm lab B nhận 503, còn nhiều instance thì khóa vô nghĩa. `PublishService` còn chứa `setSimulateAuditFailure`, `setBeforeTransactionalAccessHook`, `getWriteLock` và chuỗi `"secret-fixture /private.db"`, tức là test đang kiểm tra những nhánh code chỉ tồn tại vì test. Khi chuyển sang khóa DB, lần chạy đầu test 503 nhận `500`: HikariCP đóng connection sau `SQLTimeoutException`, rollback lỗi nên Spring ném `TransactionSystemException` che mất lỗi gốc.
- **Changed:** Mỗi transaction publish lấy `SELECT … FOR UPDATE` trên dòng `labs` của chính lab đó (`LOCK_TIMEOUT=1000`). Timeout → 503 (dò cả `TransactionSystemException.getApplicationException()`). Gỡ toàn bộ hook khỏi `src/main`. Ba test đổi **cách dựng tình huống** (Mockito spy, constraint SQL thật, connection khác giữ khóa dòng thật) và **không xoá hay nới assertion nào**. Thêm assertion `publications`/`published_marks` vào test thu hồi quyền, thêm test `lockOnOneLabDoesNotBlockPublishingAnotherLab`. Tổng **32 test**. Commit [`d191b83`](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/pull/6/commits/d191b83), run xanh [42770818101](https://github.com/HuynhDucThinh/LeeTKOB-AWAD/actions/runs/42770818101), merge `32af0a3`.
- **Rejected:** Từ chối sửa contract cho khớp khóa JVM (đó là hạ chuẩn để hợp thức hóa code sai). Từ chối tăng `LOCK_TIMEOUT` lên vài giây chỉ để test chạy "an toàn" hơn, vì contract đã nói rõ 503 kèm `Retry-After: 1`.

