# Hợp đồng Giao tiếp API LeeTKOB — Công bố Điểm Bài Lab (`POST /v1/labs/{labId}/publish`)

> **Trạng thái:** Hợp đồng chính thức Cột mốc `PA#2` (Ngày hiệu lực: 10/11/2026)  
> **Nhóm tác giả:** Trà Văn Sỹ (`23120197`) · Huỳnh Đức Thịnh (`23120199`) · Lê Trung Thành Đạt (`23120228`)  
> **Nguồn tham chiếu nghiệp vụ:** Đề bài dự án lớp (*LeeTKOB — class project brief*, Mục 3 & 4) và [FAQ chính thức của nhóm Product Owners (`FAQ-01`, `FAQ-02`, `FAQ-09`, `FAQ-12`)](https://github.com/tkob-team/LeeTKOB-FAQ/blob/main/FAQ.md).  
> **Cấu trúc tài liệu:** Tuân thủ bố cục 5 phần chuẩn của `CONTRACT.md` (`IA#3`). Mọi bài kiểm thử trong [`PublishEndpointContractTest.java`](../../backend/src/test/java/vn/edu/hcmus/leetkob/controller/PublishEndpointContractTest.java) đều tham chiếu trực tiếp tới số mục của tài liệu này.

---

## 1. Quy ước chung (Conventions)

- **Đường dẫn & Phương thức:** `POST /v1/labs/{labId}/publish`.
  - Tham số đường dẫn `labId` phải là **số nguyên dương thập phân chuẩn (canonical positive integer)** nằm trong khoảng từ `1` đến `2147483647` (`Integer.MAX_VALUE`), không chứa dấu (`+`/`-`), không có chữ số `0` đứng đầu (như `01`), không chứa phần thập phân hay ký hiệu khoa học.
  - Mọi đường dẫn không khớp mẫu chuẩn trên (bao gồm cả đường dẫn dư dấu gạch chéo cuối `/v1/labs/1/publish/`) đều bị coi là đường dẫn không tồn tại và trả về `404 Not Found`.
- **Giao thức & Định dạng:** HTTP, mã hóa UTF-8, định dạng JSON. Mọi mốc thời gian đều ở chuẩn UTC ISO-8601 có độ chính xác đến mili-giây (`yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`). Đồng hồ của máy chủ là nguồn thời gian duy nhất có thẩm quyền.
- **Xác thực (Authentication):** Sử dụng header `Authorization: Bearer <opaque-token>`. Nếu thiếu header, sai cú pháp `Bearer`, hoặc token không khớp với bất kỳ định danh hợp lệ nào, máy chủ trả về `401 Unauthorized` kèm header `WWW-Authenticate: Bearer realm="leetkob"`. Giá trị token không bao giờ được xuất hiện trong nội dung phản hồi hoặc log.
- **Phân quyền & Quy tắc sở hữu lớp (Authorization & Class Ownership — `FAQ-01`):**
  - Người gọi bắt buộc phải có vai trò **`ta` (Trợ giảng)** và đang được phân công phụ trách lớp học sở hữu bài lab `{labId}` (trong bảng `ta_classes`).
  - Nếu người gọi đã xác thực nhưng có vai trò **`student` hoặc `admin`**, máy chủ từ chối với mã **`403 Forbidden`** (quản trị viên quản lý tài khoản/lớp học nhưng không được phép vượt qua cổng duyệt chuyên môn của trợ giảng).
  - Nếu người gọi là một **`ta` hợp lệ nhưng không phụ trách lớp của bài lab đó**, máy chủ trả về **`404 Not Found`** với cấu trúc và nội dung **giống hệt từng chữ** như khi bài lab không tồn tại (chỉ khác trường `instance` chuẩn hóa của chính request đó). Nhờ vậy, một trợ giảng không bao giờ dò biết được bài lab của lớp khác có tồn tại hay không.
- **Header Bảo mật & Tương thích cộng thêm:**
  - Mọi phản hồi (cả `200` lẫn lỗi) đều đính kèm `Cache-Control: no-store` và `X-Content-Type-Options: nosniff`.
  - Request body là một JSON object (`{}`). Các thuộc tính lạ trong JSON object được bỏ qua một cách an toàn (additive compatibility) và tuyệt đối không thể ghi đè danh tính người công bố (`publishedBy`), điểm số hay thời gian công bố.
- **Không áp dụng phân trang (Pagination):** Đây là thao tác thay đổi trạng thái trên một bài lab đơn lẻ và trả về một biên nhận công bố (Publication Receipt) có kích thước cố định, không trả về danh sách sinh viên hay mã nguồn bài nộp.

---

## 2. Danh mục Lỗi chuẩn RFC 9457 (Errors — Problem Details)

Mọi phản hồi không thành công (non-2xx) đều tuân thủ chuẩn **RFC 9457 Problem Details** với `Content-Type: application/problem+json`. Đối tượng JSON luôn chứa đúng 5 trường cơ bản:
- `type`: URI định danh loại lỗi cố định có tiền tố `https://leetkob.example/problems/<slug>`.
- `title`: Tiêu đề ngắn gọn cố định theo từng `type`.
- `status`: Số nguyên khớp chính xác với mã trạng thái HTTP.
- `detail`: Một câu mô tả an toàn, không chứa câu lệnh SQL, đường dẫn file nội bộ, stack trace, token xác thực hay thông tin của lớp khác.
- `instance`: Đường dẫn chuẩn hóa của yêu cầu (không kèm chuỗi truy vấn query string). Đối với các đường dẫn sai khuôn dạng hoặc không xác định, `instance` được cố định là `"/v1"` để không phản xạ (echo) dữ liệu đầu vào chưa tin cậy.
- Trường mở rộng `errors` (mảng các đối tượng `{pointer, detail}` hoặc `{parameter, detail}`) **chỉ xuất hiện duy nhất** ở loại lỗi `validation`.

| Slug (`type`) | HTTP Status | `title` | Điều kiện kích hoạt & Hành động phía Client |
| :--- | :-: | :--- | :--- |
| `invalid-json` | `400` | `Malformed JSON` | Body rỗng hoặc không phải chuỗi JSON hợp lệ. Client cần gửi đối tượng JSON `{}`. |
| `validation` | `400` | `Invalid request` | JSON hợp lệ nhưng không phải kiểu Object (ví dụ `null`, `[]`, `42`, `"text"`, `true`), hoặc URL có chứa bất kỳ query parameter nào. Trường `errors` liệt kê đầy đủ `{pointer: "", detail: "must be a JSON object"}` và từng `{parameter: "<tên>", detail: "query parameters are not supported"}`. |
| `authentication-required` | `401` | `Authentication required` | Thiếu hoặc sai thông tin xác thực `Bearer`. Kèm header `WWW-Authenticate: Bearer realm="leetkob"`. |
| `forbidden` | `403` | `Forbidden` | Đã xác thực nhưng vai trò không phải là `ta` (ví dụ `student` hoặc `admin`). |
| `not-found` | `404` | `Not found` | Đường dẫn không khớp, `labId` không hợp lệ, bài lab không tồn tại, hoặc bài lab thuộc lớp mà TA hiện tại không phụ trách. |
| `method-not-allowed` | `405` | `Method not allowed` | Đường dẫn hợp lệ nhưng gọi bằng phương thức khác `POST` (ví dụ `GET`, `PUT`, `DELETE`). Kèm header `Allow: POST`. |
| `deadline-not-reached` | `409` | `Submission window open` | Bài lab thuộc quyền quản lý nhưng thời gian máy chủ hiện tại chưa tới hạn nộp bài (`now < deadline` — `FAQ-12`). |
| `judging-pending` | `409` | `Judging pending` | Còn ít nhất một bài nộp được tính điểm (`counted_submissions`) đang ở trạng thái chờ hoặc đang chấm (`queued` hoặc `running`). |
| `reviews-pending` | `409` | `Reviews pending` | Còn ít nhất một mục kiểm tra ràng buộc trong hàng đợi (`review_items`) chưa có quyết định duyệt của TA (`decision IS NULL` — Đề bài Mục 3, `FAQ-09`). |
| `payload-too-large` | `413` | `Payload too large` | Kích thước request body vượt quá **4096 bytes** (tính theo số byte UTF-8 thực tế). |
| `unsupported-media-type` | `415` | `Unsupported media type` | Header `Content-Type` không phải `application/json` (chỉ cho phép tham số tùy chọn `charset=utf-8`), hoặc `Content-Encoding` khác `identity`. |
| `publication-unavailable` | `503` | `Publication unavailable` | Cơ sở dữ liệu đang bận khóa ghi hoặc tạm thời không thể mở giao dịch. Kèm header `Retry-After: 1` để client thử lại an toàn. |
| `internal` | `500` | `Internal error` | Lỗi nội bộ bất ngờ trong quá trình xác thực hoặc lưu trữ. Trường `detail` luôn cố định là `"The request could not be completed."` và mọi thay đổi dở dang đều được rollback. |

**Ví dụ phản hồi lỗi chuẩn RFC 9457 (`409 Conflict`):**

```http
HTTP/1.1 409 Conflict
Content-Type: application/problem+json
Cache-Control: no-store
X-Content-Type-Options: nosniff

{
  "type": "https://leetkob.example/problems/reviews-pending",
  "title": "Reviews pending",
  "status": 409,
  "detail": "Required TA reviews are not complete.",
  "instance": "/v1/labs/1/publish"
}
```

**Cam kết không tác dụng phụ (Zero Side-Effects on Failure):** Mọi phản hồi lỗi (`4xx` và `5xx`) đều giữ nguyên tuyệt đối trạng thái của cả 3 bảng đầu ra (`publications`, `published_marks`, `publication_audit`).

---

## 3. Tài nguyên & Mô hình Lưu trữ Giao dịch (Resources)

### Biên nhận công bố điểm (Publication Receipt)

```json
{
  "labId": 1,
  "status": "published",
  "publishedAt": "2026-11-05T08:00:00.000Z",
  "publishedBy": "ta-own",
  "submissionCount": 2
}
```

| Trường | Quy tắc & Ý nghĩa |
| :--- | :--- |
| `labId` | Số nguyên dương định danh bài lab đã yêu cầu. |
| `status` | Chuỗi hằng số cố định `"published"`. |
| `publishedAt` | Thời điểm giao dịch công bố đầu tiên thành công theo đồng hồ máy chủ (UTC ISO-8601 mili-giây); bất biến ở mọi lần gọi lặp lại. |
| `publishedBy` | Mã định danh (`principal.id`) của trợ giảng đầu tiên thực hiện công bố thành công; không bao giờ bị ghi đè bởi người gọi lặp lại sau đó. |
| `submissionCount` | Số nguyên không âm cho biết tổng số bài nộp được tính điểm (`counted_submissions`) của bài lab đã được chụp điểm số trong giao dịch đó. |

**Tính nguyên tử trên 3 bảng dữ liệu:** Một bài lab chỉ có tối đa **1 bản ghi** trong bảng `publications`, bộ bản chụp điểm số tương ứng trong bảng `published_marks` (lưu `score_tenths` từ `0` đến `100`), và đúng **1 bản ghi kiểm toán** mang sự kiện `'lab.published'` trong bảng `publication_audit`. Nếu bất kỳ thao tác ghi nào trong 3 bảng gặp sự cố, toàn bộ giao dịch phải được `ROLLBACK` hoàn toàn.

---

## 4. Hành vi Endpoint, Thứ tự Kiểm tra & Idempotency (Endpoint Behavior)

### 4.1. Mẫu Request hợp lệ

```http
POST /v1/labs/1/publish HTTP/1.1
Authorization: Bearer <opaque-token>
Content-Type: application/json

{}
```

### 4.2. Thứ tự kiểm tra bắt buộc (Ordered Checks)
Để ngăn chặn việc rò rỉ thông tin trạng thái của bài lab thuộc lớp khác thông qua các thông báo lỗi cú pháp, máy chủ thực thi nghiêm ngặt thứ tự 7 bước sau:

1. **Kiểm tra khuôn dạng đường dẫn & phương thức:** Nếu đường dẫn hoặc `labId` không khớp quy tắc số nguyên dương chuẩn ở Mục 1 $\rightarrow$ trả về `404 not-found` (với `instance: "/v1"`). Nếu phương thức HTTP khác `POST` $\rightarrow$ trả về `405 method-not-allowed` (kèm header `Allow: POST`).
2. **Xác thực & Kiểm tra vai trò:** Nếu thiếu hoặc sai `Bearer` token $\rightarrow$ trả về `401 authentication-required` (kèm `WWW-Authenticate`). Nếu vai trò của người gọi khác `ta` (`student` hoặc `admin`) $\rightarrow$ trả về `403 forbidden`.
3. **Kiểm tra tồn tại bài lab & Quyền sở hữu lớp (trước khi đọc body):** Kiểm tra bài lab `{labId}` có tồn tại và TA hiện tại có nằm trong bảng phân công `ta_classes` của lớp đó hay không. Nếu không $\rightarrow$ trả về ngay `404 not-found` mà **không đọc hay phân tích request body**.
4. **Kiểm tra Header & Cú pháp Request Body:**
   - Kiểm tra `Content-Type` phải là `application/json` (cho phép `charset=utf-8` không phân biệt hoa thường) và `Content-Encoding` vắng mặt hoặc bằng `identity`; nếu sai $\rightarrow$ `415 unsupported-media-type`.
   - Kiểm tra độ dài byte UTF-8 của body không vượt quá `4096` bytes; nếu vượt quá $\rightarrow$ `413 payload-too-large`.
   - Phân tích cú pháp JSON; nếu rỗng hoặc lỗi cú pháp $\rightarrow$ `400 invalid-json`.
   - Kiểm tra giá trị JSON phải là một JSON Object (`{...}`) và URL không chứa bất kỳ tham số truy vấn (query parameter) nào; nếu vi phạm $\rightarrow$ `400 validation` kèm danh sách `errors` chi tiết.
5. **Mở giao dịch ghi & Kiểm tra lại quyền sở hữu + Replay:**
   - Bên trong giao dịch SQL, kiểm tra lại quyền sở hữu lớp (`ta_classes`) của TA đối với bài lab (phòng trường hợp quyền vừa bị thu hồi ngay trước khi mở giao dịch).
   - Truy vấn bảng `publications`: nếu bài lab đã được công bố trước đó, kết thúc giao dịch và trả về ngay biên nhận JSON gốc đã lưu kèm header `Idempotent-Replayed: true`.
6. **Kiểm tra 3 điều kiện sẵn sàng công bố (theo đúng thứ tự ưu tiên):**
   - Nếu thời gian máy chủ `now < lab.deadline` $\rightarrow$ trả về `409 deadline-not-reached`.
   - Nếu còn bất kỳ bài nộp nào trong `counted_submissions` có `judge_status IN ('queued', 'running')` $\rightarrow$ trả về `409 judging-pending`.
   - Nếu còn bất kỳ mục nào trong `review_items` của bài lab có `decision IS NULL` $\rightarrow$ trả về `409 reviews-pending`.
7. **Ghi nguyên tử & Phản hồi:**
   - Đếm số bài nộp trong `counted_submissions`, tạo biên nhận JSON, ghi đồng thời vào 3 bảng `publications`, `published_marks`, `publication_audit` và `COMMIT` giao dịch trước khi gửi phản hồi `200 OK`.

### 4.3. Cam kết Idempotency & Xử lý Đồng thời (Concurrency)
- **Lần công bố đầu tiên thành công:** Trả về `200 OK`, `Content-Type: application/json`, body là biên nhận JSON, **không có** header `Idempotent-Replayed`.
- **Các lần gọi lặp lại (Replay) hợp lệ:** Khi chính TA đó hoặc một TA đồng nghiệp cùng phụ trách lớp đó gọi lại `POST /v1/labs/{labId}/publish`, máy chủ trả về `200 OK` kèm header `Idempotent-Replayed: true` và **chuỗi JSON biên nhận giống hệt từng byte** so với lần đầu tiên (`publishedAt` và `publishedBy` giữ nguyên của lần đầu, không sinh thêm bản ghi `publication_audit` hay ghi đè `published_marks`).
- **Không bỏ qua kiểm tra an toàn khi Replay:** Một bài lab dù đã công bố vẫn bắt buộc phải đi qua đầy đủ các bước xác thực (`401`), vai trò (`403`), quyền sở hữu lớp hiện tại (`404` nếu TA đã bị rút khỏi lớp) và kiểm tra định dạng đầu vào (`400`/`413`/`415`).
- **Xử lý yêu cầu đồng thời:** Khi nhiều yêu cầu hợp lệ tới cùng một lúc, khóa giao dịch và ràng buộc khóa chính trên bảng `publications` đảm bảo chỉ duy nhất **1 yêu cầu** thực hiện ghi mới (không có header replay) và tất cả các yêu cầu còn lại hội tụ về cùng một biên nhận dưới dạng replay (`Idempotent-Replayed: true`).

---

## 5. Chính sách Phiên bản & Thay đổi (Versions and Change Policy)

- Trong phạm vi tiền tố `/v1`, chỉ cho phép các thay đổi **tương thích cộng thêm (additive changes)**: bổ sung các trường tùy chọn trong request được bỏ qua, bổ sung trường thông tin mới trong response hoặc thêm endpoint mới.
- Mọi thay đổi phá vỡ tính tương thích (đổi tên hoặc xóa trường trong biên nhận, thay đổi mã trạng thái HTTP, thay đổi quy tắc sở hữu lớp, thay đổi ngữ nghĩa Idempotency hoặc thay đổi URI `type` của lỗi RFC 9457) đều bắt buộc phải chuyển sang phiên bản đường dẫn mới (`/v2/...`) thông qua một Pull Request cập nhật hợp đồng và bộ kiểm thử được xét duyệt độc lập.

