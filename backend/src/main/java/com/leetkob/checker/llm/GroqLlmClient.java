package com.leetkob.checker.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import lombok.Builder;

/**
 * Production {@link LlmClient} using Java 21 {@link HttpClient} against OpenAI-compatible Chat
 * Completions endpoints (defaulting to Groq {@code llama-3.3-70b-versatile}). Enforces strict
 * connect/request timeouts (G3), extracts real provider request IDs and token counts, and never
 * logs API keys.
 */
public class GroqLlmClient implements LlmClient {

  public static final String DEFAULT_PROVIDER = "groq";
  public static final String DEFAULT_MODEL = "openai/gpt-oss-120b";
  public static final URI GROQ_CHAT_COMPLETIONS_URI =
      URI.create("https://api.groq.com/openai/v1/chat/completions");
  public static final URI OPENAI_CHAT_COMPLETIONS_URI =
      URI.create("https://api.openai.com/v1/chat/completions");

  private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(25);
  private static final int DEFAULT_MAX_TOKENS = 2048;
  private static final int DEFAULT_MAX_RETRIES = 3;
  private static final long DEFAULT_INITIAL_BACKOFF_MS = 2000L;

  private final String provider;
  private final String model;
  private final String apiKey;
  private final URI endpointUri;
  private final Duration requestTimeout;
  private final int maxTokens;
  private final int maxRetries;
  private final long initialBackoffMs;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Builder
  public GroqLlmClient(
      String provider,
      String model,
      String apiKey,
      URI endpointUri,
      Duration connectTimeout,
      Duration requestTimeout,
      Integer maxTokens,
      Integer maxRetries,
      Long initialBackoffMs,
      HttpClient httpClient,
      ObjectMapper objectMapper) {
    this.provider =
        (provider == null || provider.isBlank())
            ? DEFAULT_PROVIDER
            : provider.trim().toLowerCase(Locale.ROOT);
    this.model = (model == null || model.isBlank()) ? DEFAULT_MODEL : model.trim();
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.endpointUri = endpointUri != null ? endpointUri : resolveDefaultEndpointUri(this.provider);
    Duration effectiveConnectTimeout =
        connectTimeout != null ? connectTimeout : DEFAULT_CONNECT_TIMEOUT;
    this.requestTimeout = requestTimeout != null ? requestTimeout : DEFAULT_REQUEST_TIMEOUT;
    this.maxTokens = (maxTokens != null && maxTokens > 0) ? maxTokens : DEFAULT_MAX_TOKENS;
    this.maxRetries = maxRetries != null ? Math.max(maxRetries, 0) : DEFAULT_MAX_RETRIES;
    this.initialBackoffMs =
        initialBackoffMs != null ? Math.max(initialBackoffMs, 10L) : DEFAULT_INITIAL_BACKOFF_MS;
    this.httpClient =
        httpClient != null
            ? httpClient
            : HttpClient.newBuilder().connectTimeout(effectiveConnectTimeout).build();
    this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
  }

  @Override
  public String providerName() {
    return provider;
  }

  @Override
  public String modelName() {
    return model;
  }

