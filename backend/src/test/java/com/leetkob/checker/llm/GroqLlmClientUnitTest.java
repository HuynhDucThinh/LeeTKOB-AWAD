package com.leetkob.checker.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetkob.checker.CheckerConstants;
import com.leetkob.checker.LlmCallRecord;
import com.leetkob.config.AppConfig;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link GroqLlmClient}, {@link LlmCostCalculator}, {@link LlmCallLogger}, and
 * fail-closed startup configuration in {@link AppConfig} using local loopback HTTP stubs (zero
 * external network calls).
 */
class GroqLlmClientUnitTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void parsesGroqCompletionHeadersTokensAndSixDecimalCost() throws Exception {
    AtomicReference<String> capturedAuthHeader = new AtomicReference<>();
    AtomicReference<String> capturedRequestBody = new AtomicReference<>();

    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/openai/v1/chat/completions",
        exchange -> {
          capturedAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
          capturedRequestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

          String groqResponse =
              """
              {
                "id": "chatcmpl-fallback-id",
                "model": "llama-3.3-70b-versatile",
                "choices": [
                  {
                    "index": 0,
                    "message": {
                      "role": "assistant",
                      "content": "{\\"verdicts\\":[{\\"constraintId\\":\\"C1\\",\\"verdict\\":\\"pass\\",\\"evidence\\":[]}]}"
                    }
                  }
                ],
                "usage": {
                  "prompt_tokens": 400,
                  "completion_tokens": 50,
                  "total_tokens": 450
                },
                "x_groq": {
                  "id": "req_body_groq_99"
                }
              }
              """;
          byte[] bytes = groqResponse.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.getResponseHeaders().add("x-request-id", "req_header_groq_01");
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });
    server.start();

    try {
      int port = server.getAddress().getPort();
      GroqLlmClient client =
          GroqLlmClient.builder()
              .provider("groq")
              .model("llama-3.3-70b-versatile")
              .apiKey("test-only-dummy-llm-key")
              .endpointUri(URI.create("http://127.0.0.1:" + port + "/openai/v1/chat/completions"))
              .objectMapper(objectMapper)
              .build();

      LlmRawResponse response =
          client.evaluateConstraint(
              LlmPromptPayload.builder()
                  .constraintId("C1")
                  .systemPrompt("Return JSON")
                  .userPrompt("Check C1 in JSON")
                  .combinedSanitizedPrompt("[SYSTEM]\nReturn JSON\n[USER]\nCheck C1 in JSON")
                  .build());

      assertEquals("Bearer test-only-dummy-llm-key", capturedAuthHeader.get());
      JsonNode sentJson = objectMapper.readTree(capturedRequestBody.get());
      assertEquals("llama-3.3-70b-versatile", sentJson.get("model").asText());
      assertEquals(0, sentJson.get("temperature").asInt());
      assertEquals("json_object", sentJson.path("response_format").path("type").asText());

      assertEquals("groq", response.provider());
      assertEquals("llama-3.3-70b-versatile", response.model());
      assertEquals("req_header_groq_01", response.providerRequestId());
      assertEquals(400, response.promptTokens());
      assertEquals(50, response.completionTokens());
      // (400 * 0.59 + 50 * 0.79) / 1_000_000 = 0.0002755 -> 0.000276
      assertEquals(0, new BigDecimal("0.000276").compareTo(response.costUsd()));
      assertTrue(response.rawContent().contains("\"constraintId\":\"C1\""));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void enforcesRequestTimeoutAndThrowsLlmTimeoutException() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/slow",
        exchange -> {
          try {
            Thread.sleep(350);
          } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
          }
          byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().close();
        });
    server.start();

    try {
      int port = server.getAddress().getPort();
      GroqLlmClient client =
          GroqLlmClient.builder()
              .provider("groq")
              .model("llama-3.3-70b-versatile")
              .apiKey("test-only-dummy-llm-key")
              .endpointUri(URI.create("http://127.0.0.1:" + port + "/slow"))
              .requestTimeout(Duration.ofMillis(70))
              .build();

      LlmTimeoutException ex =
          assertThrows(
              LlmTimeoutException.class,
              () ->
                  client.evaluateConstraint(
                      LlmPromptPayload.builder()
                          .constraintId("C_SLOW")
                          .systemPrompt("JSON")
                          .userPrompt("JSON")
                          .combinedSanitizedPrompt("JSON")
                          .build()));
      assertEquals("groq", ex.getProvider());
      assertTrue(ex.getLatencyMs() >= 40);
    } finally {
      server.stop(0);
    }
  }

  @Test
  void non2xxStatusAndBlankApiKeyFailClosedWithoutLeakingCredentials() throws Exception {
    GroqLlmClient missingKeyClient =
        GroqLlmClient.builder()
            .provider("groq")
            .model("llama-3.3-70b-versatile")
            .apiKey("   ")
            .build();

    assertThrows(
        LlmProviderException.class,
        () ->
            missingKeyClient.evaluateConstraint(
                LlmPromptPayload.builder()
                    .constraintId("C1")
                    .systemPrompt("JSON")
                    .userPrompt("JSON")
                    .combinedSanitizedPrompt("JSON")
                    .build()));

    String dummyCredential = "test-only-dummy-credential-marker";
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/rate-limit",
        exchange -> {
          byte[] bytes = "{\"error\":\"rate_limited\"}".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(429, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });
    server.start();

    try {
      int port = server.getAddress().getPort();
      GroqLlmClient client =
          GroqLlmClient.builder()
              .provider("groq")
              .model("llama-3.3-70b-versatile")
              .apiKey(dummyCredential)
              .endpointUri(URI.create("http://127.0.0.1:" + port + "/rate-limit"))
              .maxRetries(1)
              .initialBackoffMs(10L)
              .build();

      LlmProviderException ex =
          assertThrows(
              LlmProviderException.class,
              () ->
                  client.evaluateConstraint(
                      LlmPromptPayload.builder()
                          .constraintId("C1")
                          .systemPrompt("JSON")
                          .userPrompt("JSON")
                          .combinedSanitizedPrompt("JSON")
                          .build()));
      assertEquals(429, ex.getStatusCode());
      assertFalse(ex.getMessage().contains(dummyCredential));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void appConfigFailsClosedWhenWorkerRoleRequiresModelKeyAndAllowsApiOrModelOff() {
    AppConfig appConfig = new AppConfig();

    assertThrows(
        IllegalStateException.class,
        () ->
            appConfig.llmClient(
                "worker", "false", "groq", "llama-3.3-70b-versatile", "", objectMapper));
    assertThrows(
        IllegalStateException.class,
        () ->
            appConfig.llmClient(
                "all", "0", "groq", "llama-3.3-70b-versatile", "   ", objectMapper));

    assertNotNull(
        appConfig.llmClient("api", "false", "groq", "llama-3.3-70b-versatile", "", objectMapper));
    assertNotNull(
        appConfig.llmClient("worker", "1", "groq", "llama-3.3-70b-versatile", "", objectMapper));
  }

  @Test
  void llmCallLoggerAppendsValidJsonLines(@TempDir Path tempDir) throws Exception {
    Path logFile = tempDir.resolve("runs").resolve("call-log.jsonl");
    LlmCallLogger logger = LlmCallLogger.builder().objectMapper(objectMapper).build();

    LlmCallRecord record =
        LlmCallRecord.builder()
            .constraintId("DIVIDE_CONQUER")
            .provider("groq")
            .model("llama-3.3-70b-versatile")
            .providerRequestId("req_audit_77")
            .promptTokens(320)
            .completionTokens(42)
            .latencyMs(275)
            .costUsd(new BigDecimal("0.000222"))
            .sanitizedPrompt("sanitized prompt")
            .rawResponse("{\"verdicts\":[]}")
            .verdict(CheckerConstants.VERDICT_UNSURE)
            .guardrailReason(CheckerConstants.REASON_SCHEMA_INVALID)
            .build();

    logger.appendRecords(logFile, "sort-merge/s01", List.of(record));

    List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
    assertEquals(1, lines.size());
    JsonNode parsed = objectMapper.readTree(lines.get(0));
    assertEquals("sort-merge/s01", parsed.get("solution_key").asText());
    assertEquals("DIVIDE_CONQUER", parsed.get("constraint_id").asText());
    assertEquals("req_audit_77", parsed.get("provider_request_id").asText());
    assertEquals("schema_invalid", parsed.get("guardrail_reason").asText());
  }
}
