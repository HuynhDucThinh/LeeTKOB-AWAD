package com.leetkob.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetkob.LeetKobApplication;
import com.leetkob.entities.User;
import com.leetkob.repositories.ClassMembershipRepository;
import com.leetkob.repositories.UserRepository;
import com.leetkob.security.jwt.JwtUtil;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
class AdminClassesAndLabsIntegrationTest {

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
    student1User = createUser("student1@leetkob.edu", "Student Class 10", "student");
    student2User = createUser("student2@leetkob.edu", "Student Class 20", "student");

    classMembershipRepository.ensureMember(10, ta1User.getId(), "ta");
    classMembershipRepository.ensureMember(20, ta2User.getId(), "ta");
    classMembershipRepository.ensureMember(10, student1User.getId(), "student");
    classMembershipRepository.ensureMember(20, student2User.getId(), "student");

    classMembershipRepository.ensureTaClass(ta1User.getEmail(), 10);
    classMembershipRepository.ensureTaClass(ta2User.getEmail(), 20);

    adminToken = jwtUtil.generateToken(adminUser.getId(), adminUser.getEmail(), "admin");
    ta1Token = jwtUtil.generateToken(ta1User.getId(), ta1User.getEmail(), "ta");
    ta2Token = jwtUtil.generateToken(ta2User.getId(), ta2User.getEmail(), "ta");
    student1Token = jwtUtil.generateToken(student1User.getId(), student1User.getEmail(), "student");
    student2Token = jwtUtil.generateToken(student2User.getId(), student2User.getEmail(), "student");
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
  void unauthenticatedRequestsToProtectedV1EndpointsReturn401Problem() throws Exception {
    assertProblem(
        send("GET", "/v1/admin/classes", null, null),
        401,
        "authentication-required",
        "/v1/admin/classes");
    assertProblem(
        send("GET", "/v1/classes", null, null), 401, "authentication-required", "/v1/classes");
    assertProblem(send("GET", "/v1/labs", null, null), 401, "authentication-required", "/v1/labs");
  }

  @Test
  void nonAdminCallingAdminEndpointsReceives403Forbidden() throws Exception {
    assertProblem(
        send("GET", "/v1/admin/classes", ta1Token, null), 403, "forbidden", "/v1/admin/classes");
    assertProblem(
        send("POST", "/v1/admin/classes", student1Token, "{\"name\":\"CSC13114-C\"}"),
        403,
        "forbidden",
        "/v1/admin/classes");
    assertProblem(
        send("PUT", "/v1/admin/classes/10/members", ta1Token, "{\"taIds\":[],\"studentIds\":[]}"),
        403,
        "forbidden",
        "/v1/admin/classes/10/members");
    assertProblem(
        send("GET", "/v1/admin/users", student1Token, null), 403, "forbidden", "/v1/admin/users");
    assertProblem(
        send(
            "POST",
            "/v1/admin/users",
            ta1Token,
            "{\"username\":\"new@leetkob.edu\",\"displayName\":\"New\",\"role\":\"student\",\"password\":\"Pass@123\"}"),
        403,
        "forbidden",
        "/v1/admin/users");
  }

  @Test
  void adminCanCreateClassListClassesAndSyncMembersAcrossClassMembersAndTaClasses()
      throws Exception {
    HttpResponse<String> createResp =
        send("POST", "/v1/admin/classes", adminToken, "{\"name\":\"CSC13114-C\"}");
    assertEquals(201, createResp.statusCode());
    JsonNode created = objectMapper.readTree(createResp.body());
    int newClassId = created.get("id").asInt();
    assertTrue(newClassId > 0);
    assertEquals("CSC13114-C", created.get("name").asText());
    assertEquals(0, created.get("taIds").size());
    assertEquals(0, created.get("studentIds").size());
    assertEquals(0, created.get("labCount").asInt());

    String updateBody =
        objectMapper.writeValueAsString(
            java.util.Map.of(
                "taIds", List.of(ta2User.getId().toString()),
                "studentIds", List.of(student1User.getId().toString())));
    HttpResponse<String> updateResp =
        send("PUT", "/v1/admin/classes/" + newClassId + "/members", adminToken, updateBody);
    assertEquals(200, updateResp.statusCode());
    JsonNode updated = objectMapper.readTree(updateResp.body());
    assertEquals(List.of(ta2User.getId().toString()), toStringList(updated.get("taIds")));
    assertEquals(List.of(student1User.getId().toString()), toStringList(updated.get("studentIds")));

    Integer taClassCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ta_classes WHERE class_id = ? AND ta_id = ?",
            Integer.class,
            newClassId,
            ta2User.getEmail());
    assertEquals(1, taClassCount);

    Integer classMemberCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM class_members WHERE class_id = ?", Integer.class, newClassId);
    assertEquals(2, classMemberCount);

