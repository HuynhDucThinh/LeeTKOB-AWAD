package com.leetkob.checker.guardrail;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetkob.checker.CheckerConstants;
import com.leetkob.checker.EvidenceLine;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Builder;

/**
 * Enforces G1 (strict JSON schema validation) and G5 (ignoring any model-invented deduction or
 * score fields so TA-configured {@code deductionTenths} remains the sole source of truth).
 */
public class ResponseSchemaValidator {

  private static final Set<String> ALLOWED_VERDICTS =
      Set.of(
          CheckerConstants.VERDICT_PASS,
          CheckerConstants.VERDICT_VIOLATION,
          CheckerConstants.VERDICT_UNSURE);

  private final ObjectMapper strictObjectMapper;

  @Builder
  public ResponseSchemaValidator(ObjectMapper objectMapper) {
    ObjectMapper base = objectMapper != null ? objectMapper.copy() : new ObjectMapper();
    base.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    this.strictObjectMapper = base;
  }

  public ResponseSchemaValidator() {
    this(null);
  }

  public SchemaValidationOutcome validate(String rawContent, String expectedConstraintId) {
    if (expectedConstraintId == null || expectedConstraintId.isBlank()) {
      return invalid("expectedConstraintId is blank");
    }
    if (rawContent == null || rawContent.isBlank()) {
      return invalid("LLM response content is empty");
    }

    String trimmed = rawContent.trim();
    if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
      return invalid("LLM response is not a pure JSON object");
    }

    JsonNode root;
    try {
      root = strictObjectMapper.readTree(trimmed);
    } catch (IOException ex) {
      return invalid("Malformed JSON: " + ex.getMessage());
    }

    if (root == null || !root.isObject()) {
      return invalid("Root JSON value must be an object");
    }

    JsonNode verdictsNode = root.get("verdicts");
    if (verdictsNode == null || !verdictsNode.isArray()) {
      return invalid("Missing or non-array 'verdicts' field");
    }
    if (verdictsNode.isEmpty()) {
      return invalid("'verdicts' array is empty");
    }

    Set<String> seenConstraintIds = new HashSet<>();
    for (JsonNode entry : verdictsNode) {
      if (entry == null || !entry.isObject()) {
        return invalid("Each item in 'verdicts' must be a JSON object");
      }
      JsonNode cidNode = entry.get("constraintId");
      if (cidNode == null || !cidNode.isTextual() || cidNode.asText().isBlank()) {
        return invalid("Missing or non-string 'constraintId'");
      }
      if (!seenConstraintIds.add(cidNode.asText())) {
        return invalid("Duplicate constraintId in 'verdicts': " + cidNode.asText());
      }
    }

    if (verdictsNode.size() != 1) {
      return invalid("Expected exactly 1 verdict entry but received " + verdictsNode.size());
    }

    JsonNode verdictEntry = verdictsNode.get(0);
    String constraintId = verdictEntry.get("constraintId").asText();
    if (!expectedConstraintId.equals(constraintId)) {
      return invalid(
          "Mismatched constraintId: expected '"
              + expectedConstraintId
              + "' but got '"
              + constraintId
              + "'");
    }

    JsonNode verdictNode = verdictEntry.get("verdict");
    if (verdictNode == null || !verdictNode.isTextual()) {
      return invalid("Missing or non-string 'verdict'");
    }
    String verdict = verdictNode.asText();
    if (!ALLOWED_VERDICTS.contains(verdict)) {
      return invalid("Disallowed verdict value: " + verdict);
    }

    JsonNode evidenceNode = verdictEntry.get("evidence");
    if (evidenceNode == null || !evidenceNode.isArray()) {
      return invalid("Missing or non-array 'evidence' field");
    }

    List<EvidenceLine> evidenceLines = new ArrayList<>(evidenceNode.size());
    for (JsonNode evidenceItem : evidenceNode) {
      if (evidenceItem == null || !evidenceItem.isObject()) {
        return invalid("Each item in 'evidence' must be an object");
      }
      JsonNode lineNode = evidenceItem.get("line");
      JsonNode textNode = evidenceItem.get("text");
      if (lineNode == null || !lineNode.isIntegralNumber() || !lineNode.canConvertToInt()) {
        return invalid("Evidence 'line' must be an integer");
      }
      if (textNode == null || !textNode.isTextual()) {
        return invalid("Evidence 'text' must be a string");
      }
      evidenceLines.add(
          EvidenceLine.builder().line(lineNode.asInt()).text(textNode.asText()).build());
    }

    // G5: Any model-invented fields such as "deduction", "deductionTenths", or "score"
    // are intentionally ignored and never copied into ParsedConstraintVerdict.
    return SchemaValidationOutcome.builder()
        .valid(true)
        .parsedVerdict(
            ParsedConstraintVerdict.builder()
                .constraintId(constraintId)
                .verdict(verdict)
                .evidence(List.copyOf(evidenceLines))
                .build())
        .errorDetail(null)
        .build();
  }

  private static SchemaValidationOutcome invalid(String reason) {
    return SchemaValidationOutcome.builder()
        .valid(false)
        .parsedVerdict(null)
        .errorDetail(reason)
        .build();
  }
}
