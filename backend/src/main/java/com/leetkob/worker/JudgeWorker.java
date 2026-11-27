package com.leetkob.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetkob.checker.CheckPipelineResult;
import com.leetkob.checker.CheckRequest;
import com.leetkob.checker.ConstraintCheckPipeline;
import com.leetkob.checker.ConstraintSpec;
import com.leetkob.checker.ConstraintVerdictResult;
import com.leetkob.checker.LlmCallRecord;
import com.leetkob.domain.ReviewQueueSampler;
import com.leetkob.dtos.submission.JudgeStatusSseEvent;
import com.leetkob.entities.CountedSubmission;
import com.leetkob.entities.JudgeJob;
import com.leetkob.entities.Lab;
import com.leetkob.entities.LabConstraint;
import com.leetkob.entities.LabTest;
import com.leetkob.entities.LlmCallLog;
import com.leetkob.entities.ReviewItem;
import com.leetkob.entities.Submission;
import com.leetkob.entities.User;
import com.leetkob.entities.ids.ReviewItemId;
import com.leetkob.repositories.CountedSubmissionRepository;
import com.leetkob.repositories.JudgeJobRepository;
import com.leetkob.repositories.LabConstraintRepository;
import com.leetkob.repositories.LabTestRepository;
import com.leetkob.repositories.LlmCallLogRepository;
import com.leetkob.repositories.ReviewItemRepository;
import com.leetkob.repositories.SubmissionRepository;
import com.leetkob.sandbox.SandboxRunResult;
import com.leetkob.sandbox.SandboxRunner;
import com.leetkob.sandbox.TestCaseSpec;
import com.leetkob.services.SubmissionEventBroadcaster;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Trình điều phối chấm bài bất đồng bộ (Separate Judge Worker - Q1 Exact-Once).
 *
 * <p>Quy trình thực thi gồm các bước chính:
 *
 * <ol>
 *   <li>Quét hàng đợi {@code judge_jobs} với cơ chế khóa hàng {@code FOR UPDATE SKIP LOCKED}.
 *   <li>Đổi trạng thái sang {@code running} để chống trùng lặp tiến trình.
 *   <li>Thu thập test cases và chuyển mã nguồn cho {@link SandboxRunner} (C++ Sandbox).
 *   <li>Nếu Sandbox chạy thành công (AC), chuyển mã nguồn cho {@link ConstraintCheckPipeline} để
 *       kiểm tra ràng buộc.
 *   <li>Lưu điểm chức năng (tuyệt đối không tự trừ điểm trước khi TA duyệt — Syllabus Rule), ghi
 *       {@code llm_call_logs}, áp dụng {@link ReviewQueueSampler} (H1) để lưu {@code review_items},
 *       đồng bộ {@code counted_submissions} và đánh dấu {@code judge_jobs} hoàn tất trong cùng 1
 *       giao dịch nguyên tử.
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeWorker {

  private final JudgeJobRepository judgeJobRepository;
  private final SubmissionRepository submissionRepository;
  private final LabTestRepository labTestRepository;
  private final LabConstraintRepository labConstraintRepository;
  private final CountedSubmissionRepository countedSubmissionRepository;
  private final ReviewItemRepository reviewItemRepository;
  private final LlmCallLogRepository llmCallLogRepository;
  private final ReviewQueueSampler reviewQueueSampler;
  private final SandboxRunner sandboxRunner;
  private final ConstraintCheckPipeline constraintCheckPipeline;
  private final SubmissionEventBroadcaster submissionEventBroadcaster;
  private final ObjectMapper objectMapper;
  private final TransactionTemplate transactionTemplate;

  @Value("${leetkob.role:all}")
  private String leetkobRole;

  @Value("${leetkob.checker.model-off:${MODEL_OFF:false}}")
  private String modelOffRaw = "false";

  @Getter @Setter private long pollingIntervalMs = 250;

  @Getter private final AtomicBoolean running = new AtomicBoolean(false);

  private Thread workerLoopThread;

  @PostConstruct
  public void init() {
    if ("worker".equalsIgnoreCase(leetkobRole) || "all".equalsIgnoreCase(leetkobRole)) {
      log.info(
          "JudgeWorker background loop enabled for role={}, sandboxRunner={}",
          leetkobRole,
          sandboxRunner.getClass().getSimpleName());
      startWorkerLoop();
    } else {
      log.info("JudgeWorker background loop disabled for role={} (API-only mode)", leetkobRole);
    }
  }

  @PreDestroy
  public void stop() {
    stopWorkerLoop();
  }

  /** Khởi động vòng lặp Adaptive Polling trong thread ngầm. */
  public synchronized void startWorkerLoop() {
    if (running.compareAndSet(false, true)) {
      workerLoopThread =
          new Thread(
              () -> {
                log.info("JudgeWorker background loop started with role={}", leetkobRole);
                while (running.get()) {
                  try {
                    boolean processed = processNextJob();
                    if (!processed) {
                      Thread.sleep(pollingIntervalMs);
                    }
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                  } catch (Exception ex) {
                    log.error("Unhandled exception in JudgeWorker loop: {}", ex.getMessage(), ex);
                    try {
                      Thread.sleep(pollingIntervalMs);
                    } catch (InterruptedException ie) {
                      Thread.currentThread().interrupt();
                      break;
                    }
                  }
                }
                log.info("JudgeWorker background loop terminated.");
              },
              "judge-worker-loop");
      workerLoopThread.setDaemon(true);
      workerLoopThread.start();
    }
  }

  /** Dừng vòng lặp ngầm của Worker. */
  public synchronized void stopWorkerLoop() {
    if (running.compareAndSet(true, false)) {
      if (workerLoopThread != null) {
        workerLoopThread.interrupt();
        try {
          workerLoopThread.join(2000);
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  /** Dữ liệu job đã khóa để chuyển sang thực thi Sandbox ngoài giao dịch DB. */
  private record ClaimedJob(
      long jobId,
      int submissionId,
      int labId,
      String labStatementMd,
      BigDecimal labCostCapUsd,
      java.util.UUID studentId,
      String studentEmail,
      String studentFullName,
      String sourceCode,
      List<TestCaseSpec> testSpecs,
      JudgeStatusSseEvent runningEvent) {}

  /**
   * Bốc và xử lý 1 job kế tiếp từ hàng đợi:
   *
   * <ol>
   *   <li>Transaction 1: Khóa job {@code FOR UPDATE SKIP LOCKED}, chuyển {@code running}, commit và
   *       phát sự kiện SSE {@code running}.
   *   <li>Ngoài DB Transaction: Chạy {@link SandboxRunner} (tránh giữ kết nối DB trong lúc chạy
   *       Docker).
   *   <li>Transaction 2: Kiểm tra ràng buộc (nếu AC), lưu kết quả nguyên tử, commit và phát sự kiện
   *       SSE cuối cùng.
   * </ol>
   *
   * @return {@code true} nếu có job được xử lý; {@code false} nếu hàng đợi rỗng.
   */
  public boolean processNextJob() {
    ClaimedJob claimed = transactionTemplate.execute(status -> claimNextJob());
    if (claimed == null) {
      return false;
    }

    submissionEventBroadcaster.publish(claimed.runningEvent());

    SandboxRunResult sandboxResult =
        sandboxRunner.compileAndRun(claimed.sourceCode(), claimed.testSpecs());

    JudgeStatusSseEvent finalEvent =
        transactionTemplate.execute(status -> completeJob(claimed, sandboxResult));
    submissionEventBroadcaster.publish(finalEvent);
    return true;
  }

  /** Transaction 1: Khóa và nhận 1 job đang chờ, đổi trạng thái sang running rồi commit ngay. */
  private ClaimedJob claimNextJob() {
    Optional<JudgeJob> jobOpt = judgeJobRepository.findNextQueuedJobForUpdate();
    if (jobOpt.isEmpty()) {
      return null;
    }

    JudgeJob job = jobOpt.get();
    Submission submission = job.getSubmission();
    Lab lab = submission.getLab();
    User student = submission.getStudent();

    log.info(
        "Processing judge job id={}, submissionId={}, labId={}",
        job.getId(),
        submission.getId(),
        lab.getId());

    job.setStatus("running");
    job.setLockedAt(OffsetDateTime.now());
    job.setLockedBy("worker-" + Thread.currentThread().getName());
    submission.setJudgeStatus("running");
    judgeJobRepository.save(job);
    submissionRepository.save(submission);

    List<LabTest> labTests = labTestRepository.findByLabIdOrderByOrderIndexAsc(lab.getId());
    List<TestCaseSpec> testSpecs =
        labTests.stream()
            .map(
                t ->
                    new TestCaseSpec(
                        t.getName(),
                        t.getInputData(),
                        t.getExpectedOutput(),
                        t.getTimeLimitMs(),
                        t.getMemoryLimitMb()))
            .toList();

    JudgeStatusSseEvent runningEvent =
        new JudgeStatusSseEvent(
            submission.getId(),
            lab.getId(),
            "running",
            submission.getPassedTests(),
            testSpecs.size(),
            null,
            null,
            null,
            submission.getScoreTenths(),
            null);

    return new ClaimedJob(
        job.getId(),
        submission.getId(),
        lab.getId(),
        lab.getStatementMd(),
        lab.getCostCapUsd(),
        student.getId(),
        student.getEmail(),
        student.getFullName(),
        submission.getSourceCode(),
        testSpecs,
        runningEvent);
  }

  /** Transaction 2: Chấm ràng buộc (nếu AC), cập nhật DB nguyên tử và trả về sự kiện SSE cuối. */
  private JudgeStatusSseEvent completeJob(ClaimedJob claimed, SandboxRunResult sandboxResult) {
    JudgeJob job = judgeJobRepository.findById(claimed.jobId()).orElseThrow();
    Submission submission = submissionRepository.findById(claimed.submissionId()).orElseThrow();

    int passedTests = sandboxResult.passedTests();
    int totalTests = sandboxResult.totalTests();
    String judgeStatus = sandboxResult.judgeStatus();
    Integer execTime = sandboxResult.executionTimeMs();
    Integer memoryKb = sandboxResult.memoryKb();
    String compileError = sandboxResult.compileError();

    submission.setPassedTests(passedTests);
    submission.setTotalTests(totalTests);
    submission.setExecutionTimeMs(execTime);
    submission.setMemoryKb(memoryKb);
    submission.setCompileError(compileError);

    // Điểm chức năng thuần từ bộ test (thang 0..100 tenths).
    // Tuyệt đối không tự trừ điểm theo verdict của checker ở bước này (Syllabus Rule):
    // điểm trừ chỉ được tính khi TA xác nhận quyết định trong Review Queue.
    int functionalScoreTenths = totalTests > 0 ? (passedTests * 100) / totalTests : 0;

    // Đồng bộ CountedSubmission trước để thỏa mãn khóa ngoại review_items -> counted_submissions
    syncCountedSubmission(
        submission.getId(),
        claimed.labId(),
        claimed.studentId().toString(),
        judgeStatus,
        functionalScoreTenths);

    if ("AC".equalsIgnoreCase(judgeStatus)) {
      evaluateAndPersistConstraints(claimed, submission);
    }

    OffsetDateTime now = OffsetDateTime.now();
    submission.setJudgeStatus(judgeStatus);
    submission.setScoreTenths(functionalScoreTenths);
    submission.setJudgedAt(now);
    submissionRepository.save(submission);

    job.setStatus("done");
    job.setFinishedAt(now);
    judgeJobRepository.save(job);

    log.info(
        "Finished judge job id={}, submissionId={}, finalStatus={}, score={}",
        job.getId(),
        submission.getId(),
        judgeStatus,
        functionalScoreTenths);

    return new JudgeStatusSseEvent(
        submission.getId(),
        claimed.labId(),
        judgeStatus,
        passedTests,
        totalTests,
        execTime,
        memoryKb,
        compileError,
        functionalScoreTenths,
        now);
  }

  private void evaluateAndPersistConstraints(ClaimedJob claimed, Submission submission) {
    List<LabConstraint> constraints = labConstraintRepository.findByLabId(claimed.labId());

    List<String> piiTokens = new ArrayList<>();
    if (claimed.studentEmail() != null) {
      piiTokens.add(claimed.studentEmail());
    }
    if (claimed.studentFullName() != null) {
      piiTokens.add(claimed.studentFullName());
    }

    List<ConstraintSpec> constraintSpecs =
        constraints.stream()
            .map(
                constraint ->
                    ConstraintSpec.builder()
                        .constraintId(constraint.getId().getConstraintId())
                        .type(constraint.getConstraintType())
                        .description(constraint.getDescription())
                        .deductionTenths(constraint.getDeductionTenths())
                        .build())
            .toList();

    BigDecimal remainingBudgetUsd = calculateRemainingBudgetUsd(claimed);
    CheckRequest checkRequest =
        CheckRequest.builder()
            .solutionKey("submission-" + submission.getId())
            .statementMd(claimed.labStatementMd())
            .sourceCode(submission.getSourceCode())
            .piiTokensToStrip(piiTokens)
            .constraints(constraintSpecs)
            .remainingBudgetUsd(remainingBudgetUsd)
            .modelOff(isModelOffEnabled())
            .build();

    CheckPipelineResult pipelineResult = constraintCheckPipeline.check(checkRequest);
    submission.setStrippedCode(pipelineResult.strippedCode());

    persistLlmCallLogs(submission, pipelineResult.callLogs());

    if (pipelineResult.verdicts() != null) {
      for (ConstraintVerdictResult verdict : pipelineResult.verdicts()) {
        String evidenceJson = serializeEvidence(verdict);
        ReviewQueueSampler.SamplingOutcome sampling =
            reviewQueueSampler.evaluate(verdict.verdict());

        ReviewItem reviewItem =
            ReviewItem.builder()
                .id(
                    ReviewItemId.builder()
                        .submissionId(submission.getId())
                        .constraintId(verdict.constraintId())
                        .build())
                .decision(null)
                .verdict(verdict.verdict())
                .source(verdict.source())
                .evidenceJson(evidenceJson)
                .guardrailReason(verdict.guardrailReason())
                .inQueue(sampling.inQueue())
                .sampledPass(sampling.sampledPass())
                .build();
        reviewItemRepository.save(reviewItem);
      }
    }
  }

  private boolean isModelOffEnabled() {
    if (modelOffRaw == null) {
      return false;
    }
    String normalized = modelOffRaw.trim();
    return "1".equals(normalized) || "true".equalsIgnoreCase(normalized);
  }

  private BigDecimal calculateRemainingBudgetUsd(ClaimedJob claimed) {
    BigDecimal costCap =
        claimed.labCostCapUsd() != null ? claimed.labCostCapUsd() : BigDecimal.ZERO;
    BigDecimal spent = llmCallLogRepository.sumCostUsdByLabId(claimed.labId());
    BigDecimal safeSpent = spent != null ? spent : BigDecimal.ZERO;
    return costCap.subtract(safeSpent).max(BigDecimal.ZERO);
  }

  private void persistLlmCallLogs(Submission submission, List<LlmCallRecord> callLogs) {
    if (callLogs == null || callLogs.isEmpty()) {
      return;
    }
    Lab lab = submission.getLab();
    for (LlmCallRecord callRecord : callLogs) {
      LlmCallLog logEntity =
          LlmCallLog.builder()
              .submission(submission)
              .lab(lab)
              .constraintId(callRecord.constraintId())
              .provider(callRecord.provider())
              .model(callRecord.model())
              .providerRequestId(callRecord.providerRequestId())
              .promptTokens(callRecord.promptTokens())
              .completionTokens(callRecord.completionTokens())
              .latencyMs(callRecord.latencyMs())
              .costUsd(callRecord.costUsd() != null ? callRecord.costUsd() : BigDecimal.ZERO)
              .sanitizedPrompt(callRecord.sanitizedPrompt())
              .rawResponse(callRecord.rawResponse())
              .verdict(callRecord.verdict())
              .guardrailReason(callRecord.guardrailReason())
              .build();
      llmCallLogRepository.save(logEntity);
    }
  }

  private String serializeEvidence(ConstraintVerdictResult verdict) {
    if (verdict.evidence() == null) {
      return "[]";
    }
    try {
      return objectMapper.writeValueAsString(verdict.evidence());
    } catch (JsonProcessingException ignore) {
      return "[]";
    }
  }

  /**
   * Đồng bộ bản ghi sang bảng counted_submissions để bảo toàn khóa ngoại của review_items và tính
   * tương thích ngược với hợp đồng PA#2. Nếu sinh viên nộp lại bài mới trên cùng 1 lab, thay thế
   * bản ghi cũ theo ràng buộc UNIQUE (lab_id, student_id).
   */
  private void syncCountedSubmission(
      Integer submissionId, Integer labId, String studentId, String judgeStatus, int scoreTenths) {
    Optional<CountedSubmission> existingForStudent =
        countedSubmissionRepository.findByLabIdAndStudentId(labId, studentId);
    if (existingForStudent.isPresent() && !existingForStudent.get().getId().equals(submissionId)) {
      int oldId = existingForStudent.get().getId();
      reviewItemRepository.deleteBySubmissionId(oldId);
      countedSubmissionRepository.deleteById(oldId);
      countedSubmissionRepository.flush();
    }

    CountedSubmission counted =
        countedSubmissionRepository
            .findById(submissionId)
            .orElseGet(
                () ->
                    CountedSubmission.builder()
                        .id(submissionId)
                        .labId(labId)
                        .studentId(studentId)
                        .build());

    counted.setJudgeStatus(judgeStatus);
    counted.setScoreTenths(scoreTenths);
    countedSubmissionRepository.save(counted);
  }
}
