package com.leetkob.checker.hard;

import com.leetkob.checker.ConstraintSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses constraint descriptions starting with {@code "Do not use"} or {@code "Do not call"} and
 * extracts backticked banned C++ headers, namespace-qualified symbols (including multi-segment
 * paths such as {@code std::ranges::sort}), or unqualified symbols/keywords.
 */
public class BanConstraintParser {

  private static final List<String> BAN_DESCRIPTION_PREFIXES = List.of("do not use", "do not call");
  private static final Pattern BACKTICKED_TARGET_PATTERN = Pattern.compile("`([^`]+)`");
  private static final String EMPTY_CALL_SUFFIX = "()";
  private static final String SCOPE_OPERATOR = "::";

  public boolean isBanListConstraint(ConstraintSpec constraint) {
    if (constraint == null || constraint.description() == null) {
      return false;
    }
    String normalizedDescription = constraint.description().trim().toLowerCase(Locale.ROOT);
    boolean hasBanPrefix =
        BAN_DESCRIPTION_PREFIXES.stream().anyMatch(normalizedDescription::startsWith);
    return hasBanPrefix && !extractRawTargets(constraint.description()).isEmpty();
  }

  public List<String> extractRawTargets(String description) {
    List<String> rawTargets = new ArrayList<>();
    if (description == null || description.isEmpty()) {
      return rawTargets;
    }

    Matcher backtickMatcher = BACKTICKED_TARGET_PATTERN.matcher(description);
    while (backtickMatcher.find()) {
      String candidate = backtickMatcher.group(1).trim();
      if (!candidate.isEmpty()) {
        rawTargets.add(candidate);
      }
    }
    return rawTargets;
  }

  public List<BannedTarget> parseBannedTargets(String description) {
    List<String> rawTargets = extractRawTargets(description);
    List<BannedTarget> parsedTargets = new ArrayList<>(rawTargets.size());
    for (String rawTarget : rawTargets) {
      BannedTarget classified = classifyTarget(rawTarget);
      if (classified != null) {
        parsedTargets.add(classified);
      }
    }
    return parsedTargets;
  }

  private static BannedTarget classifyTarget(String rawTarget) {
    String cleanedTarget = stripTrailingCallParentheses(rawTarget.trim());
    if (cleanedTarget.isEmpty()) {
      return null;
    }

    if (isHeaderLiteral(cleanedTarget)) {
      return BannedTarget.builder()
          .kind(BannedTargetKind.INCLUDE_HEADER)
          .namespacePath(List.of())
          .targetName(cleanedTarget)
          .build();
    }

    String withoutLeadingGlobal =
        cleanedTarget.startsWith(SCOPE_OPERATOR)
            ? cleanedTarget.substring(SCOPE_OPERATOR.length()).trim()
            : cleanedTarget;

    if (withoutLeadingGlobal.contains(SCOPE_OPERATOR)) {
      List<String> segments =
          Arrays.stream(withoutLeadingGlobal.split(Pattern.quote(SCOPE_OPERATOR)))
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .toList();
      if (segments.size() >= 2) {
        return BannedTarget.builder()
            .kind(BannedTargetKind.QUALIFIED_SYMBOL)
            .namespacePath(List.copyOf(segments.subList(0, segments.size() - 1)))
            .targetName(segments.get(segments.size() - 1))
            .build();
      }
    }

    return BannedTarget.builder()
        .kind(BannedTargetKind.UNQUALIFIED_SYMBOL)
        .namespacePath(List.of())
        .targetName(withoutLeadingGlobal)
        .build();
  }

  private static String stripTrailingCallParentheses(String target) {
    String cleaned = target;
    if (cleaned.endsWith(EMPTY_CALL_SUFFIX)) {
      cleaned = cleaned.substring(0, cleaned.length() - EMPTY_CALL_SUFFIX.length()).trim();
    }
    if (cleaned.endsWith("[]")) {
      cleaned = cleaned.substring(0, cleaned.length() - 2).trim();
    }
    return cleaned;
  }

  private static boolean isHeaderLiteral(String target) {
    return (target.startsWith("<") && target.endsWith(">"))
        || (target.startsWith("\"") && target.endsWith("\""));
  }
}
