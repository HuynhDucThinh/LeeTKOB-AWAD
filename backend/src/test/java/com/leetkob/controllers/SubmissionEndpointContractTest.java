package com.leetkob.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetkob.LeetKobApplication;
import com.leetkob.security.jwt.JwtUtil;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Kiểm thử hợp đồng API nộp bài (/v1/labs/{labId}/submissions và /v1/submissions/{id}) tuân thủ
 * chuẩn RFC 9457 và các rào chắn phân quyền.
 */
@SpringBootTest(
    classes = LeetKobApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"leetkob.role=api", "leetkob.sandbox.enabled=false"})
public class SubmissionEndpointContractTest {

  @LocalServerPort private int port;

  @Autowired private JwtUtil jwtUtil;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private ObjectMapper objectMapper;

  private final HttpClient httpClient = HttpClient.newHttpClient();

  private UUID student1Id;
  private UUID student2Id;
  private UUID taId;
  private String student1Token;
  private String student2Token;
  private String taToken;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM publication_audit");
    jdbcTemplate.update("DELETE FROM published_marks");
    jdbcTemplate.update("DELETE FROM publications");
    jdbcTemplate.update("DELETE FROM review_items");
    jdbcTemplate.update("DELETE FROM counted_submissions");
    jdbcTemplate.update("DELETE FROM judge_jobs");
    jdbcTemplate.update("DELETE FROM submissions");
    jdbcTemplate.update("DELETE FROM lab_tests");
    jdbcTemplate.update("DELETE FROM lab_constraints");
    jdbcTemplate.update("DELETE FROM ta_classes");
    jdbcTemplate.update("DELETE FROM labs WHERE id IN (8801, 8802)");
    jdbcTemplate.update(
        "DELETE FROM users WHERE email IN ('sub_student1@leetkob.edu', 'sub_student2@leetkob.edu', 'sub_ta@leetkob.edu')");

    student1Id = UUID.randomUUID();
    student2Id = UUID.randomUUID();
    taId = UUID.randomUUID();

    jdbcTemplate.update(
        "INSERT INTO users (id, email, full_name, role) VALUES (?, 'sub_student1@leetkob.edu', 'Student 1', 'student')",
        student1Id);
    jdbcTemplate.update(
        "INSERT INTO users (id, email, full_name, role) VALUES (?, 'sub_student2@leetkob.edu', 'Student 2', 'student')",
        student2Id);
    jdbcTemplate.update(
        "INSERT INTO users (id, email, full_name, role) VALUES (?, 'sub_ta@leetkob.edu', 'TA User', 'ta')",
        taId);

    student1Token = jwtUtil.generateToken(student1Id, "sub_student1@leetkob.edu", "student");
    student2Token = jwtUtil.generateToken(student2Id, "sub_student2@leetkob.edu", "student");
    taToken = jwtUtil.generateToken(taId, "sub_ta@leetkob.edu", "ta");

    // Lab 8801: còn hạn
    jdbcTemplate.update(
        "INSERT INTO labs (id, class_id, deadline, title, statement_md) VALUES (8801, 1, ?, 'Active Lab', '# Active')",
        Instant.now().plusSeconds(86400).toString());