    // Replace TA2 with TA1 and verify TA2 is removed from ta_classes
    String reassignBody =
        objectMapper.writeValueAsString(
            java.util.Map.of(
                "taIds", List.of(ta1User.getId().toString()), "studentIds", List.of()));
    HttpResponse<String> reassignResp =
        send("PUT", "/v1/admin/classes/" + newClassId + "/members", adminToken, reassignBody);
    assertEquals(200, reassignResp.statusCode());

    assertEquals(
        0,
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ta_classes WHERE class_id = ? AND ta_id = ?",
            Integer.class,
            newClassId,
            ta2User.getEmail()));
    assertEquals(
        1,
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ta_classes WHERE class_id = ? AND ta_id = ?",
            Integer.class,
            newClassId,
            ta1User.getEmail()));

    HttpResponse<String> listResp = send("GET", "/v1/admin/classes", adminToken, null);
    assertEquals(200, listResp.statusCode());
    JsonNode classes = objectMapper.readTree(listResp.body());
    assertEquals(3, classes.size());

    // Unknown class returns 404
    assertProblem(
        send("PUT", "/v1/admin/classes/999999/members", adminToken, reassignBody),
        404,
        "not-found",
        "/v1/admin/classes/999999/members");
  }

  @Test
  void adminCanCreateAndFilterUsersAndDuplicateEmailReturns409() throws Exception {
    String createPayload =
        """
        {
          "username": "ta3@leetkob.edu",
          "displayName": "TA Three",
          "role": "ta",
          "password": "Ta3@Password"
        }
        """;
    HttpResponse<String> createResp = send("POST", "/v1/admin/users", adminToken, createPayload);
    assertEquals(201, createResp.statusCode());
    JsonNode created = objectMapper.readTree(createResp.body());
    assertNotNull(UUID.fromString(created.get("id").asText()));
    assertEquals("ta3@leetkob.edu", created.get("username").asText());
    assertEquals("TA Three", created.get("displayName").asText());
    assertEquals("ta", created.get("role").asText());
    assertEquals(0, created.get("classIds").size());
    assertNotNull(Instant.parse(created.get("createdAt").asText()));

    // Duplicate email -> 409 conflict
    assertProblem(
        send("POST", "/v1/admin/users", adminToken, createPayload),
        409,
        "conflict",
        "/v1/admin/users");

    HttpResponse<String> taUsersResp = send("GET", "/v1/admin/users?role=ta", adminToken, null);
    assertEquals(200, taUsersResp.statusCode());
    JsonNode taUsers = objectMapper.readTree(taUsersResp.body());
    assertEquals(3, taUsers.size());
    for (JsonNode user : taUsers) {
      assertEquals("ta", user.get("role").asText());
    }
  }

  @Test
  void getClassesReturnsScopedClassesForTaAndStudentAndAllForAdmin() throws Exception {
    HttpResponse<String> ta1Resp = send("GET", "/v1/classes", ta1Token, null);
    assertEquals(200, ta1Resp.statusCode());
    JsonNode ta1Classes = objectMapper.readTree(ta1Resp.body());
    assertEquals(1, ta1Classes.size());
    assertEquals(10, ta1Classes.get(0).get("id").asInt());

    HttpResponse<String> student1Resp = send("GET", "/v1/classes", student1Token, null);
    assertEquals(200, student1Resp.statusCode());
    JsonNode student1Classes = objectMapper.readTree(student1Resp.body());
    assertEquals(1, student1Classes.size());
    assertEquals(10, student1Classes.get(0).get("id").asInt());

    HttpResponse<String> adminResp = send("GET", "/v1/classes", adminToken, null);
    assertEquals(200, adminResp.statusCode());
    JsonNode adminClasses = objectMapper.readTree(adminResp.body());
    assertEquals(2, adminClasses.size());
  }

  @Test
  void taLabCrudEnforcesT1ClassIsolationAndStudentSampleOnlyVisibility() throws Exception {
    String createLabPayload =
        """
        {
          "classId": 10,
          "title": "Lab 1: Merge Sort",
          "deadline": "2026-12-01T23:59:00.000Z",
          "costCapUsd": 5.0,
          "statementMd": "# Merge Sort\\nImplement merge sort without std::sort.",
          "tests": [
            {
              "name": "Sample 1",
              "input": "3\\n3 1 2\\n",
              "expectedOutput": "1 2 3\\n",
              "timeLimitMs": 2000,
              "memoryLimitMb": 256,
              "isSample": true
            },
            {
              "name": "Hidden 1",
              "input": "5\\n5 4 3 2 1\\n",
              "expectedOutput": "1 2 3 4 5\\n",
              "timeLimitMs": 2000,
              "memoryLimitMb": 256,
              "isSample": false
            }
          ],
          "constraints": [
            {
              "constraintId": "C1",
              "type": "hard",
              "description": "Do not use std::sort",
              "deductionTenths": 50
            }
          ]
        }
        """;

    // Student & admin cannot create labs -> 403
    assertProblem(
        send("POST", "/v1/labs", student1Token, createLabPayload), 403, "forbidden", "/v1/labs");
    assertProblem(
        send("POST", "/v1/labs", adminToken, createLabPayload), 403, "forbidden", "/v1/labs");

    // Foreign TA (class 20) cannot create lab for class 10 -> 404 (no leak)
    assertProblem(
        send("POST", "/v1/labs", ta2Token, createLabPayload), 404, "not-found", "/v1/labs");

    // Own TA (class 10) creates lab -> 201
    HttpResponse<String> createResp = send("POST", "/v1/labs", ta1Token, createLabPayload);
    assertEquals(201, createResp.statusCode());
    JsonNode createdLab = objectMapper.readTree(createResp.body());
    int labId = createdLab.get("id").asInt();
    assertTrue(labId > 0);
    assertEquals(10, createdLab.get("classId").asInt());
    assertEquals("Lab 1: Merge Sort", createdLab.get("title").asText());
    assertEquals(2, createdLab.get("tests").size());
    assertEquals(1, createdLab.get("constraints").size());
    assertFalse(createdLab.get("isPublished").asBoolean());

    // Own TA lists and gets full detail (sample + hidden tests)
    HttpResponse<String> ta1ListResp = send("GET", "/v1/labs", ta1Token, null);
    assertEquals(200, ta1ListResp.statusCode());
    assertEquals(1, objectMapper.readTree(ta1ListResp.body()).size());

    HttpResponse<String> ta1DetailResp = send("GET", "/v1/labs/" + labId, ta1Token, null);
    assertEquals(200, ta1DetailResp.statusCode());
    assertEquals(2, objectMapper.readTree(ta1DetailResp.body()).get("tests").size());

    // Foreign TA sees empty list and gets identical 404 on GET and PUT as missing lab
    HttpResponse<String> ta2ListResp = send("GET", "/v1/labs", ta2Token, null);
    assertEquals(200, ta2ListResp.statusCode());
    assertEquals(0, objectMapper.readTree(ta2ListResp.body()).size());

    HttpResponse<String> ta2ForeignGet = send("GET", "/v1/labs/" + labId, ta2Token, null);
    HttpResponse<String> ta2MissingGet = send("GET", "/v1/labs/999999", ta2Token, null);
    assertProblem(ta2ForeignGet, 404, "not-found", "/v1/labs/" + labId);
    assertProblem(ta2MissingGet, 404, "not-found", "/v1/labs/999999");
    assertEquals(
        objectMapper.readTree(ta2ForeignGet.body()).get("detail").asText(),
        objectMapper.readTree(ta2MissingGet.body()).get("detail").asText());

    assertProblem(
        send("PUT", "/v1/labs/" + labId, ta2Token, createLabPayload),
        404,
        "not-found",
        "/v1/labs/" + labId);

    // Student in class 10 sees lab detail with ONLY sample tests (hidden tests stripped)
    HttpResponse<String> student1DetailResp = send("GET", "/v1/labs/" + labId, student1Token, null);
    assertEquals(200, student1DetailResp.statusCode());
    JsonNode studentLab = objectMapper.readTree(student1DetailResp.body());
    assertEquals(1, studentLab.get("tests").size());
    assertTrue(studentLab.get("tests").get(0).get("isSample").asBoolean());
    assertEquals("Sample 1", studentLab.get("tests").get(0).get("name").asText());

    // Student in class 20 gets 404; Student in class 10 calling PUT gets 403
    assertProblem(
        send("GET", "/v1/labs/" + labId, student2Token, null),
        404,
        "not-found",
        "/v1/labs/" + labId);
    assertProblem(
        send("PUT", "/v1/labs/" + labId, student1Token, createLabPayload),
        403,
        "forbidden",
        "/v1/labs/" + labId);

    // Own TA updates lab -> 200
    String updatePayload = createLabPayload.replace("Lab 1: Merge Sort", "Lab 1: Merge Sort v2");
    HttpResponse<String> updateResp = send("PUT", "/v1/labs/" + labId, ta1Token, updatePayload);
    assertEquals(200, updateResp.statusCode());
    assertEquals(
        "Lab 1: Merge Sort v2", objectMapper.readTree(updateResp.body()).get("title").asText());
  }

  @Test
  void versionEndpointIsPublicAndReturnsVersionInfo() throws Exception {
    HttpResponse<String> resp = send("GET", "/v1/version", null, null);
    assertEquals(200, resp.statusCode());
    JsonNode body = objectMapper.readTree(resp.body());
    assertFalse(body.get("commit").asText().isBlank());
    assertTrue(body.has("tag"));
    assertNotNull(Instant.parse(body.get("deployedAt").asText()));
  }

  private List<String> toStringList(JsonNode arrayNode) {
    List<String> result = new ArrayList<>();
    for (JsonNode item : arrayNode) {
      result.add(item.asText());
    }
    return result;
  }
}
