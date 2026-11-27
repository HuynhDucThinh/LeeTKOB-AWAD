package com.leetkob.checker.lexical;

/**
 * Shared lexical span scanner for C++ comments, character literals, quoted strings, and C++11 raw
 * string literals. Centralizes span boundary detection so comment stripping and lexical
 * tokenization stay 100% consistent on line numbers.
 */
public final class CppSpanScanner {

  private static final int NO_MATCH = -1;
  private static final int MAX_RAW_STRING_DELIMITER_LENGTH = 16;

  private CppSpanScanner() {}

  /**
   * If {@code startIndex} begins a {@code //} comment, returns the exclusive end index right before
   * the terminating unescaped newline (or {@code source.length()} at EOF), advancing across any
   * backslash-continued lines. Returns {@code -1} if not at a single-line comment.
   */
  public static int tryScanSingleLineCommentEnd(String source, int startIndex) {
    int sourceLength = source.length();
    if (startIndex + 1 >= sourceLength
        || source.charAt(startIndex) != '/'
        || source.charAt(startIndex + 1) != '/') {
      return NO_MATCH;
    }

    int cursor = startIndex + 2;
    while (cursor < sourceLength) {
      if (source.charAt(cursor) == '\n') {
        boolean isLineContinued = source.charAt(cursor - 1) == '\\';
        if (!isLineContinued) {
          return cursor;
        }
      }
      cursor++;
    }
    return sourceLength;
  }

  /**
   * If {@code startIndex} begins a {@code /* ... *\/} block comment, returns the exclusive index
   * immediately after {@code *\/} (or {@code source.length()} if unterminated). Returns {@code -1}
   * if not at a block comment.
   */
  public static int tryScanBlockCommentEnd(String source, int startIndex) {
    int sourceLength = source.length();
    if (startIndex + 1 >= sourceLength
        || source.charAt(startIndex) != '/'
        || source.charAt(startIndex + 1) != '*') {
      return NO_MATCH;
    }

    int cursor = startIndex + 2;
    while (cursor < sourceLength) {
      if (source.charAt(cursor) == '*'
          && cursor + 1 < sourceLength
          && source.charAt(cursor + 1) == '/') {
        return cursor + 2;
      }
      cursor++;
    }
    return sourceLength;
  }

  /**
   * If {@code startIndex} begins a C++11 raw string literal, standard string literal, or character
   * literal, returns the exclusive index after the closing delimiter. Returns {@code -1} otherwise.
   */
  public static int tryScanStringOrCharLiteralEnd(String source, int startIndex) {
    int rawContentAfterQuote = matchRawStringOpeningQuoteEnd(source, startIndex);
    if (rawContentAfterQuote >= 0) {
      return scanRawStringEnd(source, rawContentAfterQuote);
    }

    char currentChar = source.charAt(startIndex);
    if (currentChar == '"' || currentChar == '\'') {
      return scanQuotedLiteralEnd(source, startIndex, currentChar);
    }
    return NO_MATCH;
  }

  public static boolean isIdentifierStart(char character) {
    return Character.isLetter(character) || character == '_';
  }

  public static boolean isIdentifierPart(char character) {
    return Character.isLetterOrDigit(character) || character == '_';
  }

  private static int matchRawStringOpeningQuoteEnd(String source, int startIndex) {
    int sourceLength = source.length();
    int probeIndex = startIndex;

    if (probeIndex < sourceLength && source.charAt(probeIndex) == 'u') {
      probeIndex++;
      if (probeIndex < sourceLength && source.charAt(probeIndex) == '8') {
        probeIndex++;
      }
    } else if (probeIndex < sourceLength
        && (source.charAt(probeIndex) == 'U' || source.charAt(probeIndex) == 'L')) {
      probeIndex++;
    }

    if (probeIndex + 1 < sourceLength
        && source.charAt(probeIndex) == 'R'
        && source.charAt(probeIndex + 1) == '"') {
      if (startIndex > 0 && isIdentifierPart(source.charAt(startIndex - 1))) {
        return NO_MATCH;
      }
      return probeIndex + 2;
    }
    return NO_MATCH;
  }

  private static int scanRawStringEnd(String source, int afterOpeningQuoteIndex) {
    int sourceLength = source.length();
    int openingParenIndex = source.indexOf('(', afterOpeningQuoteIndex);
    if (openingParenIndex < 0
        || openingParenIndex - afterOpeningQuoteIndex > MAX_RAW_STRING_DELIMITER_LENGTH) {
      return afterOpeningQuoteIndex;
    }

    String delimiter = source.substring(afterOpeningQuoteIndex, openingParenIndex);
    String closingSequence = ")" + delimiter + "\"";
    int closingSequenceIndex = source.indexOf(closingSequence, openingParenIndex + 1);
    if (closingSequenceIndex < 0) {
      return sourceLength;
    }
    return closingSequenceIndex + closingSequence.length();
  }

  private static int scanQuotedLiteralEnd(String source, int quoteStartIndex, char quoteChar) {
    int sourceLength = source.length();
    int cursor = quoteStartIndex + 1;

    while (cursor < sourceLength) {
      char currentChar = source.charAt(cursor);
      if (currentChar == '\\') {
        cursor += Math.min(2, sourceLength - cursor);
        continue;
      }
      if (currentChar == quoteChar) {
        return cursor + 1;
      }
      if (currentChar == '\n') {
        return cursor;
      }
      cursor++;
    }
    return sourceLength;
  }
}