  @Override
  public LlmRawResponse evaluateConstraint(LlmPromptPayload payload) {
    Objects.requireNonNull(payload, "payload must not be null");
    if (apiKey.isEmpty()) {
      throw new LlmProviderException(
          provider,
          model,
          0,
          0,
          "MODEL_API_KEY is missing or blank; refusing unauthenticated LLM call");
    }

    String requestBody = buildChatCompletionsRequestBody(payload);
    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(endpointUri)
            .timeout(requestTimeout)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("User-Agent", "leetkob-checker/1.0")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
            .build();

    long startNanos = System.nanoTime();
    HttpResponse<String> response = null;
    for (int attempt = 0; attempt <= maxRetries; attempt++) {
      try {
        response =
            httpClient.send(
                httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      } catch (HttpTimeoutException timeoutEx) {
        int latencyMs = elapsedMillis(startNanos);
        throw new LlmTimeoutException(
            provider,
            model,
            latencyMs,
            "LLM request timed out after " + latencyMs + " ms",
            timeoutEx);
      } catch (InterruptedException interruptedEx) {
        Thread.currentThread().interrupt();
        int latencyMs = elapsedMillis(startNanos);
        throw new LlmProviderException(
            provider, model, 0, latencyMs, "LLM request interrupted", interruptedEx);
      } catch (IOException ioEx) {
        int latencyMs = elapsedMillis(startNanos);
        throw new LlmProviderException(
            provider, model, 0, latencyMs, "LLM network I/O failure", ioEx);
      }

      int status = response.statusCode();
      boolean isRetryableRateOrServiceLimit = status == 429 || status == 503;
      if (!isRetryableRateOrServiceLimit || attempt == maxRetries) {
        break;
      }

      long sleepMs = computeRetryDelayMs(response.headers(), attempt);
      try {
        Thread.sleep(sleepMs);
      } catch (InterruptedException interruptedEx) {
        Thread.currentThread().interrupt();
        int latencyMs = elapsedMillis(startNanos);
        throw new LlmProviderException(
            provider,
            model,
            status,
            latencyMs,
            "Interrupted during rate-limit backoff",
            interruptedEx);
      }
    }

    int latencyMs = elapsedMillis(startNanos);
    int statusCode = response.statusCode();
    if (statusCode < 200 || statusCode >= 300) {
      String errorCode = extractSafeErrorSummary(response.body());
      throw new LlmProviderException(
          provider,
          model,
          statusCode,
          latencyMs,
          "LLM provider returned HTTP status "
              + statusCode
              + (errorCode.isEmpty() ? "" : " (" + errorCode + ")"));
    }

    return parseCompletionResponse(response.body(), response.headers(), latencyMs);
  }

  private long computeRetryDelayMs(HttpHeaders headers, int attempt) {
    if (headers != null) {
      String retryAfter = headers.firstValue("retry-after").orElse("").trim();
      if (!retryAfter.isEmpty()) {
        try {
          double seconds = Double.parseDouble(retryAfter);
          if (seconds > 0) {
            return Math.min(Math.round(seconds * 1000.0) + 250L, 20_000L);
          }
        } catch (NumberFormatException ignore) {
          // fall back to exponential backoff
        }
      }
    }
    long exponential = initialBackoffMs * (1L << Math.min(attempt, 4));
    return Math.min(exponential, 20_000L);
  }

  private String extractSafeErrorSummary(String responseBody) {
    if (responseBody == null || responseBody.isBlank()) {
      return "";
    }
    try {
      JsonNode errorNode = objectMapper.readTree(responseBody).path("error");
      String type = textOrDefault(errorNode.get("type"), "");
      String code = textOrDefault(errorNode.get("code"), "");
      if (!type.isEmpty() && !code.isEmpty()) {
        return type + ":" + code;
      }
      return !code.isEmpty() ? code : type;
    } catch (IOException ignore) {
      return "";
    }
  }

  private String buildChatCompletionsRequestBody(LlmPromptPayload payload) {
    try {
      ObjectNode root = objectMapper.createObjectNode();
      root.put("model", model);
      root.put("temperature", 0);
      root.put("max_tokens", maxTokens);

      ObjectNode responseFormat = root.putObject("response_format");
      responseFormat.put("type", "json_object");

      ArrayNode messages = root.putArray("messages");
      ObjectNode systemMsg = messages.addObject();
      systemMsg.put("role", "system");
      systemMsg.put("content", payload.systemPrompt());

      ObjectNode userMsg = messages.addObject();
      userMsg.put("role", "user");
      userMsg.put("content", payload.userPrompt());

      return objectMapper.writeValueAsString(root);
    } catch (IOException ex) {
      throw new LlmProviderException(
          provider, model, 0, 0, "Failed to serialize LLM request payload", ex);
    }
  }

  private LlmRawResponse parseCompletionResponse(
      String responseBody, HttpHeaders headers, int latencyMs) {
    if (responseBody == null || responseBody.isBlank()) {
      throw new LlmProviderException(
          provider, model, 200, latencyMs, "LLM provider returned an empty HTTP body");
    }

    JsonNode root;
    try {
      root = objectMapper.readTree(responseBody);
    } catch (IOException ex) {
      throw new LlmProviderException(
          provider, model, 200, latencyMs, "LLM provider returned malformed outer JSON", ex);
    }

    String resolvedModel = textOrDefault(root.get("model"), model);
    String providerRequestId = extractProviderRequestId(root, headers);
    String rawContent = extractAssistantContent(root);

    JsonNode usage = root.path("usage");
    int promptTokens = Math.max(usage.path("prompt_tokens").asInt(0), 0);
    int completionTokens = Math.max(usage.path("completion_tokens").asInt(0), 0);
    BigDecimal costUsd =
        LlmCostCalculator.calculateCostUsd(resolvedModel, promptTokens, completionTokens);

    return LlmRawResponse.builder()
        .provider(provider)
        .model(resolvedModel)
        .providerRequestId(providerRequestId)
        .rawContent(rawContent)
        .promptTokens(promptTokens)
        .completionTokens(completionTokens)
        .latencyMs(latencyMs)
        .costUsd(costUsd)
        .build();
  }

  private static String extractProviderRequestId(JsonNode root, HttpHeaders headers) {
    if (headers != null) {
      String headerRequestId = headers.firstValue("x-request-id").orElse("").trim();
      if (!headerRequestId.isEmpty()) {
        return headerRequestId;
      }
    }

    String groqId = textOrDefault(root.path("x_groq").get("id"), "");
    if (!groqId.isEmpty()) {
      return groqId;
    }

    String completionId = textOrDefault(root.get("id"), "");
    return completionId.isEmpty() ? null : completionId;
  }

  private static String extractAssistantContent(JsonNode root) {
    JsonNode choices = root.get("choices");
    if (choices == null || !choices.isArray() || choices.isEmpty()) {
      return "";
    }
    JsonNode contentNode = choices.get(0).path("message").get("content");
    if (contentNode == null || contentNode.isNull()) {
      return "";
    }
    return contentNode.asText("");
  }

  private static String textOrDefault(JsonNode node, String fallback) {
    if (node == null || node.isNull() || !node.isTextual()) {
      return fallback;
    }
    String trimmed = node.asText().trim();
    return trimmed.isEmpty() ? fallback : trimmed;
  }

  private static int elapsedMillis(long startNanos) {
    return (int) Duration.ofNanos(Math.max(System.nanoTime() - startNanos, 0L)).toMillis();
  }

  private static URI resolveDefaultEndpointUri(String providerName) {
    if ("openai".equalsIgnoreCase(providerName)) {
      return OPENAI_CHAT_COMPLETIONS_URI;
    }
    return GROQ_CHAT_COMPLETIONS_URI;
  }
}
