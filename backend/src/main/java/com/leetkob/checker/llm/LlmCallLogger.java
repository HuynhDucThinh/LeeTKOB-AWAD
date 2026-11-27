package com.leetkob.checker.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.leetkob.checker.LlmCallRecord;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Formats and persists {@link LlmCallRecord} entries as JSON Lines ({@code call-log.jsonl}) for
 * standalone CLI evaluation runs and audit trails.
 */
@Builder
@AllArgsConstructor
public class LlmCallLogger {

  @Builder.Default private final ObjectMapper objectMapper = new ObjectMapper();

  public LlmCallLogger() {
    this(new ObjectMapper());
  }

  public String formatJsonLine(String solutionKey, LlmCallRecord record) {
    try {
      ObjectNode node = objectMapper.createObjectNode();
      node.put("timestamp", Instant.now().toString());
      node.put("solution_key", solutionKey == null ? "" : solutionKey);
      node.put("constraint_id", record.constraintId());
      node.put("provider", record.provider());
      node.put("model", record.model());
      if (record.providerRequestId() != null) {
        node.put("provider_request_id", record.providerRequestId());
      } else {
        node.putNull("provider_request_id");
      }
      node.put("prompt_tokens", record.promptTokens());
      node.put("completion_tokens", record.completionTokens());
      node.put("latency_ms", record.latencyMs());
      node.put(
          "cost_usd", record.costUsd() != null ? record.costUsd() : BigDecimal.ZERO.setScale(6));
      node.put("verdict", record.verdict());
      if (record.guardrailReason() != null) {
        node.put("guardrail_reason", record.guardrailReason());
      } else {
        node.putNull("guardrail_reason");
      }
      node.put("sanitized_prompt", record.sanitizedPrompt());
      node.put("raw_response", record.rawResponse());
      return objectMapper.writeValueAsString(node);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to serialize LlmCallRecord to JSONL", ex);
    }
  }

  public void appendRecords(Path jsonlPath, String solutionKey, List<LlmCallRecord> records)
      throws IOException {
    if (jsonlPath == null || records == null || records.isEmpty()) {
      return;
    }
    Path parent = jsonlPath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    StringBuilder buffer = new StringBuilder();
    for (LlmCallRecord record : records) {
      buffer.append(formatJsonLine(solutionKey, record)).append('\n');
    }
    Files.writeString(
        jsonlPath,
        buffer.toString(),
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.APPEND);
  }
}
