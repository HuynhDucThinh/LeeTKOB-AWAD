package com.leetkob.checker.hard;

import com.leetkob.checker.ConstraintSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses constraint descriptions starting with {@code "Do not use"} or {@code "Do not call"} and
 * extracts backticked banned C++ headers or symbols.
 */
public class BanConstraintParser {

  private static final List<String> BAN_DESCRIPTION_PREFIXES = List.of("do not use", "do not call");
  private static final Pattern BACKTICKED_TARGET_PATTERN = Pattern.compile("`([^`]+)`");
  private static final String EMPTY_CALL_SUFFIX = "()";
  private static final String STD_NAMESPACE_PREFIX = "std::";
  private static final String GLOBAL_STD_NAMESPACE_PREFIX = "::std::";

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
      parsedTargets.add(classifyTarget(rawTarget));
    }
    return parsedTargets;
  }

  private static BannedTarget classifyTarget(String rawTarget) {
    String cleanedTarget = stripTrailingCallParentheses(rawTarget.trim());

    if (isHeaderLiteral(cleanedTarget)) {
      return BannedTarget.builder()
          .kind(BannedTargetKind.INCLUDE_HEADER)
          .targetName(cleanedTarget)
          .build();
    }

    if (cleanedTarget.startsWith(GLOBAL_STD_NAMESPACE_PREFIX)) {
      return BannedTarget.builder()
          .kind(BannedTargetKind.STD_QUALIFIED_SYMBOL)
          .targetName(cleanedTarget.substring(GLOBAL_STD_NAMESPACE_PREFIX.length()))
          .build();
    }

    if (cleanedTarget.startsWith(STD_NAMESPACE_PREFIX)) {
      return BannedTarget.builder()
          .kind(BannedTargetKind.STD_QUALIFIED_SYMBOL)
          .targetName(cleanedTarget.substring(STD_NAMESPACE_PREFIX.length()))
          .build();
    }

    return BannedTarget.builder()
        .kind(BannedTargetKind.UNQUALIFIED_SYMBOL)
        .targetName(cleanedTarget)
        .build();
  }

  private static String stripTrailingCallParentheses(String target) {
    if (target.endsWith(EMPTY_CALL_SUFFIX)) {
      return target.substring(0, target.length() - EMPTY_CALL_SUFFIX.length()).trim();
    }
    return target;
  }

  private static boolean isHeaderLiteral(String target) {
    return (target.startsWith("<") && target.endsWith(">"))
        || (target.startsWith("\"") && target.endsWith("\""));
  }
}
