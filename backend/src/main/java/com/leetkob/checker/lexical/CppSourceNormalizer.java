package com.leetkob.checker.lexical;

/** Line-ending normalization and newline counting utilities for C++ source code. */
public final class CppSourceNormalizer {

  private CppSourceNormalizer() {}

  public static String normalizeLineEndings(String rawText) {
    if (rawText == null || rawText.isEmpty()) {
      return "";
    }
    return rawText.replace("\r\n", "\n").replace("\r", "\n");
  }

  public static int countNewlines(String source, int startInclusive, int endExclusive) {
    int newlineCount = 0;
    for (int index = startInclusive; index < endExclusive; index++) {
      if (source.charAt(index) == '\n') {
        newlineCount++;
      }
    }
    return newlineCount;
  }
}
