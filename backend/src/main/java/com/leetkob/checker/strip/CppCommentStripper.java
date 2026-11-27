package com.leetkob.checker.strip;

import com.leetkob.checker.lexical.CppSourceNormalizer;
import com.leetkob.checker.lexical.CppSpanScanner;

/**
 * Removes C++ single-line and block comments while preserving exact 1-based line numbers and
 * inserting a single space for inline block comments so adjacent tokens never merge.
 */
public class CppCommentStripper {

  public String stripComments(String normalizedSource) {
    if (normalizedSource == null || normalizedSource.isEmpty()) {
      return "";
    }

    int sourceLength = normalizedSource.length();
    StringBuilder output = new StringBuilder(sourceLength);
    int cursor = 0;

    while (cursor < sourceLength) {
      int literalEnd = CppSpanScanner.tryScanStringOrCharLiteralEnd(normalizedSource, cursor);
      if (literalEnd > cursor) {
        output.append(normalizedSource, cursor, literalEnd);
        cursor = literalEnd;
        continue;
      }

      int lineCommentEnd = CppSpanScanner.tryScanSingleLineCommentEnd(normalizedSource, cursor);
      if (lineCommentEnd > cursor) {
        appendPreservedNewlines(output, normalizedSource, cursor, lineCommentEnd);
        cursor = lineCommentEnd;
        continue;
      }

      int blockCommentEnd = CppSpanScanner.tryScanBlockCommentEnd(normalizedSource, cursor);
      if (blockCommentEnd > cursor) {
        int preservedNewlines =
            appendPreservedNewlines(output, normalizedSource, cursor, blockCommentEnd);
        if (preservedNewlines == 0) {
          output.append(' ');
        }
        cursor = blockCommentEnd;
        continue;
      }

      output.append(normalizedSource.charAt(cursor));
      cursor++;
    }

    return output.toString();
  }

  private static int appendPreservedNewlines(
      StringBuilder output, String source, int startInclusive, int endExclusive) {
    int newlineCount = CppSourceNormalizer.countNewlines(source, startInclusive, endExclusive);
    for (int count = 0; count < newlineCount; count++) {
      output.append('\n');
    }
    return newlineCount;
  }
}
