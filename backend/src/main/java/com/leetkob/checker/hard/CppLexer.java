package com.leetkob.checker.hard;

import com.leetkob.checker.lexical.CppSourceNormalizer;
import com.leetkob.checker.lexical.CppSpanScanner;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * Syntax-aware C++ lexical scanner that skips comments and string/character literals via {@link
 * CppSpanScanner} while preserving exact 1-based line numbers for every token and {@code #include}
 * directive.
 */
@Builder
@NoArgsConstructor
public class CppLexer {

  private static final CppLexer DEFAULT_INSTANCE = CppLexer.builder().build();
  private static final String INCLUDE_KEYWORD = "include";
  private static final String SCOPE_RESOLUTION_TEXT = "::";
  private static final String ARROW_MEMBER_ACCESS_TEXT = "->";
  private static final String DOT_MEMBER_ACCESS_TEXT = ".";

  public static List<CppToken> tokenize(String rawSourceCode) {
    return DEFAULT_INSTANCE.tokenizeSource(rawSourceCode);
  }

  public List<CppToken> tokenizeSource(String rawSourceCode) {
    List<CppToken> tokens = new ArrayList<>();
    if (rawSourceCode == null || rawSourceCode.isEmpty()) {
      return tokens;
    }

    String source = CppSourceNormalizer.normalizeLineEndings(rawSourceCode);
    int sourceLength = source.length();
    int cursor = 0;
    int lineNumber = 1;
    boolean atLineStart = true;

    while (cursor < sourceLength) {
      char currentChar = source.charAt(cursor);

      if (currentChar == '\n') {
        lineNumber++;
        atLineStart = true;
        cursor++;
        continue;
      }
      if (isHorizontalWhitespace(currentChar)) {
        cursor++;
        continue;
      }

      int lineCommentEnd = CppSpanScanner.tryScanSingleLineCommentEnd(source, cursor);
      if (lineCommentEnd > cursor) {
        lineNumber += CppSourceNormalizer.countNewlines(source, cursor, lineCommentEnd);
        cursor = lineCommentEnd;
        continue;
      }

      int blockCommentEnd = CppSpanScanner.tryScanBlockCommentEnd(source, cursor);
      if (blockCommentEnd > cursor) {
        int commentNewlines = CppSourceNormalizer.countNewlines(source, cursor, blockCommentEnd);
        lineNumber += commentNewlines;
        atLineStart = atLineStart || commentNewlines > 0;
        cursor = blockCommentEnd;
        continue;
      }

      if (currentChar == '#' && atLineStart) {
        DirectiveScanResult directiveResult =
            readPreprocessorDirective(source, cursor, lineNumber, tokens);
        cursor = directiveResult.nextCursor();
        lineNumber += directiveResult.consumedNewlines();
        atLineStart = false;
        continue;
      }

      atLineStart = false;

      int literalEnd = CppSpanScanner.tryScanStringOrCharLiteralEnd(source, cursor);
      if (literalEnd > cursor) {
        lineNumber += CppSourceNormalizer.countNewlines(source, cursor, literalEnd);
        cursor = literalEnd;
        continue;
      }

      if (Character.isDigit(currentChar)) {
        cursor = skipNumericLiteral(source, cursor);
        continue;
      }

      if (CppSpanScanner.isIdentifierStart(currentChar)) {
        cursor = readIdentifier(source, cursor, lineNumber, tokens);
        continue;
      }

      cursor = readOperatorOrPunctuation(source, cursor, lineNumber, tokens);
    }

    return tokens;
  }

  private static DirectiveScanResult readPreprocessorDirective(
      String source, int hashIndex, int directiveLine, List<CppToken> tokens) {
    int sourceLength = source.length();
    int probeIndex = hashIndex + 1;
    while (probeIndex < sourceLength && isHorizontalWhitespace(source.charAt(probeIndex))) {
      probeIndex++;
    }

    int directiveWordStart = probeIndex;
    while (probeIndex < sourceLength && Character.isLetter(source.charAt(probeIndex))) {
      probeIndex++;
    }

    String directiveKeyword = source.substring(directiveWordStart, probeIndex);
    if (!INCLUDE_KEYWORD.equals(directiveKeyword)) {
      // Leave macro bodies (e.g. `#define MY_SORT std::sort`) in the token stream so hard rules
      // inspect macro aliases as normal C++ tokens.
      return new DirectiveScanResult(probeIndex, 0);
    }

    int consumedNewlines = 0;
    while (probeIndex < sourceLength) {
      if (source.charAt(probeIndex) == '\n') {
        boolean isContinuedLine = source.charAt(probeIndex - 1) == '\\';
        if (!isContinuedLine) {
          break;
        }
        consumedNewlines++;
      }
      probeIndex++;
    }

    String normalizedDirective =
        source.substring(hashIndex, probeIndex).replace("\\\n", " ").trim();
    tokens.add(
        CppToken.builder()
            .kind(CppTokenKind.INCLUDE_DIRECTIVE)
            .text(normalizedDirective)
            .line(directiveLine)
            .build());
    return new DirectiveScanResult(probeIndex, consumedNewlines);
  }

  private static int skipNumericLiteral(String source, int startIndex) {
    int sourceLength = source.length();
    int cursor = startIndex + 1;
    while (cursor < sourceLength && isNumericLiteralPart(source.charAt(cursor))) {
      cursor++;
    }
    return cursor;
  }

  private static int readIdentifier(
      String source, int startIndex, int lineNumber, List<CppToken> tokens) {
    int sourceLength = source.length();
    int cursor = startIndex + 1;
    while (cursor < sourceLength && CppSpanScanner.isIdentifierPart(source.charAt(cursor))) {
      cursor++;
    }
    tokens.add(
        CppToken.builder()
            .kind(CppTokenKind.IDENTIFIER)
            .text(source.substring(startIndex, cursor))
            .line(lineNumber)
            .build());
    return cursor;
  }

  private static int readOperatorOrPunctuation(
      String source, int startIndex, int lineNumber, List<CppToken> tokens) {
    int sourceLength = source.length();
    char currentChar = source.charAt(startIndex);

    if (currentChar == ':'
        && startIndex + 1 < sourceLength
        && source.charAt(startIndex + 1) == ':') {
      tokens.add(
          CppToken.builder()
              .kind(CppTokenKind.SCOPE_RESOLUTION)
              .text(SCOPE_RESOLUTION_TEXT)
              .line(lineNumber)
              .build());
      return startIndex + 2;
    }

    if (currentChar == '-'
        && startIndex + 1 < sourceLength
        && source.charAt(startIndex + 1) == '>') {
      tokens.add(
          CppToken.builder()
              .kind(CppTokenKind.MEMBER_ACCESS)
              .text(ARROW_MEMBER_ACCESS_TEXT)
              .line(lineNumber)
              .build());
      return startIndex + 2;
    }

    if (currentChar == '.') {
      tokens.add(
          CppToken.builder()
              .kind(CppTokenKind.MEMBER_ACCESS)
              .text(DOT_MEMBER_ACCESS_TEXT)
              .line(lineNumber)
              .build());
      return startIndex + 1;
    }

    CppTokenKind tokenKind = mapSingleCharacterKind(currentChar);
    tokens.add(
        CppToken.builder()
            .kind(tokenKind)
            .text(String.valueOf(currentChar))
            .line(lineNumber)
            .build());
    return startIndex + 1;
  }

  private static CppTokenKind mapSingleCharacterKind(char character) {
    return switch (character) {
      case '(' -> CppTokenKind.LEFT_PAREN;
      case ')' -> CppTokenKind.RIGHT_PAREN;
      case '{' -> CppTokenKind.LEFT_BRACE;
      case '}' -> CppTokenKind.RIGHT_BRACE;
      case '<' -> CppTokenKind.LEFT_ANGLE;
      case '>' -> CppTokenKind.RIGHT_ANGLE;
      case ';' -> CppTokenKind.SEMICOLON;
      case ',' -> CppTokenKind.COMMA;
      case '=' -> CppTokenKind.ASSIGN;
      default -> CppTokenKind.OTHER_SYMBOL;
    };
  }

  private static boolean isHorizontalWhitespace(char character) {
    return character == ' ' || character == '\t' || character == '\f';
  }

  private static boolean isNumericLiteralPart(char character) {
    return Character.isLetterOrDigit(character)
        || character == '_'
        || character == '.'
        || character == '\'';
  }

  private record DirectiveScanResult(int nextCursor, int consumedNewlines) {}
}
