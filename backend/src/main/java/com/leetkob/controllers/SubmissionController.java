package com.leetkob.controllers;

import com.leetkob.dtos.submission.JudgeStatusSseEvent;
import com.leetkob.dtos.submission.SubmissionDetail;
import com.leetkob.dtos.submission.SubmissionSummary;
import com.leetkob.dtos.submission.SubmitRequest;
import com.leetkob.security.CustomUserDetails;
import com.leetkob.security.V1AuthHelper;
import com.leetkob.services.SubmissionEventBroadcaster;
import com.leetkob.services.SubmissionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class SubmissionController {

  private final SubmissionService submissionService;
  private final SubmissionEventBroadcaster eventBroadcaster;
  private final V1AuthHelper authHelper;

  /**
   * Sinh viên nộp bài cho lab. Trả về mã 201 Created và thông tin submission với status 'queued'.
   */
  @PostMapping("/labs/{labId}/submissions")
  public ResponseEntity<SubmissionSummary> submitLab(
      @PathVariable("labId") int labId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestHeader(value = "traceparent", required = false) String traceParent,
      @Valid @RequestBody SubmitRequest request) {

    CustomUserDetails user = authHelper.requireRole("student");

    SubmissionSummary summary =
        submissionService.submit(
            labId, user.getId(), request.getSourceCode(), idempotencyKey, traceParent);

    return ResponseEntity.status(HttpStatus.CREATED).body(summary);
  }

  /** Xem danh sách bài nộp của lab. */
  @GetMapping("/labs/{labId}/submissions")
  public ResponseEntity<List<SubmissionSummary>> getLabSubmissions(
      @PathVariable("labId") int labId) {

    CustomUserDetails user = authHelper.requireRole("student", "ta", "admin");
    List<SubmissionSummary> list =
        submissionService.getLabSubmissions(labId, user.getId(), user.getRole());

    return ResponseEntity.ok(list);
  }

  /** Xem chi tiết 1 bài nộp theo ID. */
  @GetMapping("/submissions/{id}")
  public ResponseEntity<SubmissionDetail> getSubmissionDetail(@PathVariable("id") int id) {

    CustomUserDetails user = authHelper.requireRole("student", "ta", "admin");
    SubmissionDetail detail =
        submissionService.getSubmissionDetail(id, user.getId(), user.getRole());

    return ResponseEntity.ok(detail);
  }

  /** Mở luồng SSE real-time theo dõi trạng thái chấm bài của 1 bài nộp. */
  @GetMapping(value = "/submissions/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamSubmissionEvents(@PathVariable("id") int id) {
    CustomUserDetails user = authHelper.requireRole("student", "ta", "admin");
    JudgeStatusSseEvent initialSnapshot =
        submissionService.getSubmissionSseSnapshot(id, user.getId(), user.getRole());
    return eventBroadcaster.subscribe(id, initialSnapshot);
  }
}
