package com.leetkob.service;

import com.leetkob.dto.submission.JudgeStatusSseEvent;
import com.leetkob.dto.submission.SubmissionDetail;
import com.leetkob.dto.submission.SubmissionSummary;
import java.util.List;
import java.util.UUID;

/** Hợp đồng dịch vụ nộp bài và quản lý bài nộp của sinh viên. */
public interface SubmissionService {

  /** Tạo bài nộp mới và enqueue vào hàng đợi chấm bài (judge_jobs). */
  SubmissionSummary submit(
      int labId, UUID studentId, String sourceCode, String idempotencyKey, String traceParent);

  /**
   * Lấy danh sách tóm tắt các bài nộp của lab. Sinh viên chỉ thấy bài của mình; TA/Admin thấy toàn
   * bộ bài của lab.
   */
  List<SubmissionSummary> getLabSubmissions(int labId, UUID currentUserId, String role);

  /** Lấy chi tiết bài nộp theo ID. */
  SubmissionDetail getSubmissionDetail(int submissionId, UUID currentUserId, String role);

  /** Kiểm tra quyền truy cập và lấy snapshot trạng thái hiện tại để khởi tạo luồng SSE. */
  JudgeStatusSseEvent getSubmissionSseSnapshot(int submissionId, UUID currentUserId, String role);
}
