package com.leetkob.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.leetkob.LeetKobApplication;
import com.leetkob.dto.submission.SubmissionSummary;
import com.leetkob.entity.JudgeJob;
import com.leetkob.entity.Lab;
import com.leetkob.entity.Submission;
import com.leetkob.entity.User;
import com.leetkob.repository.JudgeJobRepository;
import com.leetkob.repository.LabRepository;
import com.leetkob.repository.LabTestRepository;
import com.leetkob.repository.SubmissionRepository;
import com.leetkob.repository.UserRepository;
import com.leetkob.service.SubmissionService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Kiểm thử đảm bảo chất lượng hàng đợi bất đồng bộ Q1 (Exact-Once Execution & Idempotency). */
@SpringBootTest(
    classes = LeetKobApplication.class,
    properties = {"leetkob.role=api", "leetkob.sandbox.enabled=false"})
public class JudgeWorkerExactOnceTest {

  @Autowired private JudgeWorker judgeWorker;
  @Autowired private SubmissionService submissionService;
  @Autowired private SubmissionRepository submissionRepository;
  @Autowired private JudgeJobRepository judgeJobRepository;
  @Autowired private LabRepository labRepository;
  @Autowired private LabTestRepository labTestRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private Lab lab;
  private User student;

  @BeforeEach
  void setUp() {
    // Dọn dẹp dữ liệu bài nộp và job trước mỗi bài test
    jdbcTemplate.update("DELETE FROM review_items");
    jdbcTemplate.update("DELETE FROM counted_submissions");
    jdbcTemplate.update("DELETE FROM judge_jobs");
    jdbcTemplate.update("DELETE FROM submissions");
    jdbcTemplate.update("DELETE FROM lab_tests");
    jdbcTemplate.update("DELETE FROM lab_constraints");
    jdbcTemplate.update("DELETE FROM ta_classes");
    jdbcTemplate.update("DELETE FROM labs WHERE id = 9901");
    jdbcTemplate.update("DELETE FROM users WHERE email = 'student_test_q1@leetkob.edu'");

    // Tạo sinh viên mẫu qua jdbcTemplate
    UUID studentUuid = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO users (id, email, full_name, role) VALUES (?, 'student_test_q1@leetkob.edu', 'Test Student Q1', 'student')",
        studentUuid);
    student = userRepository.findById(studentUuid).orElseThrow();

    // Tạo Lab mẫu qua jdbcTemplate
    jdbcTemplate.update(
        "INSERT INTO labs (id, class_id, deadline, title, statement_md) VALUES (9901, 1, ?, 'Sort Merge Lab', '# Sort Problem')",
        java.time.Instant.now().plusSeconds(86400).toString());
    lab = labRepository.findById(9901).orElseThrow();

