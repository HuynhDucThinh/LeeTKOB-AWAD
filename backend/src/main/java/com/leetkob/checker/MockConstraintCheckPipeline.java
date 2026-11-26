package com.leetkob.checker;

import java.util.Collections;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Mock Pipeline dùng tạm thời cho JudgeWorker của Thành viên 2 trong khi đợi Thành viên 3 hoàn
 * thành bộ phân tích cú pháp Hard Rules và LLM Client thật.
 *
 * <p>Cơ chế: Mặc định coi tất cả các constraints được gửi vào là PASS (hợp lệ), không vi phạm, tạo
 * 0 cuộc gọi LLM và 0 chi phí. Khi Thành viên 3 cung cấp bean chính thức mang tên
 * "realConstraintCheckPipeline", Spring Boot sẽ tự động bỏ qua bean này nhờ
 * {@code @ConditionalOnMissingBean}.
 */
@Component
@ConditionalOnMissingBean(name = "realConstraintCheckPipeline")
public class MockConstraintCheckPipeline implements ConstraintCheckPipeline {

  @Override
  public CheckPipelineResult check(CheckRequest request) {
    if (request == null || request.constraints() == null) {
      return new CheckPipelineResult(
          request != null ? request.sourceCode() : "",
          Collections.emptyList(),
          Collections.emptyList());
    }

    // Giả lập: Mặc định tất cả các ràng buộc (constraints) đều đạt chuẩn (PASS)
    List<ConstraintVerdictResult> verdicts =
        request.constraints().stream()
            .map(
                c ->
                    new ConstraintVerdictResult(
                        c.constraintId(), "pass", "hard_rule", Collections.emptyList(), null))
            .toList();

    return new CheckPipelineResult(request.sourceCode(), verdicts, Collections.emptyList());
  }
}
