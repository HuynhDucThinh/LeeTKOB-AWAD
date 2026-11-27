package com.leetkob.checker.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.leetkob.checker.CheckerConstants;
import com.leetkob.checker.ConstraintVerdictResult;
import com.leetkob.checker.EvidenceLine;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Serializes constraint checker verdicts into the exact {@code predictions.jsonl} schema consumed
 * by {@code eval.mjs}. Guarantees valid JSON per line and prevents duplicate {@code (solution,
 * constraint)} pairs.
 */
@Builder
@AllArgsConstructor
public class PredictionWriter {

  @Builder.Default private final ObjectMapper objectMapper = new ObjectMapper();

  public PredictionWriter() {
    this(new ObjectMapper());
  }

  public record PredictionRow(String solutionKey, ConstraintVerdictResult verdictResult) {}

  public void writePredictions(Path outputFile, List<PredictionRow> rows) throws IOException {
    Path parent = outputFile.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    StringBuilder jsonlBuffer = new StringBuilder();
    Set<String> seenPairs = new HashSet<>();

    for (PredictionRow row : rows) {
      ConstraintVerdictResult result = row.verdictResult();
      String pairKey = row.solutionKey() + "\u0000" + result.constraintId();
      if (!seenPairs.add(pairKey)) {
        continue;
      }
      jsonlBuffer.append(formatJsonLine(row.solutionKey(), result)).append('\n');
    }

    Files.writeString(outputFile, jsonlBuffer.toString(), StandardCharsets.UTF_8);
  }

  public String formatJsonLine(String solutionKey, ConstraintVerdictResult result)
      throws IOException {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("solution", solutionKey);
    root.put("constraint", result.constraintId());
    root.put("verdict", sanitizeVerdict(result.verdict()));

    ArrayNode evidenceArray = root.putArray("evidence");
    if (result.evidence() != null) {
      for (EvidenceLine line : result.evidence()) {
        if (line != null && line.line() >= 1 && line.text() != null) {
          ObjectNode evNode = evidenceArray.addObject();
          evNode.put("line", line.line());
          evNode.put("text", line.text());
        }
      }
    }
    return objectMapper.writeValueAsString(root);
  }

  private static String sanitizeVerdict(String rawVerdict) {
    if (CheckerConstants.VERDICT_PASS.equals(rawVerdict)
        || CheckerConstants.VERDICT_VIOLATION.equals(rawVerdict)
        || CheckerConstants.VERDICT_UNSURE.equals(rawVerdict)) {
      return rawVerdict;
    }
    return CheckerConstants.VERDICT_UNSURE;
  }
}
