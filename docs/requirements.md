# Phân tích Yêu cầu và Quyết định Thiết kế Cột mốc `PA#2` (`docs/requirements.md`)

**Nhóm thực hiện:** Trà Văn Sỹ (`23120197`) · Huỳnh Đức Thịnh (`23120199`) · Lê Trung Thành Đạt (`23120228`)

---

## 1. Thứ tự ưu tiên của các nguồn đặc tả (Source Hierarchy)

1. **Rubric chấm điểm `PA#2` của giảng viên:** Quy định 7 tiêu chí chấm điểm (100 điểm) và các bằng chứng bắt buộc phải nộp.
2. **Đề bài tổng thể dự án lớp (`LeeTKOB — class project brief`):** Quy định bài toán nghiệp vụ, các ngưỡng an toàn bắt buộc và lộ trình từ `PA#2` đến `PA#6`.
3. **FAQ chính thức của nhóm Product Owners (`FAQ-01`, `FAQ-02`, `FAQ-09`, `FAQ-12`):** Làm rõ các tình huống nghiệp vụ mà đề bài tổng thể để mở.
4. **Hợp đồng Endpoint của nhóm ([`docs/contract/publish-lab.md`](contract/publish-lab.md)):** Đưa ra các cam kết kỹ thuật tường minh ở cấp độ giao thức HTTP và giao dịch SQL.

---

## 2. Các câu hỏi thiết kế được giải quyết trước khi cài đặt

| Câu hỏi thiết kế | Nguồn tham chiếu & Quyết định của nhóm |
| :--- | :--- |
| **1. `PA#2` có bắt buộc phải cài đặt Sandbox chấm C++, nối LLM và làm giao diện UI đầy đủ không?** | **Không.** Mục 7 của đề bài LeeTKOB và Rubric `PA#2` xác định rõ `PA#2` chấm bộ khung Harness, cổng CI chặn merge, đặc tả Contract của 1 endpoint và bộ Contract Tests. Việc chấm C++ trong Sandbox và gọi LLM thuộc về `PA#3`–`PA#4`. |
| **2. Tại sao nhóm chọn Endpoint công bố điểm (`POST /v1/labs/{labId}/publish`)?** | Đây là **Human Gate (Cổng duyệt của con người)** quan trọng nhất trong LeeTKOB: hành động chuyển kết quả chấm từ trạng thái nháp nội bộ sang điểm số chính thức mà sinh viên nhìn thấy và là bước rất khó hoàn tác. |
| **3. Mọi trợ giảng (TA) đã đăng nhập có được công bố bất kỳ bài lab nào không?** | **Không (`FAQ-01`).** Mỗi TA chỉ được phép công bố điểm cho bài lab thuộc lớp mà TA đó đang được phân công phụ trách (`ta_classes`). Quyền sở hữu lớp phải được kiểm tra lại ở mọi lần gọi, kể cả khi gọi lặp lại (replay). |
| **4. Vai trò `admin` hay `student` gọi vào endpoint này thì xử lý thế nào?** | Hợp đồng dành riêng hành động công bố điểm cho `ta` phụ trách lớp đó. Nếu người gọi có vai trò `student` hoặc `admin`, trả về `403 Forbidden` (`admin` quản trị tài khoản/lớp học nhưng không được đi đường tắt bỏ qua bước duyệt chuyên môn của TA). |
| **5. Nếu một TA gọi vào bài lab của lớp khác (hoặc bài lab không tồn tại) thì trả về `403` hay `404`?** | Trả về **`404 Not Found`** với nội dung hoàn toàn giống hệt như khi bài lab không tồn tại (chỉ khác đường dẫn `instance` chuẩn hóa). Kiểm tra này phải diễn ra **trước khi đọc và parse JSON body** để TA lớp ngoài không thể gửi JSON sai nhằm dò tìm xem bài lab của lớp khác có tồn tại hay không. |
| **6. Khi nào một bài lab được phép công bố điểm?** | Theo Đề bài Mục 3 và `FAQ-12`: (1) Thời gian máy chủ đã đến hoặc qua hạn nộp bài (`now >= deadline`), (2) Không còn bài nộp hợp lệ nào đang chờ chấm trong hàng đợi (`queued` hoặc `running`), và (3) Mọi mục cần con người duyệt trong hàng đợi (`review_items`, bao gồm cả `violation`, `unsure` và mẫu `pass` 10%) đều đã có quyết định của TA (`decision IS NOT NULL`). |
| **7. Cơ chế chống trùng lặp (Idempotency) khi mạng lỗi hoặc bấm đúp hoạt động ra sao?** | Sử dụng **Idempotency tự nhiên theo `labId`** được lưu trữ bền vững trong bảng `publications`. Lần công bố đầu tiên thành công trả về `200 OK` (không kèm header replay). Mọi lần gọi hợp lệ tiếp theo của TA phụ trách lớp đều trả về `200 OK` kèm header `Idempotent-Replayed: true` và **giữ nguyên từng byte JSON biên nhận ban đầu** (không tạo thêm bản ghi kiểm toán hay cập nhật lại mốc thời gian `publishedAt`). |
| **8. Mô hình LLM nào và hạn mức chi tiêu (Spending Cap) nào áp dụng ở `PA#2`?** | Chưa kết nối mô hình nào ở `PA#2` (`none`). Không có khóa API mô hình trong dự án; hạn mức chi tiêu bằng **USD 0 / bài lab và USD 0 tổng cộng**. Tuyệt đối cấm gọi mô hình thật trong các bài kiểm thử. |

