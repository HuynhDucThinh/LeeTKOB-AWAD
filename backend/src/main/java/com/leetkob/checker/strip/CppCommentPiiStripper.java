package com.leetkob.checker.strip;

import com.leetkob.checker.lexical.CppSourceNormalizer;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Coordinates C++ comment stripping and student PII redaction while preserving exact 1-based line
 * numbers so every evidence citation maps 1-to-1 back to the student's original submission.
 */
@Builder
@AllArgsConstructor
public class CppCommentPiiStripper {

  private static final CppCommentPiiStripper DEFAULT_INSTANCE =
      CppCommentPiiStripper.builder().build();

  @Builder.Default private final CppCommentStripper commentStripper = new CppCommentStripper();

  @Builder.Default private final StudentPiiRedactor piiRedactor = new StudentPiiRedactor();

  @Builder.Default private final SourceLineFormatter lineFormatter = new SourceLineFormatter();

  public CppCommentPiiStripper() {
    this(new CppCommentStripper(), new StudentPiiRedactor(), new SourceLineFormatter());
  }

  public String stripSource(String rawSourceCode, List<String> piiTokensToStrip) {
    if (rawSourceCode == null || rawSourceCode.isEmpty()) {
      return "";
    }
    String normalizedSource = CppSourceNormalizer.normalizeLineEndings(rawSourceCode);
    String commentFreeSource = commentStripper.stripComments(normalizedSource);
    return piiRedactor.redact(commentFreeSource, piiTokensToStrip);
  }

  public String numberLines(String strippedCode) {
    return lineFormatter.formatWithLineNumbers(strippedCode);
  }

  public static String strip(String rawSourceCode, List<String> piiTokensToStrip) {
    return DEFAULT_INSTANCE.stripSource(rawSourceCode, piiTokensToStrip);
  }

  public static String formatWithLineNumbers(String strippedCode) {
    return DEFAULT_INSTANCE.numberLines(strippedCode);
  }
}
