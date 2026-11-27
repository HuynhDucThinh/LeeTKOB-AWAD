package com.leetkob.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetkob.LeetKobApplication;
import com.leetkob.entity.User;
import com.leetkob.repository.ClassMembershipRepository;
import com.leetkob.repository.UserRepository;
import com.leetkob.security.jwt.JwtUtil;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(
    classes = LeetKobApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReviewQueueAndAppealsIntegrationTest {

  private static final String PAST_DEADLINE = "2020-01-01T00:00:00.000Z";

  @LocalServerPort private int port;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private UserRepository userRepository;

  @Autowired private ClassMembershipRepository classMembershipRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtUtil jwtUtil;

  @Autowired private ObjectMapper objectMapper;

  private HttpClient httpClient;

  private User adminUser;
  private User ta1User;
  private User ta2User;
  private User student1User;
  private User student2User;

  private String adminToken;
  private String ta1Token;
  private String ta2Token;
  private String student1Token;
  private String student2Token;

  private int labId;
  private int submission1Id;
  private int submission2Id;

  @BeforeEach
  void resetDatabase() {
    httpClient = HttpClient.newHttpClient();

    jdbcTemplate.update("DELETE FROM appeals");
    jdbcTemplate.update("DELETE FROM llm_call_logs");
    jdbcTemplate.update("DELETE FROM judge_jobs");
    jdbcTemplate.update("DELETE FROM submissions");
    jdbcTemplate.update("DELETE FROM publication_audit");
    jdbcTemplate.update("DELETE FROM published_marks");
    jdbcTemplate.update("DELETE FROM publications");
    jdbcTemplate.update("DELETE FROM review_items");
    jdbcTemplate.update("DELETE FROM counted_submissions");
    jdbcTemplate.update("DELETE FROM lab_constraints");
    jdbcTemplate.update("DELETE FROM lab_tests");
    jdbcTemplate.update("DELETE FROM ta_classes");
    jdbcTemplate.update("DELETE FROM labs");
    jdbcTemplate.update("DELETE FROM class_members");
    jdbcTemplate.update("DELETE FROM classes");
    jdbcTemplate.update("DELETE FROM oauth_accounts");
    jdbcTemplate.update("DELETE FROM users");

    classMembershipRepository.ensureClass(10, "CSC13114-A", "Intro to C++ — Class 10");
    classMembershipRepository.ensureClass(20, "CSC13114-B", "Intro to C++ — Class 20");

    adminUser = createUser("admin@leetkob.edu", "System Admin", "admin");
    ta1User = createUser("ta1@leetkob.edu", "TA Class 10", "ta");
    ta2User = createUser("ta2@leetkob.edu", "TA Class 20", "ta");
    student1User = createUser("student1@leetkob.edu", "Student 1", "student");
    student2User = createUser("student2@leetkob.edu", "Student 2", "student");

    classMembershipRepository.ensureMember(10, ta1User.getId(), "ta");
    classMembershipRepository.ensureMember(20, ta2User.getId(), "ta");
    classMembershipRepository.ensureMember(10, student1User.getId(), "student");
    classMembershipRepository.ensureMember(10, student2User.getId(), "student");

    classMembershipRepository.ensureTaClass(ta1User.getEmail(), 10);
    classMembershipRepository.ensureTaClass(ta2User.getEmail(), 20);

    adminToken = jwtUtil.generateToken(adminUser.getId(), adminUser.getEmail(), "admin");
    ta1Token = jwtUtil.generateToken(ta1User.getId(), ta1User.getEmail(), "ta");
    ta2Token = jwtUtil.generateToken(ta2User.getId(), ta2User.getEmail(), "ta");
    student1Token = jwtUtil.generateToken(student1User.getId(), student1User.getEmail(), "student");
    student2Token = jwtUtil.generateToken(student2User.getId(), student2User.getEmail(), "student");

    labId = 100;
    jdbcTemplate.update(
        "INSERT INTO labs (id, class_id, deadline, title, statement_md, cost_cap_usd) "
            + "VALUES (?, 10, ?, 'Merge Sort', '# Sort without std::sort', 5.0)",
        labId,
        PAST_DEADLINE);

    jdbcTemplate.update(
        "INSERT INTO lab_constraints (lab_id, constraint_id, constraint_type, description, deduction_tenths) "
            + "VALUES (?, 'C1', 'hard', 'Do not use std::sort', 30), "
            + "(?, 'C2', 'semantic', 'Must use divide and conquer merge sort', 20)",
        labId,
        labId);

    submission1Id = 1001;
    submission2Id = 1002;
    jdbcTemplate.update(
        "INSERT INTO submissions (id, lab_id, student_id, source_code, stripped_code, judge_status, score_tenths, passed_tests, total_tests, trace_id) "
            + "VALUES (?, ?, ?, 'int main(){ std::sort(a,a+n); }', 'int main(){ std::sort(a,a+n); }', 'AC', 100, 5, 5, 'tr-1001'), "
            + "(?, ?, ?, 'int main(){ mergeSort(a,0,n); }', 'int main(){ mergeSort(a,0,n); }', 'AC', 100, 5, 5, 'tr-1002')",
        submission1Id,
        labId,
        student1User.getId(),
        submission2Id,
        labId,
        student2User.getId());

    jdbcTemplate.update(
        "INSERT INTO counted_submissions (id, lab_id, student_id, judge_status, score_tenths) "
            + "VALUES (?, ?, ?, 'AC', 70), (?, ?, ?, 'AC', 100)",
        submission1Id,
        labId,
        student1User.getId().toString(),
        submission2Id,
        labId,
        student2User.getId().toString());
  }

  private User createUser(String email, String fullName, String role) {
    return userRepository.saveAndFlush(
        User.builder()
            .email(email)
            .fullName(fullName)
            .role(role)
            .passwordHash(passwordEncoder.encode("Password@123"))
            .build());
  }

  private HttpResponse<String> send(String method, String path, String bearerToken, String body)
      throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + port + path));
    if (bearerToken != null) {
      builder.header("Authorization", "Bearer " + bearerToken);
    }
    if (body != null) {
      builder.header("Content-Type", "application/json");
      builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
    } else {
      builder.method(method, HttpRequest.BodyPublishers.noBody());
    }
    return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  private void assertProblem(
      HttpResponse<String> response, int expectedStatus, String expectedSlug, String expectedPath)
      throws Exception {
    assertEquals(expectedStatus, response.statusCode());
    assertTrue(
        response
            .headers()
            .firstValue("Content-Type")
            .orElse("")
            .startsWith("application/problem+json"));
    assertEquals("no-store", response.headers().firstValue("Cache-Control").orElse(""));
    assertEquals("nosniff", response.headers().firstValue("X-Content-Type-Options").orElse(""));

    JsonNode node = objectMapper.readTree(response.body());
    assertEquals("https://leetkob.example/problems/" + expectedSlug, node.get("type").asText());
    assertEquals(expectedStatus, node.get("status").asInt());
    assertFalse(node.get("title").asText().isBlank());
    assertFalse(node.get("detail").asText().isBlank());
    if (expectedPath != null) {
      assertEquals(expectedPath, node.get("instance").asText());
    }
  }

  @Test
  void reviewQueueEnforcesT1IsolationAndPublishBlocksUntilAllQueuedItemsDecided() throws Exception {
    // 1. Insert queued violation item, queued sampled-pass item, and non-queued pass item
    jdbcTemplate.update(
        "INSERT INTO review_items (submission_id, constraint_id, verdict, source, evidence_json, in_queue, sampled_pass, decision) "
            + "VALUES (?, 'C1', 'violation', 'hard_rule', '[{\"line\":1,\"text\":\"std::sort(a,a+n);\"}]', TRUE, FALSE, NULL), "
            + "(?, 'C2', 'pass', 'llm', '[]', TRUE, TRUE, NULL), "
            + "(?, 'C1', 'pass', 'hard_rule', '[]', FALSE, FALSE, NULL)",
        submission1Id,
        submission2Id,
        submission2Id);

    // Unauthenticated -> 401
    assertProblem(
        send("GET", "/v1/labs/" + labId + "/review-queue", null, null),
        401,
        "authentication-required",
        "/v1/labs/" + labId + "/review-queue");

    // Student & admin -> 403
    assertProblem(
        send("GET", "/v1/labs/" + labId + "/review-queue", student1Token, null),
        403,
        "forbidden",
        "/v1/labs/" + labId + "/review-queue");
    assertProblem(
        send("GET", "/v1/labs/" + labId + "/review-queue", adminToken, null),
        403,
        "forbidden",
        "/v1/labs/" + labId + "/review-queue");

    // Foreign TA -> identical 404 as missing lab (T1)
    HttpResponse<String> foreignQueue =
        send("GET", "/v1/labs/" + labId + "/review-queue", ta2Token, null);
    HttpResponse<String> missingQueue = send("GET", "/v1/labs/999999/review-queue", ta2Token, null);
    assertProblem(foreignQueue, 404, "not-found", "/v1/labs/" + labId + "/review-queue");
    assertProblem(missingQueue, 404, "not-found", "/v1/labs/999999/review-queue");
    assertEquals(
        objectMapper.readTree(foreignQueue.body()).get("detail").asText(),
        objectMapper.readTree(missingQueue.body()).get("detail").asText());

    // Own TA gets only the 2 in_queue = TRUE items
    HttpResponse<String> queueResp =
        send("GET", "/v1/labs/" + labId + "/review-queue", ta1Token, null);
    assertEquals(200, queueResp.statusCode());
    JsonNode queue = objectMapper.readTree(queueResp.body());
    assertEquals(2, queue.get("total").asInt());
    assertEquals(2, queue.get("items").size());

    // Publish is blocked with 409 reviews-pending while queued items are undecided (H2/H3)
    assertProblem(
        send("POST", "/v1/labs/" + labId + "/publish", ta1Token, "{}"),
        409,
        "reviews-pending",
        "/v1/labs/" + labId + "/publish");

    // Foreign TA cannot decide -> 404
    assertProblem(
        send(
            "PUT",
            "/v1/labs/" + labId + "/review-queue/" + submission1Id + "/C1",
            ta2Token,
            "{\"decision\":\"violation\"}"),
        404,
        "not-found",
        "/v1/labs/" + labId + "/review-queue/" + submission1Id + "/C1");

    // Invalid decision -> 400 validation
    assertProblem(
        send(
            "PUT",
            "/v1/labs/" + labId + "/review-queue/" + submission1Id + "/C1",
            ta1Token,
            "{\"decision\":\"unsure\"}"),
        400,
        "validation",
        "/v1/labs/" + labId + "/review-queue/" + submission1Id + "/C1");

    // Own TA decides C1 -> violation
    HttpResponse<String> decide1 =
        send(
            "PUT",
            "/v1/labs/" + labId + "/review-queue/" + submission1Id + "/C1",
            ta1Token,
            "{\"decision\":\"violation\"}");
    assertEquals(200, decide1.statusCode());
    JsonNode decidedItem1 = objectMapper.readTree(decide1.body());
    assertEquals("violation", decidedItem1.get("decision").asText());
    assertEquals("ta1@leetkob.edu", decidedItem1.get("decidedBy").asText());
    assertNotNull(Instant.parse(decidedItem1.get("decidedAt").asText()));
    assertEquals(1, decidedItem1.get("evidence").size());
    assertEquals(1, decidedItem1.get("evidence").get(0).get("line").asInt());

    // Still 1 sampled_pass item undecided -> Publish still blocked 409
    assertProblem(
        send("POST", "/v1/labs/" + labId + "/publish", ta1Token, "{}"),
        409,
        "reviews-pending",
        "/v1/labs/" + labId + "/publish");

    // Own TA decides sampled_pass C2 -> pass
    HttpResponse<String> decide2 =
        send(
            "PUT",
            "/v1/labs/" + labId + "/review-queue/" + submission2Id + "/C2",
            ta1Token,
            "{\"decision\":\"pass\"}");
    assertEquals(200, decide2.statusCode());
    assertTrue(objectMapper.readTree(decide2.body()).get("sampledPass").asBoolean());

    // Now Publish succeeds (non-queued pass item does NOT block publish!)
    HttpResponse<String> publishResp =
        send("POST", "/v1/labs/" + labId + "/publish", ta1Token, "{}");
    assertEquals(200, publishResp.statusCode());
    JsonNode receipt = objectMapper.readTree(publishResp.body());
    assertEquals("published", receipt.get("status").asText());
    assertEquals(2, receipt.get("submissionCount").asInt());
  }

  @Test
  void appealsEnforceA1BeforePublishA2OwnershipAndA3AcceptedUpdatesPublishedMarks()
      throws Exception {
    // A1: Appeal before publish -> 409 appeal-before-publish
    String appealBody = "{\"constraintId\":\"C1\",\"reason\":\"I implemented my own merge sort\"}";
    assertProblem(
        send("POST", "/v1/submissions/" + submission1Id + "/appeals", student1Token, appealBody),
        409,
        "appeal-before-publish",
        "/v1/submissions/" + submission1Id + "/appeals");

    // A2: Student2 attempting to appeal Student1's submission -> 404 not-found
    assertProblem(
        send("POST", "/v1/submissions/" + submission1Id + "/appeals", student2Token, appealBody),
        404,
        "not-found",
        "/v1/submissions/" + submission1Id + "/appeals");

    // TA calling student appeal creation -> 403 forbidden
    assertProblem(
        send("POST", "/v1/submissions/" + submission1Id + "/appeals", ta1Token, appealBody),
        403,
        "forbidden",
        "/v1/submissions/" + submission1Id + "/appeals");

    // Publish the lab first (with decided violation on submission1 C1, score_tenths = 70)
    jdbcTemplate.update(
        "INSERT INTO review_items (submission_id, constraint_id, verdict, source, in_queue, sampled_pass, decision) "
            + "VALUES (?, 'C1', 'violation', 'hard_rule', TRUE, FALSE, 'violation')",
        submission1Id);
    HttpResponse<String> publishResp =
        send("POST", "/v1/labs/" + labId + "/publish", ta1Token, "{}");
    assertEquals(200, publishResp.statusCode());

    // Student1 creates appeal after publish -> 201
    HttpResponse<String> createAppealResp =
        send("POST", "/v1/submissions/" + submission1Id + "/appeals", student1Token, appealBody);
    assertEquals(201, createAppealResp.statusCode());
    JsonNode appeal = objectMapper.readTree(createAppealResp.body());
    long appealId = appeal.get("id").asLong();
    assertEquals(labId, appeal.get("labId").asInt());
    assertEquals(submission1Id, appeal.get("submissionId").asInt());
    assertEquals("C1", appeal.get("constraintId").asText());
    assertEquals(30, appeal.get("deductionTenths").asInt());
    assertEquals("pending", appeal.get("status").asText());

    // Duplicate appeal for same (submission, constraint) -> 409 appeal-already-exists
    assertProblem(
        send("POST", "/v1/submissions/" + submission1Id + "/appeals", student1Token, appealBody),
        409,
        "appeal-already-exists",
        "/v1/submissions/" + submission1Id + "/appeals");

    // Listing appeals: Student1 sees 1, Student2 sees 0, TA1 sees 1, Foreign TA2 gets 404
    assertEquals(
        1,
        objectMapper
            .readTree(send("GET", "/v1/labs/" + labId + "/appeals", student1Token, null).body())
            .size());
    assertEquals(
        0,
        objectMapper
            .readTree(send("GET", "/v1/labs/" + labId + "/appeals", student2Token, null).body())
            .size());
    assertEquals(
        1,
        objectMapper
            .readTree(send("GET", "/v1/labs/" + labId + "/appeals", ta1Token, null).body())
            .size());
    assertProblem(
        send("GET", "/v1/labs/" + labId + "/appeals", ta2Token, null),
        404,
        "not-found",
        "/v1/labs/" + labId + "/appeals");

    // Foreign TA deciding appeal -> 404; Student deciding -> 403; Blank reason -> 400 validation
    String acceptBody =
        "{\"status\":\"accepted\",\"decisionReason\":\"Confirmed custom sort function\"}";
    assertProblem(
        send("PUT", "/v1/appeals/" + appealId + "/decision", ta2Token, acceptBody),
        404,
        "not-found",
        "/v1/appeals/" + appealId + "/decision");
    assertProblem(
        send("PUT", "/v1/appeals/" + appealId + "/decision", student1Token, acceptBody),
        403,
        "forbidden",
        "/v1/appeals/" + appealId + "/decision");
    assertProblem(
        send(
            "PUT",
            "/v1/appeals/" + appealId + "/decision",
            ta1Token,
            "{\"status\":\"accepted\",\"decisionReason\":\"   \"}"),
        400,
        "validation",
        "/v1/appeals/" + appealId + "/decision");

    // A3: Own TA accepts appeal -> saves decidedBy/decidedAt/decisionReason & updates
    // published_marks (70 + 30 = 100)
    HttpResponse<String> decideResp =
        send("PUT", "/v1/appeals/" + appealId + "/decision", ta1Token, acceptBody);
    assertEquals(200, decideResp.statusCode());
    JsonNode decided = objectMapper.readTree(decideResp.body());
    assertEquals("accepted", decided.get("status").asText());
    assertEquals("Confirmed custom sort function", decided.get("decisionReason").asText());
    assertEquals("ta1@leetkob.edu", decided.get("decidedBy").asText());
    assertNotNull(Instant.parse(decided.get("decidedAt").asText()));

    Integer updatedPublishedScore =
        jdbcTemplate.queryForObject(
            "SELECT score_tenths FROM published_marks WHERE lab_id = ? AND submission_id = ?",
            Integer.class,
            labId,
            submission1Id);
    assertEquals(100, updatedPublishedScore);
  }
}
