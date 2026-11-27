package com.leetkob.services.impl;

import com.leetkob.dtos.submission.JudgeStatusSseEvent;
import com.leetkob.dtos.submission.PublishedDeductionItem;
import com.leetkob.dtos.submission.SubmissionDetail;
import com.leetkob.dtos.submission.SubmissionSummary;
import com.leetkob.entities.JudgeJob;
import com.leetkob.entities.Lab;
import com.leetkob.entities.LabConstraint;
import com.leetkob.entities.PublishedMark;
import com.leetkob.entities.ReviewItem;
import com.leetkob.entities.Submission;
import com.leetkob.entities.User;
import com.leetkob.entities.ids.PublishedMarkId;
import com.leetkob.entities.ids.ReviewItemId;
import com.leetkob.exceptions.problem.ProblemException;
import com.leetkob.exceptions.problem.ProblemKind;
import com.leetkob.repositories.JudgeJobRepository;
import com.leetkob.repositories.LabConstraintRepository;
import com.leetkob.repositories.LabRepository;
import com.leetkob.repositories.PublicationRepository;
import com.leetkob.repositories.PublishedMarkRepository;
import com.leetkob.repositories.ReviewItemRepository;
import com.leetkob.repositories.SubmissionRepository;
import com.leetkob.repositories.UserRepository;
import com.leetkob.services.SubmissionService;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cài đặt nghiệp vụ nộp bài, kiểm tra deadline, xử lý Idempotency và truy vấn kết quả bài nộp. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

  private final SubmissionRepository submissionRepository;
  private final JudgeJobRepository judgeJobRepository;
  private final LabRepository labRepository;
  private final UserRepository userRepository;
  private final PublicationRepository publicationRepository;
  private final PublishedMarkRepository publishedMarkRepository;
  private final LabConstraintRepository labConstraintRepository;
  private final ReviewItemRepository reviewItemRepository;

  @Override
  @Transactional
  public SubmissionSummary submit(
      int labId, UUID studentId, String sourceCode, String idempotencyKey, String traceParent) {

    Lab lab =
        labRepository
            .findById(labId)
            .orElseThrow(() -> new ProblemException(ProblemKind.NOT_FOUND));

    // Kiểm tra hạn nộp bài: nếu đã qua deadline, fail-closed với 409 DEADLINE_PASSED
    if (lab.getDeadline() != null && !lab.getDeadline().isBlank()) {
      try {
        Instant deadline = Instant.parse(lab.getDeadline());
        if (Instant.now().isAfter(deadline)) {
          throw new ProblemException(ProblemKind.DEADLINE_PASSED);
        }
      } catch (DateTimeParseException ignored) {
      }
    }

    User student =
        userRepository
            .findById(studentId)
            .orElseThrow(() -> new ProblemException(ProblemKind.NOT_FOUND));

    // Xử lý Idempotency-Key
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      Optional<Submission> existing =
          submissionRepository.findByLabIdAndStudentIdAndIdempotencyKey(
              labId, studentId, idempotencyKey);
      if (existing.isPresent()) {
        log.info(
            "Idempotency hit for labId={}, studentId={}, key={}. Returning existing submission id={}",
            labId,
            studentId,
            idempotencyKey,
            existing.get().getId());
        return toSummary(existing.get(), isLabPublished(labId));
      }
    }

    // Tạo mã trace ngẫu nhiên
    String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

    Submission submission =
        Submission.builder()
            .lab(lab)
            .student(student)
            .sourceCode(sourceCode)
            .judgeStatus("queued")
            .scoreTenths(0)
            .passedTests(0)
            .totalTests(0)
            .traceId(traceId)
            .idempotencyKey(
                (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey : null)
            .build();

    submission = submissionRepository.save(submission);

    // Enqueue vào bảng judge_jobs
    JudgeJob job =
        JudgeJob.builder().submission(submission).status("queued").traceParent(traceParent).build();

    judgeJobRepository.save(job);

    log.info(
        "Enqueued new submission id={} for labId={}, studentId={}, traceId={}",
        submission.getId(),
        labId,
        studentId,
        traceId);

    return toSummary(submission, false);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SubmissionSummary> getLabSubmissions(int labId, UUID currentUserId, String role) {
    boolean published = isLabPublished(labId);

    List<Submission> submissions;
    if ("student".equalsIgnoreCase(role)) {
      // Sinh viên chỉ thấy các bài nộp của chính mình
      submissions =
          submissionRepository.findByLabIdAndStudentIdOrderBySubmittedAtDesc(labId, currentUserId);
    } else {
      // TA/Admin thấy toàn bộ bài nộp của lab
      submissions = submissionRepository.findByLabIdOrderBySubmittedAtDesc(labId);
    }

    return submissions.stream().map(s -> toSummary(s, published)).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public SubmissionDetail getSubmissionDetail(int submissionId, UUID currentUserId, String role) {

    Submission sub =
        submissionRepository
            .findById(submissionId)
            .orElseThrow(() -> new ProblemException(ProblemKind.NOT_FOUND));

    // Sinh viên chỉ được xem bài nộp của chính mình
    if ("student".equalsIgnoreCase(role) && !sub.getStudent().getId().equals(currentUserId)) {
      throw new ProblemException(ProblemKind.NOT_FOUND);
    }

    boolean published = isLabPublished(sub.getLab().getId());

    List<PublishedDeductionItem> deductions = null;
    if (published) {
      // Khi đã Publish: nạp danh sách khấu trừ điểm vi phạm đã được TA xác nhận
      List<LabConstraint> constraints = labConstraintRepository.findByLabId(sub.getLab().getId());
      deductions = new ArrayList<>();
      for (LabConstraint c : constraints) {
        // Tìm review item tương ứng
        Optional<ReviewItem> rOpt =
            reviewItemRepository.findById(
                ReviewItemId.builder()
                    .submissionId(sub.getId())
                    .constraintId(c.getId().getConstraintId())
                    .build());
        if (rOpt.isPresent()) {
          ReviewItem r = rOpt.get();
          if ("violation".equalsIgnoreCase(r.getDecision())) {
            deductions.add(
                PublishedDeductionItem.builder()
                    .constraintId(c.getId().getConstraintId())
                    .verdict(r.getDecision())
                    .deductionTenths(c.getDeductionTenths())
                    .description(c.getDescription())
                    .build());
          }
        }
      }
    }

    Integer publishedScore =
        published
            ? resolvePublishedScore(sub.getLab().getId(), sub.getId(), sub.getScoreTenths())
            : null;

    return SubmissionDetail.builder()
        .id(sub.getId())
        .labId(sub.getLab().getId())
        .studentId(sub.getStudent().getId())
        .studentName(
            sub.getStudent().getFullName() != null
                ? sub.getStudent().getFullName()
                : sub.getStudent().getEmail())
        .judgeStatus(sub.getJudgeStatus())
        .scoreTenths(sub.getScoreTenths())
        .publishedScoreTenths(publishedScore)
        .isCounted(true)
        .passedTests(sub.getPassedTests())
        .totalTests(sub.getTotalTests())
        .executionTimeMs(sub.getExecutionTimeMs())
        .memoryKb(sub.getMemoryKb())
        .compileError(sub.getCompileError())
        .submittedAt(sub.getSubmittedAt())
        .judgedAt(sub.getJudgedAt())
        .traceId(sub.getTraceId())
        .sourceCode(sub.getSourceCode())
        .strippedCode(sub.getStrippedCode())
        .publishedDeductions(deductions)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public JudgeStatusSseEvent getSubmissionSseSnapshot(
      int submissionId, UUID currentUserId, String role) {
    Submission sub =
        submissionRepository
            .findById(submissionId)
            .orElseThrow(() -> new ProblemException(ProblemKind.NOT_FOUND));

    // Sinh viên chỉ được theo dõi bài nộp của chính mình (trả 404 nếu xem bài người khác)
    if ("student".equalsIgnoreCase(role) && !sub.getStudent().getId().equals(currentUserId)) {
      throw new ProblemException(ProblemKind.NOT_FOUND);
    }

    return new JudgeStatusSseEvent(
        sub.getId(),
        sub.getLab().getId(),
        sub.getJudgeStatus(),
        sub.getPassedTests(),
        sub.getTotalTests(),
        sub.getExecutionTimeMs(),
        sub.getMemoryKb(),
        sub.getCompileError(),
        sub.getScoreTenths(),
        sub.getJudgedAt());
  }

  private boolean isLabPublished(int labId) {
    return publicationRepository.existsById(labId);
  }

  private Integer resolvePublishedScore(int labId, int submissionId, Integer fallbackScore) {
    return publishedMarkRepository
        .findById(PublishedMarkId.builder().labId(labId).submissionId(submissionId).build())
        .map(PublishedMark::getScoreTenths)
        .orElse(fallbackScore);
  }

  private SubmissionSummary toSummary(Submission s, boolean isPublished) {
    Integer publishedScore =
        isPublished
            ? resolvePublishedScore(s.getLab().getId(), s.getId(), s.getScoreTenths())
            : null;
    return SubmissionSummary.builder()
        .id(s.getId())
        .labId(s.getLab().getId())
        .studentId(s.getStudent().getId())
        .studentName(
            s.getStudent().getFullName() != null
                ? s.getStudent().getFullName()
                : s.getStudent().getEmail())
        .judgeStatus(s.getJudgeStatus())
        .scoreTenths(s.getScoreTenths())
        .publishedScoreTenths(publishedScore)
        .isCounted(true)
        .passedTests(s.getPassedTests())
        .totalTests(s.getTotalTests())
        .executionTimeMs(s.getExecutionTimeMs())
        .memoryKb(s.getMemoryKb())
        .compileError(s.getCompileError())
        .submittedAt(s.getSubmittedAt())
        .judgedAt(s.getJudgedAt())
        .traceId(s.getTraceId())
        .build();
  }
}
