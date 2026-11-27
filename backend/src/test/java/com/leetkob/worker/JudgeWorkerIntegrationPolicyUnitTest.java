package com.leetkob.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetkob.checker.CheckPipelineResult;
import com.leetkob.checker.CheckRequest;
import com.leetkob.checker.CheckerConstants;
import com.leetkob.checker.ConstraintCheckPipeline;
import com.leetkob.checker.ConstraintVerdictResult;
import com.leetkob.checker.EvidenceLine;
import com.leetkob.checker.LlmCallRecord;
import com.leetkob.domain.ReviewQueueSampler;
import com.leetkob.entity.CountedSubmission;
import com.leetkob.entity.JudgeJob;
import com.leetkob.entity.Lab;
import com.leetkob.entity.LabConstraint;
import com.leetkob.entity.LabTest;
import com.leetkob.entity.LlmCallLog;
import com.leetkob.entity.ReviewItem;
import com.leetkob.entity.Submission;
import com.leetkob.entity.User;
import com.leetkob.entity.id.LabConstraintId;
import com.leetkob.repository.CountedSubmissionRepository;
import com.leetkob.repository.JudgeJobRepository;
import com.leetkob.repository.LabConstraintRepository;
import com.leetkob.repository.LabTestRepository;
import com.leetkob.repository.LlmCallLogRepository;
import com.leetkob.repository.ReviewItemRepository;
import com.leetkob.repository.SubmissionRepository;
import com.leetkob.sandbox.SandboxRunResult;
import com.leetkob.sandbox.SandboxRunner;
import com.leetkob.service.SubmissionEventBroadcaster;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verifies the Section 0 integration safeguards in {@link JudgeWorker}: no pre-TA score deduction
 * (Syllabus Rule), persistence of {@code llm_call_logs}, real remaining budget + {@code modelOff}
 * forwarding, and {@link ReviewQueueSampler} (H1) routing.
 */
class JudgeWorkerIntegrationPolicyUnitTest {

