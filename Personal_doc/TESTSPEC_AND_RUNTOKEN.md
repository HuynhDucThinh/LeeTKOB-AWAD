# Giải thích Thiết kế: `TestCaseSpec`, Batch Execution & `runToken`

Tài liệu tổng hợp toàn bộ lý do kỹ thuật và bảo mật đằng sau việc chuyển đổi `LabTest` $\rightarrow$ `TestCaseSpec` và cơ chế dùng `runToken` khi chạy Docker Sandbox.

---

## 1. Tại sao `JudgeWorker` chuyển `LabTest` sang `TestCaseSpec`?

Trong [`JudgeWorker.java`](file:///d:/ALL_SOURCE_CODE/Learning/AWEB/LastPrj/LeeTKOB-AWAD/backend/src/main/java/com/leetkob/worker/JudgeWorker.java), trước khi gọi Sandbox, Worker map danh sách `LabTest` sang `TestCaseSpec`:

```java
List<LabTest> labTests = labTestRepository.findByLabIdOrderByOrderIndexAsc(lab.getId());
List<TestCaseSpec> testSpecs =
    labTests.stream()
        .map(t -> new TestCaseSpec(
            t.getName(),
            t.getInputData(),
            t.getExpectedOutput(),
            t.getTimeLimitMs(),
            t.getMemoryLimitMb()))
        .toList();
```

### 3 lý do cốt lõi:
1. **Phân tách tầng DB và tầng thực thi (Decoupling):**
   - `LabTest` là JPA Entity gắn chặt với cấu trúc bảng `lab_tests` trong PostgreSQL.
   - `SandboxRunner` ([`DockerSandboxRunner.java`](file:///d:/ALL_SOURCE_CODE/Learning/AWEB/LastPrj/LeeTKOB-AWAD/backend/src/main/java/com/leetkob/sandbox/DockerSandboxRunner.java)) chỉ cần 5 thông số thuần túy của [`TestCaseSpec.java`](file:///d:/ALL_SOURCE_CODE/Learning/AWEB/LastPrj/LeeTKOB-AWAD/backend/src/main/java/com/leetkob/sandbox/TestCaseSpec.java) (`name`, `inputData`, `expectedOutput`, `timeLimitMs`, `memoryLimitMb`) để chạy Docker, không bị phụ thuộc vào thay đổi schema DB sau này.
2. **Tránh lỗi Hibernate Lazy & rò rỉ Session:**
   - `TestCaseSpec` là Java `record` bất biến (immutable) chứa dữ liệu thuần trong RAM. Nạp dứt điểm sang `TestCaseSpec` giúp tránh giữ Hibernate proxy/Session trong suốt quá trình chạy container lâu.
3. **Dễ kiểm thử độc lập (Testability):**
   - Bộ test [`DockerSandboxSecurityTest.java`](file:///d:/ALL_SOURCE_CODE/Learning/AWEB/LastPrj/LeeTKOB-AWAD/backend/src/test/java/com/leetkob/sandbox/DockerSandboxSecurityTest.java) có thể truyền trực tiếp `new TestCaseSpec(...)` vào `DockerSandboxRunner` để test Docker thật mà **không cần khởi tạo DB, JPA hay mock Repository**.

---

## 2. Tại sao phải đánh dấu ranh giới các test mà không "gửi thẳng vào"?

Đây là bài toán đánh đổi (**Trade-off**) giữa **hiệu năng** và **cách tách luồng dữ liệu**:

### Cách 1: Gửi thẳng từng test (1 container / 1 test — Cách cũ)
- Nếu bài lab có **30 test cases**, hệ thống phải gọi `docker run` **30 lần** và biên dịch `g++` **30 lần** cho cùng 1 file `solution.cpp`.
- **Nhược điểm:** Mất 15–30 giây/bài nộp, lãng phí CPU nghiêm trọng khi nhiều sinh viên nộp cùng lúc.

### Cách 2: Gộp chung vào 1 container & biên dịch 1 lần (Cách hiện tại)
- Backend nén `solution.cpp` + `test_0.in ... test_{N-1}.in` vào 1 gói `tar` trong RAM, chạy **1 container duy nhất**, biên dịch `g++` **1 lần duy nhất**, rồi chạy vòng lặp qua từng test case.
- **Vấn đề nảy sinh:** Tất cả output từ container chảy về Backend Java qua **1 đường ống `stdout` duy nhất** (nếu không đánh dấu ranh giới, `8`, `15`, `3000000000` sẽ dính chùm thành `8153000000000`, Java không thể biết đoạn nào của test nào hay test nào bị lỗi runtime).
- **Giải pháp:** Shell script bên trong container chèn thẻ đánh dấu đầu/cuối quanh output của từng test case để Java dễ dàng cắt đúng từng phần và đọc kèm mã thoát (`exitCode`).

---

## 3. Tại sao phải dùng `runToken` ngẫu nhiên thay vì chuỗi cố định `__START_TEST_1_0__`?

### 3.1. Lỗ hổng Output Injection (Output Spoofing) nếu dùng chuỗi cố định
Việc dùng chuỗi cố định **không bypass lớp cách ly OS của Docker**, nhưng **bypass logic đọc kết quả của Backend**.

Giả sử bài toán có Test 0 dễ (input `1` $\rightarrow$ `2`) và Test 1 nặng (input `1000000` $\rightarrow$ `15485863`). Nếu script dùng thẻ cố định `__START_TEST_1_0__` / `__END_TEST_1__`, sinh viên có thể nộp code gian lận:

```cpp
#include <iostream>
int main() {
    int n;
    std::cin >> n;
    if (n == 1) { // Khi đang chạy Test 0
        std::cout << "2\n";
        // Cố tình in lén thẻ giả mạo và đáp án của Test 1:
        std::cout << "__END_TEST_0__\n";
        std::cout << "__START_TEST_1_0__\n";
        std::cout << "15485863\n";
        std::cout << "__END_TEST_1__\n";
        return 0;
    }
    while (true) {} // Test 1 thật sẽ bị TLE nếu chạy tới
}
```
Khi Java tìm chuỗi `__START_TEST_1_0__`, nó đọc nhầm đoạn in lén từ Test 0 và chấm nhầm thành **`AC`**!

### 3.2. Cơ chế chống giả mạo bằng `runToken`
Mỗi lần `compileAndRun(...)` chạy, Backend sinh ra một chuỗi UUID ngẫu nhiên trong RAM:
```java
String runToken = UUID.randomUUID().toString().replace("-", "");
```

Cấu trúc thẻ đánh dấu gồm 4 thành phần:
```text
__START_<runToken>_<i: chỉ số test>_<rc: exit code>__
... (stdout thực tế của test i) ...
__END_<runToken>_<i>__
```

- **Bất khả đoán:** Code C++ của sinh viên chạy trong container không thể biết trước `runToken` của lần chấm đó là gì nên **không thể giả mạo delimiter** bằng `std::cout`.
- **Chính xác:** Hàm `evaluateBatchOutput(...)` trong `DockerSandboxRunner.java` dựa vào đúng cặp `__START_<runToken>_<i>_<rc>__` và `__END_<runToken>_<i>__` để:
  1. Kiểm tra `rc` (`0` = chạy xong bình thường, `152` = `TLE` do `SIGXCPU`, `137` = `MLE` do OOM Killer, khác `0` = `RE`).
  2. Cắt đúng đoạn output nằm giữa cặp thẻ để so sánh với `expectedOutput` của test thứ `i`.
