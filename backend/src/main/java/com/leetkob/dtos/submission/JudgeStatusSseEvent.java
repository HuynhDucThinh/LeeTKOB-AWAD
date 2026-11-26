package com.leetkob.dtos.submission;

import java.time.OffsetDateTime;

/**
 * Sự kiện SSE gửi từ Backend sang Frontend mỗi khi trạng thái chấm bài thay đổi (khớp với {@code
 * JudgeStatusSseEvent} ở Frontend).
 */
public record JudgeStatusSseEvent(
    int submissionId,
    int labId,
    String judgeStatus,
    int passedTests,
    int totalTests,
    Integer executionTimeMs,
    Integer memoryKb,
    String compileError,
    int scoreTenths,
    OffsetDateTime judgedAt) {

  /** Kiểm tra xem trạng thái đã là kết quả cuối cùng (không còn thay đổi nữa) hay chưa. */
  public boolean isTerminal() {
    return judgeStatus != null
        && !"queued".equalsIgnoreCase(judgeStatus)
        && !"running".equalsIgnoreCase(judgeStatus);
  }
}
