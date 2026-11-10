package vn.edu.hcmus.leetkob.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.edu.hcmus.leetkob.dto.PublicationReceipt;
import vn.edu.hcmus.leetkob.security.AuthResolver;
import vn.edu.hcmus.leetkob.security.Principal;
import vn.edu.hcmus.leetkob.service.PublishService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PublishEndpointContractTest {

  private static final String NOW_ISO = "2026-11-05T08:00:00.000Z";
  private static final Instant DEFAULT_NOW = Instant.parse(NOW_ISO);
  private static final Pattern LEAK_PATTERN =
      Pattern.compile("SELECT |INSERT |private\\.db|secret-fixture|Exception|\\.java:\\d");

  private static final Map<String, Principal> IDENTITIES =
      Map.of(
          "own", new Principal("ta-own", "ta"),
          "colleague", new Principal("ta-colleague", "ta"),
          "foreign", new Principal("ta-foreign", "ta"),
          "student", new Principal("student-1", "student"),
          "admin", new Principal("admin-1", "admin"));

  private static final AtomicReference<Instant> CURRENT_TIME = new AtomicReference<>(DEFAULT_NOW);
  private static final AtomicReference<AuthResolver> DELEGATE_AUTH = new AtomicReference<>();

  @TestConfiguration
  static class ContractTestConfig {

    @Bean
    @Primary
    Clock controllableClock() {
      return new Clock() {
        @Override
        public ZoneId getZone() {
          return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
          return this;
        }

        @Override
        public Instant instant() {
          return CURRENT_TIME.get();
        }
      };
    }

    @Bean
    @Primary
    AuthResolver controllableAuthResolver() {
      return header -> DELEGATE_AUTH.get().authenticate(header);
    }
  }

  @LocalServerPort private int port;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private PublishService publishService;

  @Autowired private ObjectMapper objectMapper;

  private HttpClient httpClient;

  @BeforeEach
  void resetFixture() {
    httpClient = HttpClient.newHttpClient();
    CURRENT_TIME.set(DEFAULT_NOW);
    DELEGATE_AUTH.set(
        header -> {
          if (header == null || !header.startsWith("Bearer ")) {
            return Optional.empty();
          }
          return Optional.ofNullable(IDENTITIES.get(header.substring("Bearer ".length())));
        });
    publishService.setBeforeTransactionalAccessHook(null);
    publishService.setSimulateAuditFailure(false);

    jdbcTemplate.update("DELETE FROM publication_audit");
    jdbcTemplate.update("DELETE FROM published_marks");
    jdbcTemplate.update("DELETE FROM publications");
    jdbcTemplate.update("DELETE FROM review_items");
    jdbcTemplate.update("DELETE FROM counted_submissions");
    jdbcTemplate.update("DELETE FROM ta_classes");
    jdbcTemplate.update("DELETE FROM labs");

    jdbcTemplate.update(
        "INSERT INTO labs (id, class_id, deadline) VALUES (1, 10, ?), (2, 20, ?)",
        NOW_ISO,
        NOW_ISO);
    jdbcTemplate.update(
        "INSERT INTO ta_classes (ta_id, class_id) VALUES ('ta-own', 10), ('ta-colleague', 10), ('ta-foreign', 20)");
    jdbcTemplate.update(
        "INSERT INTO counted_submissions (id, lab_id, student_id, judge_status, score_tenths) VALUES (11, 1, 'student-1', 'AC', 70), (12, 1, 'student-2', 'WA', 0)");
    jdbcTemplate.update(
        "INSERT INTO review_items (submission_id, constraint_id, decision) VALUES (11, 'C1', 'violation'), (11, 'C2', 'pass')");
  }

  private HttpResponse<String> sendRequest(
      String method,
      String path,
      String token,
      String contentType,
      String contentEncoding,
      String body)
      throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + port + path));

    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }
    if (contentType != null) {
      builder.header("Content-Type", contentType);
    }
    if (contentEncoding != null) {
      builder.header("Content-Encoding", contentEncoding);
    }

    if ("GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
      builder.method(method, HttpRequest.BodyPublishers.noBody());
    } else {
      byte[] payload = (body != null ? body : "{}").getBytes(StandardCharsets.UTF_8);
      builder.method(method, HttpRequest.BodyPublishers.ofByteArray(payload));
    }

    return httpClient.send(
        builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  private HttpResponse<String> postPublish(String path, String token, String body)
      throws Exception {
    return sendRequest("POST", path, token, "application/json", null, body);
  }

  private Map<String, Object> assertProblem(
      HttpResponse<String> response, int expectedStatus, String expectedSlug, String expectedTitle)
      throws Exception {
    assertEquals(expectedStatus, response.statusCode());
    assertEquals(
        "application/problem+json", response.headers().firstValue("content-type").orElse(""));
    assertEquals("no-store", response.headers().firstValue("cache-control").orElse(""));
    assertEquals("nosniff", response.headers().firstValue("x-content-type-options").orElse(""));

    String rawBody = response.body();
    assertFalse(
        LEAK_PATTERN.matcher(rawBody).find(), "Error body leaked internal details: " + rawBody);

    Map<String, Object> body =
        objectMapper.readValue(rawBody, new TypeReference<Map<String, Object>>() {});
    assertEquals("https://leetkob.example/problems/" + expectedSlug, body.get("type"));
    assertEquals(expectedTitle, body.get("title"));
    assertEquals(expectedStatus, body.get("status"));
    assertTrue(body.get("detail") instanceof String && !((String) body.get("detail")).isEmpty());
    assertTrue(body.get("instance") instanceof String);

    Set<String> expectedKeys =
        "validation".equals(expectedSlug)
            ? Set.of("type", "title", "status", "detail", "instance", "errors")
            : Set.of("type", "title", "status", "detail", "instance");
    assertEquals(expectedKeys, body.keySet());
    return body;
  }

  private void assertNoPublication() {
    for (String table : List.of("publications", "published_marks", "publication_audit")) {
      Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
      assertEquals(0, count);
    }
  }

  @Test
  void publishSnapshotsPreparedMarksAndRecordsSingleAuditReceipt() throws Exception {
    HttpResponse<String> response = postPublish("/v1/labs/1/publish", "own", "{}");

    assertEquals(200, response.statusCode());
    assertEquals("application/json", response.headers().firstValue("content-type").orElse(""));
    assertEquals("no-store", response.headers().firstValue("cache-control").orElse(""));
    assertEquals("nosniff", response.headers().firstValue("x-content-type-options").orElse(""));
    assertTrue(response.headers().firstValue("idempotent-replayed").isEmpty());

    PublicationReceipt receipt = objectMapper.readValue(response.body(), PublicationReceipt.class);
    assertEquals(new PublicationReceipt(1, "published", NOW_ISO, "ta-own", 2), receipt);

    List<Map<String, Object>> marks =
        jdbcTemplate.queryForList(
            "SELECT submission_id, score_tenths FROM published_marks ORDER BY submission_id");
    assertEquals(2, marks.size());
    assertEquals(11, marks.get(0).get("submission_id"));
    assertEquals(70, marks.get(0).get("score_tenths"));
    assertEquals(12, marks.get(1).get("submission_id"));
    assertEquals(0, marks.get(1).get("score_tenths"));

    Map<String, Object> audit = jdbcTemplate.queryForMap("SELECT * FROM publication_audit");
    assertEquals(1, audit.get("lab_id"));
    assertEquals("ta-own", audit.get("actor_id"));
    assertEquals(NOW_ISO, audit.get("at"));
    assertEquals("lab.published", audit.get("event"));
  }

  @Test
  void repeatedPostIs200AndByteIdenticalAcrossColleagueAndClock() throws Exception {
    HttpResponse<String> first = postPublish("/v1/labs/1/publish", "own", "{}");
    assertEquals(200, first.statusCode());
    String originalBody = first.body();

    CURRENT_TIME.set(Instant.parse("2026-11-06T00:00:00.000Z"));
    jdbcTemplate.update("UPDATE counted_submissions SET score_tenths = 100");

    HttpResponse<String> second =
        postPublish(
            "/v1/labs/1/publish", "colleague", "{\"publishedBy\":\"imposter\",\"score\":100}");
    assertEquals(200, second.statusCode());
    assertEquals("true", second.headers().firstValue("idempotent-replayed").orElse(""));
    assertEquals(originalBody, second.body());

    Integer auditCount =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM publication_audit", Integer.class);
    assertEquals(1, auditCount);

    Integer savedScore =
        jdbcTemplate.queryForObject(
            "SELECT score_tenths FROM published_marks WHERE submission_id = 11", Integer.class);
    assertEquals(70, savedScore);
  }

  @Test
  void missingOrInvalidCredentialsReturn401WithBearerChallenge() throws Exception {
    HttpResponse<String> invalidToken = postPublish("/v1/labs/1/publish", "invalid", "{}");
    HttpResponse<String> missingHeader = postPublish("/v1/labs/1/publish", null, "{}");

    for (HttpResponse<String> res : List.of(invalidToken, missingHeader)) {
      assertProblem(res, 401, "authentication-required", "Authentication required");
      assertEquals(
          "Bearer realm=\"leetkob\"", res.headers().firstValue("www-authenticate").orElse(""));
    }
    assertNoPublication();
  }

  @Test
  void studentsAndAdminsAreForbidden403() throws Exception {
    for (String roleToken : List.of("student", "admin")) {
      HttpResponse<String> res = postPublish("/v1/labs/1/publish", roleToken, "{}");
      assertProblem(res, 403, "forbidden", "Forbidden");
    }
    assertNoPublication();
  }

  @Test
  void foreignTaAndMissingLabReturnIdentical404BeforeBodyParse() throws Exception {
    HttpResponse<String> foreignRes =
        sendRequest("POST", "/v1/labs/1/publish", "foreign", "text/plain", null, "not-json");
    Map<String, Object> foreignProblem = assertProblem(foreignRes, 404, "not-found", "Not found");

    HttpResponse<String> missingRes = postPublish("/v1/labs/999/publish", "own", "{}");
    Map<String, Object> missingProblem = assertProblem(missingRes, 404, "not-found", "Not found");

    foreignProblem.put("instance", "");
    missingProblem.put("instance", "");
    assertEquals(missingProblem, foreignProblem);
    assertNoPublication();
  }

  @Test
  void publishedReceiptDoesNotBypassRevokedOrForeignOwnership() throws Exception {
    assertEquals(200, postPublish("/v1/labs/1/publish", "own", "{}").statusCode());

    jdbcTemplate.update("DELETE FROM ta_classes WHERE ta_id = 'ta-own'");
    assertProblem(postPublish("/v1/labs/1/publish", "own", "{}"), 404, "not-found", "Not found");
    assertProblem(
        postPublish("/v1/labs/1/publish", "foreign", "{}"), 404, "not-found", "Not found");

    Integer auditCount =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM publication_audit", Integer.class);
    assertEquals(1, auditCount);
  }

  @Test
  void ownershipIsRecheckedInsideWriteTransaction() throws Exception {
    publishService.setBeforeTransactionalAccessHook(
        () -> jdbcTemplate.update("DELETE FROM ta_classes WHERE ta_id = 'ta-own'"));

    assertProblem(postPublish("/v1/labs/1/publish", "own", "{}"), 404, "not-found", "Not found");
    assertNoPublication();
  }

  @Test
  void invalidLabIdsTrailingSlashAndUnknownRoutesReturnSafe404() throws Exception {
    for (String invalidId : List.of("0", "-1", "01", "1.0", "1e0", "9007199254740992", "abc")) {
      Map<String, Object> body =
          assertProblem(
              postPublish("/v1/labs/" + invalidId + "/publish", "own", "{}"),
              404,
              "not-found",
              "Not found");
      assertEquals("/v1", body.get("instance"));
    }

    for (String badPath : List.of("/v1/labs/1/publish/", "/v1/unknown?token=secret-fixture")) {
      Map<String, Object> body =
          assertProblem(postPublish(badPath, "own", "{}"), 404, "not-found", "Not found");
      assertEquals("/v1", body.get("instance"));
    }
    assertNoPublication();
  }

  @Test
  void wrongHttpMethodsReturn405WithAllowPost() throws Exception {
    for (String method : List.of("GET", "PUT", "DELETE")) {
      HttpResponse<String> res =
          sendRequest(method, "/v1/labs/1/publish", "own", "application/json", null, "{}");
      assertProblem(res, 405, "method-not-allowed", "Method not allowed");
      assertEquals("POST", res.headers().firstValue("allow").orElse(""));
    }
    assertNoPublication();
  }

  @Test
  void malformedOrEmptyJsonReturns400InvalidJson() throws Exception {
    for (String badBody : List.of("", "{", "{\"a\":}", "NaN", "{\"a\":1} trailing")) {
      assertProblem(
          postPublish("/v1/labs/1/publish", "own", badBody), 400, "invalid-json", "Malformed JSON");
    }
    assertNoPublication();
  }

  @Test
  void nonObjectJsonAndQueryParametersReturn400ValidationWithErrors() throws Exception {
    for (String scalarOrArray : List.of("null", "[]", "42", "true", "\"text\"")) {
      Map<String, Object> problem =
          assertProblem(
              postPublish("/v1/labs/1/publish?foo=1&bar=2&foo=3", "own", scalarOrArray),
              400,
              "validation",
              "Invalid request");
      assertEquals(
          List.of(
              Map.of("pointer", "", "detail", "must be a JSON object"),
              Map.of("parameter", "foo", "detail", "query parameters are not supported"),
              Map.of("parameter", "bar", "detail", "query parameters are not supported")),
          problem.get("errors"));
    }

    Map<String, Object> queryOnly =
        assertProblem(
            postPublish("/v1/labs/1/publish?bad=1", "own", "{}"),
            400,
            "validation",
            "Invalid request");
    assertEquals(
        List.of(Map.of("parameter", "bad", "detail", "query parameters are not supported")),
        queryOnly.get("errors"));
    assertNoPublication();
  }

  @Test
  void payloadByteLimitRejectsOver4096AndAcceptsExact4096() throws Exception {
    String overAscii = "{}" + " ".repeat(4095);
    assertProblem(
        postPublish("/v1/labs/1/publish", "own", overAscii),
        413,
        "payload-too-large",
        "Payload too large");

    String overMultibyte = "{\"v\":\"" + "é".repeat(2050) + "\"}";
    assertProblem(
        postPublish("/v1/labs/1/publish", "own", overMultibyte),
        413,
        "payload-too-large",
        "Payload too large");
    assertNoPublication();

    String exact4096 = "{}" + " ".repeat(4094);
    assertEquals(200, postPublish("/v1/labs/1/publish", "own", exact4096).statusCode());
  }

  @Test
  void onlyJsonUtf8AndIdentityEncodingAreAccepted() throws Exception {
    for (String badType :
        List.of(
            "text/plain",
            "application/jsonp",
            "application/json; charset=latin1",
            "application/json; extra=1")) {
      assertProblem(
          sendRequest("POST", "/v1/labs/1/publish", "own", badType, null, "{}"),
          415,
          "unsupported-media-type",
          "Unsupported media type");
    }

    assertProblem(
        sendRequest("POST", "/v1/labs/1/publish", "own", "application/json", "gzip", "{}"),
        415,
        "unsupported-media-type",
        "Unsupported media type");
    assertNoPublication();

    HttpResponse<String> validCaseInsensitive =
        sendRequest(
            "POST",
            "/v1/labs/1/publish",
            "own",
            "Application/JSON; Charset=\"UTF-8\"",
            "identity",
            "{}");
    assertEquals(200, validCaseInsensitive.statusCode());
  }

  @Test
  void oneMillisecondBeforeDeadlineIs409AndExactDeadlineSucceeds() throws Exception {
    CURRENT_TIME.set(DEFAULT_NOW.minusMillis(1));
    assertProblem(
        postPublish("/v1/labs/1/publish", "own", "{}"),
        409,
        "deadline-not-reached",
        "Submission window open");
    assertNoPublication();

    CURRENT_TIME.set(DEFAULT_NOW);
    assertEquals(200, postPublish("/v1/labs/1/publish", "own", "{}").statusCode());
  }

  @Test
  void queuedOrRunningJudgesBlockBeforeReviewCheck() throws Exception {
    jdbcTemplate.update("UPDATE review_items SET decision = NULL");
    for (String inFlight : List.of("queued", "running")) {
      jdbcTemplate.update(
          "UPDATE counted_submissions SET judge_status = ? WHERE id = 11", inFlight);
      assertProblem(
          postPublish("/v1/labs/1/publish", "own", "{}"),
          409,
          "judging-pending",
          "Judging pending");
      assertNoPublication();
    }

    jdbcTemplate.update("UPDATE counted_submissions SET judge_status = 'AC'");
    jdbcTemplate.update("UPDATE review_items SET decision = 'pass'");
    assertEquals(200, postPublish("/v1/labs/1/publish", "own", "{}").statusCode());
  }

  @Test
  void undecidedReviewItemBlocksUntilTaDecides() throws Exception {
    jdbcTemplate.update("UPDATE review_items SET decision = NULL WHERE constraint_id = 'C2'");
    assertProblem(
        postPublish("/v1/labs/1/publish", "own", "{}"), 409, "reviews-pending", "Reviews pending");
    assertNoPublication();

    jdbcTemplate.update("UPDATE review_items SET decision = 'pass' WHERE constraint_id = 'C2'");
    assertEquals(200, postPublish("/v1/labs/1/publish", "own", "{}").statusCode());
  }

  @Test
  void emptyLabCanPublishAndOtherLabPendingWorkDoesNotInterfere() throws Exception {
    HttpResponse<String> emptyLabRes = postPublish("/v1/labs/2/publish", "foreign", "{}");
    assertEquals(200, emptyLabRes.statusCode());
    PublicationReceipt emptyReceipt =
        objectMapper.readValue(emptyLabRes.body(), PublicationReceipt.class);
    assertEquals(0, emptyReceipt.submissionCount());

    jdbcTemplate.update(
        "INSERT INTO counted_submissions (id, lab_id, student_id, judge_status, score_tenths) VALUES (21, 2, 'foreign-student', 'running', 0)");
    jdbcTemplate.update(
        "INSERT INTO review_items (submission_id, constraint_id, decision) VALUES (21, 'C1', NULL)");
    assertEquals(200, postPublish("/v1/labs/1/publish", "own", "{}").statusCode());
  }

  @Test
  void concurrentRequestsConvergeOnSinglePublicationAndAudit() throws Exception {
    int concurrency = 16;
    ExecutorService pool = Executors.newFixedThreadPool(concurrency);
    try {
      List<Callable<HttpResponse<String>>> tasks = new ArrayList<>();
      for (int i = 0; i < concurrency; i++) {
        tasks.add(() -> postPublish("/v1/labs/1/publish", "own", "{}"));
      }
      List<Future<HttpResponse<String>>> futures = pool.invokeAll(tasks);
      List<HttpResponse<String>> responses = new ArrayList<>();
      for (Future<HttpResponse<String>> future : futures) {
        responses.add(future.get());
      }

      assertTrue(responses.stream().allMatch(r -> r.statusCode() == 200));
      long initialCount =
          responses.stream()
              .filter(r -> r.headers().firstValue("idempotent-replayed").isEmpty())
              .count();
      assertEquals(1L, initialCount);

      long distinctBodies = responses.stream().map(HttpResponse::body).distinct().count();
      assertEquals(1L, distinctBodies);

      assertEquals(
          1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM publications", Integer.class));
      assertEquals(
          2, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM published_marks", Integer.class));
      assertEquals(
          1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM publication_audit", Integer.class));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void databaseLockContentionReturns503WithRetryAfter() throws Exception {
    ExecutorService lockHolder = Executors.newSingleThreadExecutor();
    try {
      lockHolder
          .submit(
              () -> {
                publishService.getWriteLock().lock();
                return null;
              })
          .get();

      HttpResponse<String> contended = postPublish("/v1/labs/1/publish", "own", "{}");
      assertProblem(contended, 503, "publication-unavailable", "Publication unavailable");
      assertEquals("1", contended.headers().firstValue("retry-after").orElse(""));
      assertNoPublication();

      lockHolder
          .submit(
              () -> {
                publishService.getWriteLock().unlock();
                return null;
              })
          .get();
    } finally {
      lockHolder.shutdownNow();
    }

    assertEquals(200, postPublish("/v1/labs/1/publish", "own", "{}").statusCode());
  }

  @Test
  void auditFailureRollsBackPublicationAndMarksAtomically() throws Exception {
    publishService.setSimulateAuditFailure(true);

    Map<String, Object> problem =
        assertProblem(
            postPublish("/v1/labs/1/publish", "own", "{}"), 500, "internal", "Internal error");
    assertEquals("The request could not be completed.", problem.get("detail"));
    assertNoPublication();

    publishService.setSimulateAuditFailure(false);
    assertEquals(200, postPublish("/v1/labs/1/publish", "own", "{}").statusCode());
  }

  @Test
  void unexpectedAuthExceptionReturnsGeneric500WithoutLeakingSecrets() throws Exception {
    DELEGATE_AUTH.set(
        header -> {
          throw new IllegalStateException("secret-fixture /private.db token-leak");
        });

    Map<String, Object> problem =
        assertProblem(
            postPublish("/v1/labs/1/publish", "own", "{}"), 500, "internal", "Internal error");
    assertEquals("The request could not be completed.", problem.get("detail"));
    assertNoPublication();
  }

  @Test
  void invalidReplayInputStillReturns400Or415() throws Exception {
    assertEquals(200, postPublish("/v1/labs/1/publish", "own", "{}").statusCode());

    assertProblem(
        postPublish("/v1/labs/1/publish", "own", "{"), 400, "invalid-json", "Malformed JSON");
    assertProblem(
        sendRequest("POST", "/v1/labs/1/publish", "own", "text/plain", null, "{}"),
        415,
        "unsupported-media-type",
        "Unsupported media type");

    Integer auditCount =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM publication_audit", Integer.class);
    assertEquals(1, auditCount);
  }
}