    // Lab 8802: đã hết hạn nộp
    jdbcTemplate.update(
        "INSERT INTO labs (id, class_id, deadline, title, statement_md) VALUES (8802, 1, ?, 'Expired Lab', '# Expired')",
        Instant.now().minusSeconds(86400).toString());
  }

  @Test
  void submitWithValidStudentTokenReturns201Created() throws Exception {
    String payload = "{\"sourceCode\": \"#include <iostream>\\nint main() { return 0; }\"}";

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/v1/labs/8801/submissions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + student1Token)
            .header("Idempotency-Key", "sub-key-01")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(201, response.statusCode());
    JsonNode root = objectMapper.readTree(response.body());
    assertNotNull(root.get("id"));
    assertEquals(8801, root.get("labId").asInt());
    assertEquals("queued", root.get("judgeStatus").asText());
    assertNull(root.get("publishedScoreTenths").textValue());
  }

  @Test
  void submitWithoutAuthReturns401Rfc9457() throws Exception {
    String payload = "{\"sourceCode\": \"int main() {}\"}";

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/v1/labs/8801/submissions"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(401, response.statusCode());
    assertTrue(
        response
            .headers()
            .firstValue("Content-Type")
            .orElse("")
            .contains("application/problem+json"));
    JsonNode root = objectMapper.readTree(response.body());
    assertEquals(401, root.get("status").asInt());
  }

  @Test
  void submitWithTaRoleReturns403Forbidden() throws Exception {
    String payload = "{\"sourceCode\": \"int main() {}\"}";

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/v1/labs/8801/submissions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + taToken)
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(403, response.statusCode());
    JsonNode root = objectMapper.readTree(response.body());
    assertEquals(403, root.get("status").asInt());
  }

  @Test
  void submitAfterDeadlineReturns409DeadlinePassed() throws Exception {
    String payload = "{\"sourceCode\": \"int main() {}\"}";

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/v1/labs/8802/submissions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + student1Token)
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(409, response.statusCode());
    JsonNode root = objectMapper.readTree(response.body());
    assertEquals("https://leetkob.example/problems/deadline-passed", root.get("type").asText());
  }

  @Test
  void studentCannotViewOtherStudentSubmission() throws Exception {
    // Student 1 nộp bài
    String payload = "{\"sourceCode\": \"int main() {}\"}";
    HttpRequest req1 =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/v1/labs/8801/submissions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + student1Token)
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

    HttpResponse<String> resp1 = httpClient.send(req1, HttpResponse.BodyHandlers.ofString());
    assertEquals(201, resp1.statusCode());
    int subId = objectMapper.readTree(resp1.body()).get("id").asInt();

    // Student 2 cố xem bài của Student 1 -> phải nhận 404 Not Found (để không lộ thông tin bài)
    HttpRequest req2 =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/v1/submissions/" + subId))
            .header("Authorization", "Bearer " + student2Token)
            .GET()
            .build();

    HttpResponse<String> resp2 = httpClient.send(req2, HttpResponse.BodyHandlers.ofString());
    assertEquals(404, resp2.statusCode());
  }

  /** Gửi body thiếu sourceCode hợp lệ phải trả về 400 chuẩn RFC 9457. */
  @Test
  void submitEmptySourceCodeReturns400ValidationProblem() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/v1/labs/8801/submissions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + student1Token)
            .POST(HttpRequest.BodyPublishers.ofString("{\"sourceCode\":\"   \"}"))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(400, response.statusCode());
    assertTrue(
        response
            .headers()
            .firstValue("Content-Type")
            .orElse("")
            .contains("application/problem+json"));
  }

  /** Nộp bài vào labId không tồn tại phải trả về 404. */
  @Test
  void submitToNonExistentLabReturns404() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/v1/labs/999999/submissions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + student1Token)
            .POST(HttpRequest.BodyPublishers.ofString("{\"sourceCode\":\"int main(){}\"}"))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(404, response.statusCode());
  }

  /** Gọi POST 2 lần cùng Idempotency-Key phải trả về đúng 1 submission ID duy nhất. */
  @Test
  void submitWithSameIdempotencyKeyReturnsSameSubmission() throws Exception {
    String payload = "{\"sourceCode\":\"int main(){return 0;}\"}";
    String key = "http-idem-" + UUID.randomUUID();

    HttpRequest req1 =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/v1/labs/8801/submissions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + student1Token)
            .header("Idempotency-Key", key)
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

    HttpResponse<String> resp1 = httpClient.send(req1, HttpResponse.BodyHandlers.ofString());
    HttpResponse<String> resp2 = httpClient.send(req1, HttpResponse.BodyHandlers.ofString());
    assertEquals(201, resp1.statusCode());
    assertEquals(201, resp2.statusCode());

    int id1 = objectMapper.readTree(resp1.body()).get("id").asInt();
    int id2 = objectMapper.readTree(resp2.body()).get("id").asInt();
    assertEquals(id1, id2);
  }

  /** GET /v1/labs/{labId}/submissions: Sinh viên chỉ thấy bài của mình; TA thấy bài cả lớp. */
  @Test
  void getLabSubmissionsIsolatesStudentsWhileTaSeesAll() throws Exception {
    String payload = "{\"sourceCode\":\"int main(){return 0;}\"}";
    httpClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/v1/labs/8801/submissions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + student1Token)
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build(),
        HttpResponse.BodyHandlers.ofString());
    httpClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/v1/labs/8801/submissions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + student2Token)
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    HttpResponse<String> sv1List =
        httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/v1/labs/8801/submissions"))
                .header("Authorization", "Bearer " + student1Token)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, sv1List.statusCode());
    assertEquals(1, objectMapper.readTree(sv1List.body()).size());

    HttpResponse<String> taList =
        httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/v1/labs/8801/submissions"))
                .header("Authorization", "Bearer " + taToken)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, taList.statusCode());
    assertEquals(2, objectMapper.readTree(taList.body()).size());
  }

  /** Kiểm tra ẩn publishedScoreTenths trước Publish và hiện lại sau khi Lab đã Publish. */
  @Test
  void getSubmissionDetailHidesScoreBeforePublishAndRevealsAfterPublish() throws Exception {
    HttpResponse<String> created =
        httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/v1/labs/8801/submissions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + student1Token)
                .POST(HttpRequest.BodyPublishers.ofString("{\"sourceCode\":\"int main(){}\"}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    int subId = objectMapper.readTree(created.body()).get("id").asInt();

    HttpResponse<String> beforePublish =
        httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/v1/submissions/" + subId))
                .header("Authorization", "Bearer " + student1Token)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    JsonNode beforeNode = objectMapper.readTree(beforePublish.body());
    assertTrue(beforeNode.get("publishedScoreTenths").isNull());
    assertTrue(beforeNode.get("publishedDeductions").isNull());

    jdbcTemplate.update(
        "INSERT INTO publications (lab_id, receipt) VALUES (8801, '{\"labId\":8801}')");

    HttpResponse<String> afterPublish =
        httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/v1/submissions/" + subId))
                .header("Authorization", "Bearer " + student1Token)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    JsonNode afterNode = objectMapper.readTree(afterPublish.body());
    assertEquals(0, afterNode.get("publishedScoreTenths").asInt());
    assertTrue(afterNode.get("publishedDeductions").isArray());
  }
}