    // Tạo Test Cases mẫu qua jdbcTemplate
    jdbcTemplate.update(
        "INSERT INTO lab_tests (lab_id, name, input_data, expected_output, time_limit_ms, memory_limit_mb, order_index) VALUES (9901, 'Test 1', '3\n3 2 1', '1 2 3', 2000, 256, 1)");
  }

  @Test
  void testExactOnceJudgeExecution() {
    // 1. Sinh viên nộp bài: tạo 1 submission ở trạng thái 'queued'
    String compliantCode =
        """
        #include <iostream>
        int main() {
            std::cout << "1 2 3";
            return 0;
        }
        """;

    SubmissionSummary summary =
        submissionService.submit(lab.getId(), student.getId(), compliantCode, null, null);

    assertNotNull(summary.getId());
    assertEquals("queued", summary.getJudgeStatus());

    JudgeJob jobBefore = judgeJobRepository.findBySubmissionId(summary.getId()).orElseThrow();
    assertEquals("queued", jobBefore.getStatus());

    // 2. Worker bốc job lần 1: Xử lý thành công
    boolean processedFirstTime = judgeWorker.processNextJob();
    assertTrue(processedFirstTime, "Worker phải bốc và xử lý được job đang queued");

    Submission judgedSub = submissionRepository.findById(summary.getId()).orElseThrow();
    assertEquals("AC", judgedSub.getJudgeStatus());
    assertEquals(100, judgedSub.getScoreTenths());
    assertEquals(1, judgedSub.getPassedTests());
    assertEquals(1, judgedSub.getTotalTests());
    assertNotNull(judgedSub.getJudgedAt());

    JudgeJob jobAfter = judgeJobRepository.findBySubmissionId(summary.getId()).orElseThrow();
    assertEquals("done", jobAfter.getStatus());
    assertNotNull(jobAfter.getFinishedAt());

    // 3. Worker quét lần 2 (Replay / Polling lại): Không còn job nào để bốc (Exact-Once Q1)
    boolean processedSecondTime = judgeWorker.processNextJob();
    assertFalse(processedSecondTime, "Không được xử lý lại job đã xong (Exact-Once Q1)");

    JudgeJob jobFinal = judgeJobRepository.findBySubmissionId(summary.getId()).orElseThrow();
    assertEquals("done", jobFinal.getStatus(), "Trạng thái job vẫn giữ nguyên là done");
  }

  @Test
  void testCompileErrorHandlingStopsEarlyWithoutExceptions() {
    // Nộp code có lỗi biên dịch (syntax_error)
    String invalidCode =
        """
        #include <iostream>
        int main() {
            syntax_error_here
            return 0;
        }
        """;

    SubmissionSummary summary =
        submissionService.submit(lab.getId(), student.getId(), invalidCode, null, null);

    boolean processed = judgeWorker.processNextJob();
    assertTrue(processed);

    Submission judged = submissionRepository.findById(summary.getId()).orElseThrow();
    assertEquals("CE", judged.getJudgeStatus());
    assertEquals(0, judged.getScoreTenths());
    assertNotNull(judged.getCompileError());
    assertTrue(judged.getCompileError().contains("error"));

    JudgeJob job = judgeJobRepository.findBySubmissionId(summary.getId()).orElseThrow();
    assertEquals("done", job.getStatus());
  }

  @Test
  void testIdempotencyKeyPreventsDuplicateSubmission() {
    String sourceCode = "int main() { return 0; }";
    String idempotencyKey = "unique-key-" + UUID.randomUUID();

    // Nộp lần 1
    SubmissionSummary sub1 =
        submissionService.submit(lab.getId(), student.getId(), sourceCode, idempotencyKey, null);

    // Nộp lần 2 với cùng Idempotency-Key
    SubmissionSummary sub2 =
        submissionService.submit(lab.getId(), student.getId(), sourceCode, idempotencyKey, null);

    // Cả 2 lần gọi phải trả về cùng 1 Submission ID duy nhất
    assertEquals(
        sub1.getId(),
        sub2.getId(),
        "Hai lần gọi cùng idempotencyKey phải trả về cùng 1 submission");

    // Kiểm tra trong DB chỉ có đúng 1 bản ghi
    long count =
        submissionRepository.findAll().stream()
            .filter(s -> idempotencyKey.equals(s.getIdempotencyKey()))
            .count();
    assertEquals(1, count, "Trong CSDL chỉ tồn tại đúng 1 bài nộp cho key này");
  }

  /**
   * Kiểm tra nộp lại bài nhiều lần trên cùng 1 lab: bảng submissions lưu đủ lịch sử, còn
   * counted_submissions tự thay thế sang bài mới nhất không bị lỗi trùng UNIQUE (lab_id,
   * student_id).
   */
  @Test
  void testResubmissionReplacesCountedSubmissionAndPersistsReviewItems() {
    jdbcTemplate.update(
        "INSERT INTO lab_constraints (lab_id, constraint_id, constraint_type, description, deduction_tenths) VALUES (9901, 'NO_GLOBAL', 'hard', 'Do not use `goto`.', 10)");

    // Lần 1: nộp bài WA
    SubmissionSummary first =
        submissionService.submit(lab.getId(), student.getId(), "// MOCK_WA", null, null);
    assertTrue(judgeWorker.processNextJob());

    Submission firstSub = submissionRepository.findById(first.getId()).orElseThrow();
    assertEquals("WA", firstSub.getJudgeStatus());

    // Lần 2: sinh viên sửa lại và nộp bài AC
    SubmissionSummary second =
        submissionService.submit(
            lab.getId(), student.getId(), "int main() { return 0; }", null, null);
    assertTrue(judgeWorker.processNextJob());

    Submission secondSub = submissionRepository.findById(second.getId()).orElseThrow();
    assertEquals("AC", secondSub.getJudgeStatus());
    assertEquals(100, secondSub.getScoreTenths());

    // counted_submissions chỉ có 1 dòng và đã trỏ sang ID lần nộp thứ 2
    Integer countedId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM counted_submissions WHERE lab_id = 9901 AND student_id = ?",
            Integer.class,
            student.getId().toString());
    assertEquals(second.getId(), countedId);

    // review_items phải được tạo với source = 'hard_rule' khớp DB CHECK constraint
    Integer reviewCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM review_items WHERE submission_id = ? AND source = 'hard_rule'",
            Integer.class,
            second.getId());
    assertEquals(1, reviewCount);
  }

  /** Kiểm tra xử lý các trạng thái lỗi chức năng (WA, TLE, MLE, RE) và hàng đợi rỗng. */
  @Test
  void testNonAcVerdictsArePersistedCorrectly() {
    assertFalse(judgeWorker.processNextJob(), "Hàng đợi rỗng phải trả về false");

    String[] markers = {"// MOCK_WA", "// MOCK_TLE", "// MOCK_MLE", "// MOCK_RE"};
    String[] expectedStatuses = {"WA", "TLE", "MLE", "RE"};

    for (int i = 0; i < markers.length; i++) {
      SubmissionSummary sub =
          submissionService.submit(lab.getId(), student.getId(), markers[i], null, null);
      assertTrue(judgeWorker.processNextJob());
      Submission updated = submissionRepository.findById(sub.getId()).orElseThrow();
      assertEquals(expectedStatuses[i], updated.getJudgeStatus());
      JudgeJob job = judgeJobRepository.findBySubmissionId(sub.getId()).orElseThrow();
      assertEquals("done", job.getStatus());
    }
  }
}
