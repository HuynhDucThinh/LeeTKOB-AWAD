package com.leetkob.checker.strip;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redacts student personal identifiable information (emails, student IDs, display names) without
 * shifting line numbers or corrupting C++ keywords and identifiers.
 */
public class StudentPiiRedactor {

  public static final String REDACTED_PLACEHOLDER = "[REDACTED]";
  private static final int MIN_PII_TOKEN_LENGTH = 2;
  private static final String LEADING_ALPHANUMERIC_BOUNDARY = "(?<![A-Za-z0-9])";
  private static final String TRAILING_ALPHANUMERIC_BOUNDARY = "(?![A-Za-z0-9])";

  public String redact(String sourceCode, List<String> piiTokens) {
    if (sourceCode == null || sourceCode.isEmpty() || piiTokens == null || piiTokens.isEmpty()) {
      return sourceCode == null ? "" : sourceCode;
    }

    String sanitizedCode = sourceCode;
    for (String rawToken : piiTokens) {
      if (rawToken == null) {
        continue;
      }
      String singleLineToken = rawToken.replace("\r", "").replace("\n", "").trim();
      if (singleLineToken.length() < MIN_PII_TOKEN_LENGTH) {
        continue;
      }
      Pattern redactionPattern = buildSafeTokenPattern(singleLineToken);
      sanitizedCode =
          redactionPattern
              .matcher(sanitizedCode)
              .replaceAll(Matcher.quoteReplacement(REDACTED_PLACEHOLDER));
    }
    return sanitizedCode;
  }

  private static Pattern buildSafeTokenPattern(String token) {
    char firstChar = token.charAt(0);
    char lastChar = token.charAt(token.length() - 1);

    StringBuilder regexBuilder = new StringBuilder();
    if (Character.isLetterOrDigit(firstChar)) {
      regexBuilder.append(LEADING_ALPHANUMERIC_BOUNDARY);
    }
    regexBuilder.append(Pattern.quote(token));
    if (Character.isLetterOrDigit(lastChar)) {
      regexBuilder.append(TRAILING_ALPHANUMERIC_BOUNDARY);
    }
    return Pattern.compile(regexBuilder.toString(), Pattern.CASE_INSENSITIVE);
  }
}
