package com.leetkob.sandbox;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Mock Sandbox Runner dùng cho các bài kiểm thử hàng đợi Q1 và môi trường dev khi không bật Docker
 * daemon máy thật.
 *
 * <p>Cơ chế: Phân tích mã nguồn đầu vào thông qua các từ khóa mô phỏng để trả về kết quả tương ứng:
 *
 * <ul>
 *   <li>Chứa {@code syntax_error} hoặc {@code // MOCK_CE}: Lỗi biên dịch (CE).
 *   <li>Chứa {@code // MOCK_TLE}: Vượt quá thời gian chạy (TLE).
 *   <li>Chứa {@code // MOCK_MLE}: Vượt quá giới hạn bộ nhớ (MLE).
 *   <li>Chứa {@code // MOCK_RE}: Lỗi thực thi runtime (RE).
 *   <li>Chứa {@code // MOCK_WA}: Sai đáp án (WA).
 *   <li>Mặc định: Toàn bộ test cases đều đúng (AC).
 * </ul>
 */
@Component
@ConditionalOnMissingBean(name = "dockerSandboxRunner")
public class MockSandboxRunner implements SandboxRunner {

  @Override
  public SandboxRunResult compileAndRun(String sourceCode, List<TestCaseSpec> tests) {
    int total = (tests != null) ? tests.size() : 0;
    String code = (sourceCode != null) ? sourceCode : "";

    // 1. Giả lập lỗi biên dịch (Compile Error)
    if (code.contains("syntax_error") || code.contains("// MOCK_CE")) {
      return new SandboxRunResult(
          "CE", 0, total, null, null, "solution.cpp:1: error: expected ';' before '}' token");
    }

    // 2. Giả lập vượt quá thời gian (Time Limit Exceeded)
    if (code.contains("// MOCK_TLE")) {
      return new SandboxRunResult("TLE", 0, total, 2000, 2048, null);
    }

    // 3. Giả lập vượt quá bộ nhớ (Memory Limit Exceeded)
    if (code.contains("// MOCK_MLE")) {
      return new SandboxRunResult("MLE", 0, total, 350, 262144, null);
    }

    // 4. Giả lập lỗi thực thi (Runtime Error)
    if (code.contains("// MOCK_RE")) {
      return new SandboxRunResult("RE", 0, total, 50, 1024, "Segmentation fault (core dumped)");
    }

    // 5. Giả lập sai đáp án (Wrong Answer)
    if (code.contains("// MOCK_WA")) {
      int passed = Math.max(0, total - 1);
      return new SandboxRunResult("WA", passed, total, 100, 1024, null);
    }

    // 6. Mặc định: Chấp nhận toàn bộ (Accepted)
    return new SandboxRunResult("AC", total, total, 120, 2048, null);
  }
}