  @Test
  void violationNeverAutoDeductsScorePersistsCallLogsComputesBudgetAndUsesSampler() {
    JudgeJobRepository judgeJobRepository = mock(JudgeJobRepository.class);
    SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
    LabTestRepository labTestRepository = mock(LabTestRepository.class);
    LabConstraintRepository labConstraintRepository = mock(LabConstraintRepository.class);
    CountedSubmissionRepository countedSubmissionRepository =
        mock(CountedSubmissionRepository.class);
    ReviewItemRepository reviewItemRepository = mock(ReviewItemRepository.class);
    LlmCallLogRepository llmCallLogRepository = mock(LlmCallLogRepository.class);
    SandboxRunner sandboxRunner = mock(SandboxRunner.class);
    ConstraintCheckPipeline pipeline = mock(ConstraintCheckPipeline.class);
    SubmissionEventBroadcaster broadcaster = mock(SubmissionEventBroadcaster.class);
    TransactionTemplate txTemplate = mock(TransactionTemplate.class);
    ReviewQueueSampler deterministicSampler = new ReviewQueueSampler(42L);

    when(txTemplate.execute(any()))
        .thenAnswer(
            invocation -> {
              TransactionCallback<?> callback = invocation.getArgument(0);
              return callback.doInTransaction(mock(TransactionStatus.class));
            });

    UUID studentId = UUID.randomUUID();
    User student =
        User.builder()
            .id(studentId)
            .email("student1@leetkob.edu")
            .fullName("Nguyen Van A")
            .role("student")
            .build();
    Lab lab =
        Lab.builder()
            .id(10)
            .classId(1)
            .deadline("2026-12-01T00:00:00Z")
            .statementMd("# Merge Sort")
            .costCapUsd(new BigDecimal("3.5000"))
            .build();
    Submission submission =
        Submission.builder()
            .id(101)
            .lab(lab)
            .student(student)
            .sourceCode("int main() { std::sort(a, a+n); }")
            .judgeStatus("queued")
            .scoreTenths(0)
            .passedTests(0)
            .totalTests(1)
            .traceId("trace-101")
            .build();
    JudgeJob job = JudgeJob.builder().id(55L).submission(submission).status("queued").build();

    when(judgeJobRepository.findNextQueuedJobForUpdate()).thenReturn(Optional.of(job));
    when(judgeJobRepository.findById(55L)).thenReturn(Optional.of(job));
    when(submissionRepository.findById(101)).thenReturn(Optional.of(submission));
    when(labTestRepository.findByLabIdOrderByOrderIndexAsc(10))
        .thenReturn(
            List.of(
                LabTest.builder()
                    .id(1L)
                    .lab(lab)
                    .name("t1")
                    .inputData("1")
                    .expectedOutput("1")
                    .timeLimitMs(2000)
                    .memoryLimitMb(256)
                    .orderIndex(0)
                    .build()));
    when(labConstraintRepository.findByLabId(10))
        .thenReturn(
            List.of(
                LabConstraint.builder()
                    .id(LabConstraintId.builder().labId(10).constraintId("C1").build())
                    .lab(lab)
                    .constraintType("hard")
                    .description("Do not call `std::sort`.")
                    .deductionTenths(30)
                    .build(),
                LabConstraint.builder()
                    .id(LabConstraintId.builder().labId(10).constraintId("C2").build())
                    .lab(lab)
                    .constraintType("semantic")
                    .description("Implement divide-and-conquer merge sort.")
                    .deductionTenths(40)
                    .build()));
    when(llmCallLogRepository.sumCostUsdByLabId(10)).thenReturn(new BigDecimal("1.2500"));
    when(countedSubmissionRepository.findByLabIdAndStudentId(10, studentId.toString()))
        .thenReturn(Optional.empty());
    when(countedSubmissionRepository.findById(101)).thenReturn(Optional.empty());
    when(sandboxRunner.compileAndRun(any(), any()))
        .thenReturn(new SandboxRunResult("AC", 1, 1, 15, 1024, null));

    CheckPipelineResult fakePipelineResult =
        CheckPipelineResult.builder()
            .strippedCode("int main() { std::sort(a, a+n); }")
            .verdicts(
                List.of(
                    ConstraintVerdictResult.builder()
                        .constraintId("C1")
                        .verdict(CheckerConstants.VERDICT_VIOLATION)
                        .source(CheckerConstants.SOURCE_HARD_RULE)
                        .evidence(
                            List.of(
                                EvidenceLine.builder().line(1).text("std::sort(a, a+n);").build()))
                        .build(),
                    ConstraintVerdictResult.builder()
                        .constraintId("C2")
                        .verdict(CheckerConstants.VERDICT_PASS)
                        .source(CheckerConstants.SOURCE_LLM)
                        .evidence(List.of())
                        .build()))
            .callLogs(
                List.of(
                    LlmCallRecord.builder()
                        .constraintId("C2")
                        .provider("openai")
                        .model("gpt-4o-mini")
                        .providerRequestId("req_real_123")
                        .promptTokens(200)
                        .completionTokens(40)
                        .latencyMs(420)
                        .costUsd(new BigDecimal("0.000450"))
                        .sanitizedPrompt("prompt")
                        .rawResponse("{}")
                        .verdict(CheckerConstants.VERDICT_PASS)
                        .build()))
            .build();
    when(pipeline.check(any())).thenReturn(fakePipelineResult);

    JudgeWorker worker =
        new JudgeWorker(
            judgeJobRepository,
            submissionRepository,
            labTestRepository,
            labConstraintRepository,
            countedSubmissionRepository,
            reviewItemRepository,
            llmCallLogRepository,
            deterministicSampler,
            sandboxRunner,
            pipeline,
            broadcaster,
            new ObjectMapper(),
            txTemplate);
    ReflectionTestUtils.setField(worker, "modelOffRaw", "1");

    assertTrue(worker.processNextJob());

    // 1. Syllabus rule: violation MUST NOT auto-deduct functional score before TA decision
    assertEquals(100, submission.getScoreTenths());
    ArgumentCaptor<CountedSubmission> countedCaptor =
        ArgumentCaptor.forClass(CountedSubmission.class);
    verify(countedSubmissionRepository).save(countedCaptor.capture());
    assertEquals(100, countedCaptor.getValue().getScoreTenths());

    // 2. Real remaining budget (3.5000 - 1.2500 = 2.2500) and modelOff forwarded to pipeline
    ArgumentCaptor<CheckRequest> requestCaptor = ArgumentCaptor.forClass(CheckRequest.class);
    verify(pipeline).check(requestCaptor.capture());
    assertEquals(
        0, new BigDecimal("2.2500").compareTo(requestCaptor.getValue().remainingBudgetUsd()));
    assertTrue(requestCaptor.getValue().modelOff());

    // 3. Call log persisted to llm_call_logs
    ArgumentCaptor<LlmCallLog> logCaptor = ArgumentCaptor.forClass(LlmCallLog.class);
    verify(llmCallLogRepository).save(logCaptor.capture());
    assertEquals("req_real_123", logCaptor.getValue().getProviderRequestId());
    assertEquals("C2", logCaptor.getValue().getConstraintId());

    // 4. ReviewQueueSampler applied: violation -> inQueue=true, sampledPass=false
    ArgumentCaptor<ReviewItem> reviewCaptor = ArgumentCaptor.forClass(ReviewItem.class);
    verify(reviewItemRepository, org.mockito.Mockito.times(2)).save(reviewCaptor.capture());
    ReviewItem violationItem =
        reviewCaptor.getAllValues().stream()
            .filter(item -> "C1".equals(item.getId().getConstraintId()))
            .findFirst()
            .orElseThrow();
    assertTrue(violationItem.getInQueue());
    assertFalse(violationItem.getSampledPass());
  }
}