---

## 3. Bảng ánh xạ 7 Tiêu chí Rubric sang Sản phẩm bàn giao của Nhóm

| # | Tiêu chí Rubric | Điểm tối đa | Bằng chứng trong Repository |
| :-: | :--- | :-: | :--- |
| **1** | **Rules file** | 15 | [`AGENTS.md`](../AGENTS.md) và [`CLAUDE.md`](../CLAUDE.md) ngắn gọn gồm 4 phần (`Stack`, `Commands`, `Conventions`, `Never`), truy vết trực tiếp tới các sự cố `A03`, `A06`, `A07` trong [`AI-LOG.md`](../AI-LOG.md). |
| **2** | **Permissions and secrets** | 10 | [`docs/permissions.md`](permissions.md) và [`.claude/settings.json`](../.claude/settings.json) chia rõ 3 tầng quyền hạn; [`.env.example`](../.env.example) rỗng giá trị; quét lịch sử Git bằng Gitleaks 8.30.1; hạn mức LLM = USD 0 trong [`README.md`](../README.md). |
| **3** | **One command for every check** | 10 | [`package.json`](../package.json), [`README.md`](../README.md) và [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) dùng đúng 4 lệnh: `npm run lint`, `npm run format:check`, `npm run typecheck`, `npm test`. |
| **4** | **CI that blocks a merge** | 20 | Nhánh `main` bật Branch Protection yêu cầu PR, cấm bypass kể cả chủ repo, bắt buộc vượt qua 5 checks (`lint`, `format-check`, `typecheck`, `test`, `secrets`), kèm minh chứng PR bị chặn đỏ rồi sửa thành xanh trong [`PA2.md`](../PA2.md). |
| **5** | **Contract of one LeeTKOB endpoint** | 25 | [`docs/contract/publish-lab.md`](contract/publish-lab.md) gồm 5 mục chuẩn `IA#3`: quy ước, bảng 13 mã lỗi RFC 9457, cấu trúc biên nhận & giao dịch 3 bảng, thứ tự kiểm tra 7 bước & Idempotency, chính sách phiên bản `/v1`. |
| **6** | **Contract tests that bite** | 15 | [`docs/contract/coverage.md`](contract/coverage.md) ánh xạ từng điều khoản sang 24 bài test JUnit 5 trong `backend/src/test/`; kiểm tra 0 tác dụng phụ lên DB sau mọi lỗi; chứng minh bằng 2 thí nghiệm cố tình phá vỡ hợp đồng (Deliberate Break). |
| **7** | **`AI-LOG.md`** | 5 | [`AI-LOG.md`](../AI-LOG.md) ghi nhận chi tiết công cụ AI, phân công nhóm và các mục `Observed / Changed / Rejected` thực chất từ `A01` đến `A08`. |

