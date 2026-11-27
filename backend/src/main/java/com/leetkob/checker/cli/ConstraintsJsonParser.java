package com.leetkob.checker.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetkob.checker.ConstraintSpec;
import com.leetkob.checker.hard.BanConstraintParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Parses {@code constraints.json} files in the {@code leetkob-eval} layout ({@code id}, {@code
 * text}, {@code deduction}) as well as internal {@code constraintId}/{@code description} variants.
 */
@Builder
@AllArgsConstructor
public class ConstraintsJsonParser {

  @Builder.Default private final ObjectMapper objectMapper = new ObjectMapper();

  @Builder.Default private final BanConstraintParser banParser = new BanConstraintParser();

  public ConstraintsJsonParser() {
    this(new ObjectMapper(), new BanConstraintParser());
  }

  public List<ConstraintSpec> parseFile(Path constraintsJsonPath) throws IOException {
    String rawJson = Files.readString(constraintsJsonPath, StandardCharsets.UTF_8);
    return parseJson(rawJson);
  }

  public List<ConstraintSpec> parseJson(String rawJson) throws IOException {
    JsonNode root = objectMapper.readTree(rawJson);
    if (root == null || !root.isArray()) {
      throw new IOException("constraints.json root must be a JSON array");
    }

    List<ConstraintSpec> specs = new ArrayList<>(root.size());
    for (JsonNode item : root) {
      if (item == null || !item.isObject()) {
        throw new IOException("Each constraint entry in constraints.json must be an object");
      }

      String id = firstNonBlankText(item, "id", "constraintId");
      String description = firstNonBlankText(item, "text", "description");
      if (id.isEmpty() || description.isEmpty()) {
        throw new IOException("Constraint entry is missing id or text");
      }

      int deductionTenths = resolveDeductionTenths(item);
      ConstraintSpec provisional =
          ConstraintSpec.builder()
              .constraintId(id)
              .type("semantic")
              .description(description)
              .deductionTenths(deductionTenths)
              .build();

      String explicitType = firstNonBlankText(item, "type");
      String resolvedType =
          !explicitType.isEmpty()
              ? explicitType
              : (banParser.isBanListConstraint(provisional) ? "hard" : "semantic");

      specs.add(
          ConstraintSpec.builder()
              .constraintId(id)
              .type(resolvedType)
              .description(description)
              .deductionTenths(deductionTenths)
              .build());
    }
    return List.copyOf(specs);
  }

  private static int resolveDeductionTenths(JsonNode item) {
    JsonNode tenthsNode = item.get("deductionTenths");
    if (tenthsNode != null && tenthsNode.isNumber()) {
      return Math.max(tenthsNode.asInt(), 0);
    }
    JsonNode pointsNode = item.get("deduction");
    if (pointsNode != null && pointsNode.isNumber()) {
      return Math.max((int) Math.round(pointsNode.asDouble() * 10.0), 0);
    }
    return 0;
  }

  private static String firstNonBlankText(JsonNode node, String... fieldNames) {
    for (String field : fieldNames) {
      JsonNode child = node.get(field);
      if (child != null && child.isTextual() && !child.asText().isBlank()) {
        return child.asText().trim();
      }
    }
    return "";
  }
}
