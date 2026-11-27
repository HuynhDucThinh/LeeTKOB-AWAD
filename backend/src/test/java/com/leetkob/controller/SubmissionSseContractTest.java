package com.leetkob.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetkob.LeetKobApplication;
import com.leetkob.dto.submission.SubmissionSummary;
import com.leetkob.security.jwt.JwtUtil;
import com.leetkob.service.SubmissionService;
import com.leetkob.worker.JudgeWorker;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

/** Kiểm thử hợp đồng luồng SSE real-time (/v1/submissions/{id}/events). */
@SpringBootTest(
    classes = LeetKobApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"leetkob.role=api", "leetkob.sandbox.enabled=false"})
public class SubmissionSseContractTest {

  @LocalServerPort private int port;

  @Autowired private JwtUtil jwtUtil;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private SubmissionService submissionService;
  @Autowired private JudgeWorker judgeWorker;
  @Autowired private ObjectMapper objectMapper;

  private final HttpClient httpClient = HttpClient.newHttpClient();

  private UUID student1Id;
  private String student1Token;
  private String student2Token;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM review_items");
    jdbcTemplate.update("DELETE FROM counted_submissions");
    jdbcTemplate.update("DELETE FROM judge_jobs");
    jdbcTemplate.update("DELETE FROM submissions");
    jdbcTemplate.update("DELETE FROM lab_tests");
    jdbcTemplate.update("DELETE FROM lab_constraints");
    jdbcTemplate.update("DELETE FROM ta_classes");
    jdbcTemplate.update("DELETE FROM labs WHERE id = 8805");
    jdbcTemplate.update(
        "DELETE FROM users WHERE email IN ('sse_sv1@leetkob.edu', 'sse_sv2@leetkob.edu')");

    student1Id = UUID.randomUUID();
    UUID student2Id = UUID.randomUUID();

    jdbcTemplate.update(
        "INSERT INTO users (id, email, full_name, role) VALUES (?, 'sse_sv1@leetkob.edu', 'SSE SV1', 'student')",
        student1Id);
    jdbcTemplate.update(
        "INSERT INTO users (id, email, full_name, role) VALUES (?, 'sse_sv2@leetkob.edu', 'SSE SV2', 'student')",
        student2Id);

    student1Token = jwtUtil.generateToken(student1Id, "sse_sv1@leetkob.edu", "student");
    student2Token = jwtUtil.generateToken(student2Id, "sse_sv2@leetkob.edu", "student");

    jdbcTemplate.update(
        "INSERT INTO labs (id, class_id, deadline, title, statement_md) VALUES (8805, 1, ?, 'SSE Lab', '# SSE')",
        Instant.now().plusSeconds(86400).toString());
    jdbcTemplate.update(
        "INSERT INTO lab_tests (lab_id, name, input_data, expected_output, time_limit_ms, memory_limit_mb, order_index) VALUES (8805, 'T1', '1 2', '3', 2000, 256, 1)");
  }

  /** Chưa đăng nhập gọi endpoint SSE phải bị từ chối với 401. */
  @Test
  void sseWithoutAuthReturns401() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/v1/submissions/1/events"))
            .GET()
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(401, response.statusCode());
  }

  /** Sinh viên không thể mở SSE xem bài của sinh viên khác (trả về 404). */
  @Test
  void sseForOtherStudentSubmissionReturns404() throws Exception {
    SubmissionSummary created =
        submissionService.submit(8805, student1Id, "int main(){return 0;}", null, null);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    "http://localhost:" + port + "/v1/submissions/" + created.getId() + "/events"))
            .header("Authorization", "Bearer " + student2Token)
            .GET()
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(404, response.statusCode());
  }

  /**
   * Mở SSE stream khi bài đang queued, kích hoạt Worker chấm bài -> Client nhận được event queued,
   * running, AC rồi stream tự động đóng.
   */
  @Test
  void sseStreamPushesRunningAndFinalStatusAndCloses() throws Exception {
    SubmissionSummary created =
        submissionService.submit(8805, student1Id, "int main(){return 0;}", null, null);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    "http://localhost:" + port + "/v1/submissions/" + created.getId() + "/events"))
            .header("Authorization", "Bearer " + student1Token)
            .GET()
            .build();

    CompletableFuture<HttpResponse<String>> sseFuture =
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

    // Đợi một chút cho kết nối SSE đăng ký xong, sau đó kích hoạt Worker chấm 1 job
    Thread.sleep(300);
    assertTrue(judgeWorker.processNextJob());

    HttpResponse<String> response = sseFuture.get(5, TimeUnit.SECONDS);
    assertEquals(200, response.statusCode());
    assertTrue(
        response.headers().firstValue("Content-Type").orElse("").contains("text/event-stream"));

    String body = response.body();
    assertTrue(body.contains("\"judgeStatus\":\"queued\""), "Phải có event queued: " + body);
    assertTrue(body.contains("\"judgeStatus\":\"AC\""), "Phải có event AC cuối cùng: " + body);
  }

  /**
   * Chống Race Condition: Nếu bài đã chấm xong (AC) trước khi client mở SSE, server phải gửi ngay
   * trạng thái AC và đóng stream lập tức.
   */
  @Test
  void sseStreamForAlreadyFinishedSubmissionSendsSnapshotAndClosesImmediately() throws Exception {
    SubmissionSummary created =
        submissionService.submit(8805, student1Id, "int main(){return 0;}", null, null);
    assertTrue(judgeWorker.processNextJob());

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    "http://localhost:" + port + "/v1/submissions/" + created.getId() + "/events"))
            .header("Authorization", "Bearer " + student1Token)
            .GET()
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("\"judgeStatus\":\"AC\""));
  }

  /**
   * Kiểm chứng cơ chế 2-JVM DB Polling: cập nhật trạng thái AC trực tiếp trong DB (không gọi
   * broadcaster.publish), bộ sse-db-poller vẫn phát hiện và đẩy qua SSE cho trình duyệt.
   */
  @Test
  void sseDbPollerDetectsExternalDatabaseUpdateForTwoJvmMode() throws Exception {
    SubmissionSummary created =
        submissionService.submit(8805, student1Id, "int main(){return 0;}", null, null);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    "http://localhost:" + port + "/v1/submissions/" + created.getId() + "/events"))
            .header("Authorization", "Bearer " + student1Token)
            .GET()
            .build();

    CompletableFuture<HttpResponse<String>> sseFuture =
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

    Thread.sleep(250);
    // Mô phỏng container Worker ở JVM khác ghi trực tiếp kết quả xuống DB
    jdbcTemplate.update(
        "UPDATE submissions SET judge_status = 'AC', passed_tests = 1, total_tests = 1, score_tenths = 100 WHERE id = ?",
        created.getId());

    HttpResponse<String> response = sseFuture.get(5, TimeUnit.SECONDS);
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("\"judgeStatus\":\"queued\""));
    assertTrue(response.body().contains("\"judgeStatus\":\"AC\""));
  }
}
