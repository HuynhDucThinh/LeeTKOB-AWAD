package com.leetkob.checker.hard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Analyzes a C++ token stream to extract {@code using namespace ...}, {@code using ns::symbol},
 * {@code namespace alias = ...} (including multi-segment paths like {@code std::ranges}), and
 * student-authored function and type declarations/definitions.
 */
public class CppScopeAnalyzer {

  private static final String KEYWORD_USING = "using";
  private static final String KEYWORD_NAMESPACE = "namespace";
  private static final String POINTER_SYMBOL = "*";
  private static final String REFERENCE_SYMBOL = "&";

  private static final Set<String> USER_TYPE_KEYWORDS = Set.of("struct", "class", "enum");

  private static final Set<String> PRIMITIVE_TYPE_SPECIFIERS =
      Set.of(
          "int",
          "long",
          "short",
          "char",
          "bool",
          "float",
          "double",
          "void",
          "unsigned",
          "signed",
          "size_t",
          "ptrdiff_t",
          "int64_t",
          "uint64_t",
          "int32_t",
          "uint32_t",
          "auto");

  private static final Set<String> NON_DECLARATION_PRECEDING_KEYWORDS =
      Set.of(
          "return",
          "if",
          "while",
          "for",
          "switch",
          "catch",
          "else",
          "do",
          "case",
          "new",
          "delete",
          "throw",
          "sizeof",
          "co_return",
          "co_await");

  private static final Set<String> FUNCTION_POST_QUALIFIERS =
      Set.of("const", "noexcept", "override", "final");

  public CppScopeContext analyze(List<CppToken> tokens) {
    Set<List<String>> usingNamespacePaths = new HashSet<>();
    Set<List<String>> usingDeclarations = new HashSet<>();
    Map<String, List<String>> namespaceAliases = new HashMap<>();
    Set<Integer> directiveAndDeclarationTokenIndices = new HashSet<>();
    Set<String> userDefinedFunctionNames = new HashSet<>();
    Set<String> userDefinedTypeNames = new HashSet<>();
    Set<String> userDefinedVariableNames = new HashSet<>();

    int tokenCount = tokens.size();
    for (int tokenIndex = 0; tokenIndex < tokenCount; tokenIndex++) {
      CppToken currentToken = tokens.get(tokenIndex);

      PathParseResult usingNs = tryParseUsingNamespace(tokens, tokenIndex, namespaceAliases);
      if (usingNs != null) {
        usingNamespacePaths.add(usingNs.path());
        markRange(directiveAndDeclarationTokenIndices, tokenIndex, usingNs.lastTokenIndex());
        continue;
      }

      PathParseResult usingDecl = tryParseUsingDeclaration(tokens, tokenIndex, namespaceAliases);
      if (usingDecl != null) {
        usingDeclarations.add(usingDecl.path());
        markRange(directiveAndDeclarationTokenIndices, tokenIndex, usingDecl.lastTokenIndex());
        continue;
      }

      AliasParseResult nsAlias = tryParseNamespaceAlias(tokens, tokenIndex, namespaceAliases);
      if (nsAlias != null) {
        namespaceAliases.put(nsAlias.aliasName(), nsAlias.targetPath());
        markRange(directiveAndDeclarationTokenIndices, tokenIndex, nsAlias.lastTokenIndex());
        continue;
      }

      if (currentToken.kind() == CppTokenKind.IDENTIFIER
          && USER_TYPE_KEYWORDS.contains(currentToken.text())
          && tokenIndex + 2 < tokenCount
          && tokens.get(tokenIndex + 1).kind() == CppTokenKind.IDENTIFIER
          && (tokens.get(tokenIndex + 2).kind() == CppTokenKind.LEFT_BRACE
              || tokens.get(tokenIndex + 2).kind() == CppTokenKind.SEMICOLON
              || ":".equals(tokens.get(tokenIndex + 2).text()))) {
        userDefinedTypeNames.add(tokens.get(tokenIndex + 1).text());
        directiveAndDeclarationTokenIndices.add(tokenIndex + 1);
        continue;
      }

      if (currentToken.kind() == CppTokenKind.IDENTIFIER
          && tokenIndex + 1 < tokenCount
          && tokens.get(tokenIndex + 1).kind() == CppTokenKind.LEFT_PAREN
          && isFunctionDeclarationOrDefinition(tokens, tokenIndex)) {
        directiveAndDeclarationTokenIndices.add(tokenIndex);
        userDefinedFunctionNames.add(currentToken.text());
        continue;
      }

      if (currentToken.kind() == CppTokenKind.IDENTIFIER
          && isPrimitiveTypedVariableDeclaration(tokens, tokenIndex)) {
        directiveAndDeclarationTokenIndices.add(tokenIndex);
        userDefinedVariableNames.add(currentToken.text());
      }
    }

    return CppScopeContext.builder()
        .usingNamespacePaths(Set.copyOf(usingNamespacePaths))
        .usingDeclarations(Set.copyOf(usingDeclarations))
        .namespaceAliases(Map.copyOf(namespaceAliases))
        .directiveAndDeclarationTokenIndices(Set.copyOf(directiveAndDeclarationTokenIndices))
        .userDefinedFunctionNames(Set.copyOf(userDefinedFunctionNames))
        .userDefinedTypeNames(Set.copyOf(userDefinedTypeNames))
        .userDefinedVariableNames(Set.copyOf(userDefinedVariableNames))
        .build();
  }

  private static PathParseResult tryParseUsingNamespace(
      List<CppToken> tokens, int index, Map<String, List<String>> aliases) {
    int tokenCount = tokens.size();
    if (index + 2 >= tokenCount
        || !isIdentifierWithText(tokens.get(index), KEYWORD_USING)
        || !isIdentifierWithText(tokens.get(index + 1), KEYWORD_NAMESPACE)) {
      return null;
    }
    PathParseResult parsed = readScopedPath(tokens, index + 2, aliases);
    if (parsed == null || parsed.path().isEmpty()) {
      return null;
    }
    return parsed;
  }

  private static PathParseResult tryParseUsingDeclaration(
      List<CppToken> tokens, int index, Map<String, List<String>> aliases) {
    int tokenCount = tokens.size();
    if (index + 3 >= tokenCount
        || !isIdentifierWithText(tokens.get(index), KEYWORD_USING)
        || isIdentifierWithText(tokens.get(index + 1), KEYWORD_NAMESPACE)) {
      return null;
    }
    // Skip type aliases of the form `using Alias = Type;` so the RHS type is still inspected!
    if (tokens.get(index + 1).kind() == CppTokenKind.IDENTIFIER
        && tokens.get(index + 2).kind() == CppTokenKind.ASSIGN) {
      return null;
    }
    PathParseResult parsed = readScopedPath(tokens, index + 1, aliases);
    if (parsed == null || parsed.path().size() < 2) {
      return null;
    }
    return parsed;
  }

  private static AliasParseResult tryParseNamespaceAlias(
      List<CppToken> tokens, int index, Map<String, List<String>> aliases) {
    int tokenCount = tokens.size();
    if (index + 3 >= tokenCount
        || !isIdentifierWithText(tokens.get(index), KEYWORD_NAMESPACE)
        || tokens.get(index + 1).kind() != CppTokenKind.IDENTIFIER
        || tokens.get(index + 2).kind() != CppTokenKind.ASSIGN) {
      return null;
    }
    PathParseResult rhs = readScopedPath(tokens, index + 3, aliases);
    if (rhs == null || rhs.path().isEmpty()) {
      return null;
    }
    return new AliasParseResult(tokens.get(index + 1).text(), rhs.path(), rhs.lastTokenIndex());
  }

  private static PathParseResult readScopedPath(
      List<CppToken> tokens, int startIndex, Map<String, List<String>> aliases) {
    int tokenCount = tokens.size();
    int cursor = startIndex;
    boolean leadingGlobal = false;
    if (cursor < tokenCount && tokens.get(cursor).kind() == CppTokenKind.SCOPE_RESOLUTION) {
      leadingGlobal = true;
      cursor++;
    }
    if (cursor >= tokenCount || tokens.get(cursor).kind() != CppTokenKind.IDENTIFIER) {
      return null;
    }

    List<String> segments = new ArrayList<>();
    segments.add(tokens.get(cursor).text());
    int lastConsumed = cursor;
    cursor++;

    while (cursor + 1 < tokenCount
        && tokens.get(cursor).kind() == CppTokenKind.SCOPE_RESOLUTION
        && tokens.get(cursor + 1).kind() == CppTokenKind.IDENTIFIER) {
      segments.add(tokens.get(cursor + 1).text());
      lastConsumed = cursor + 1;
      cursor += 2;
    }

    if (!leadingGlobal) {
      List<String> aliased = aliases.get(segments.get(0));
      if (aliased != null) {
        List<String> expanded = new ArrayList<>(aliased.size() + segments.size() - 1);
        expanded.addAll(aliased);
        expanded.addAll(segments.subList(1, segments.size()));
        return new PathParseResult(List.copyOf(expanded), lastConsumed);
      }
    }
    return new PathParseResult(List.copyOf(segments), lastConsumed);
  }

  private static void markRange(Set<Integer> indices, int fromInclusive, int toInclusive) {
    for (int i = fromInclusive; i <= toInclusive; i++) {
      indices.add(i);
    }
  }

  private static boolean isFunctionDeclarationOrDefinition(
      List<CppToken> tokens, int functionNameIndex) {
    if (functionNameIndex == 0) {
      return false;
    }

    CppToken previousToken = tokens.get(functionNameIndex - 1);
    if (previousToken.kind() == CppTokenKind.MEMBER_ACCESS) {
      return false;
    }
    if (previousToken.kind() == CppTokenKind.IDENTIFIER
        && NON_DECLARATION_PRECEDING_KEYWORDS.contains(previousToken.text())) {
      return false;
    }
    if (!isReturnTypeEndingToken(previousToken)) {
      return false;
    }

    int closingParenIndex = findMatchingRightParen(tokens, functionNameIndex + 1);
    if (closingParenIndex < 0 || closingParenIndex + 1 >= tokens.size()) {
      return false;
    }

    int postSignatureIndex = skipPostSignatureQualifiers(tokens, closingParenIndex + 1);
    if (postSignatureIndex >= tokens.size()) {
      return false;
    }

    CppTokenKind trailingKind = tokens.get(postSignatureIndex).kind();
    if (trailingKind == CppTokenKind.LEFT_BRACE) {
      return true;
    }
    return trailingKind == CppTokenKind.SEMICOLON
        && isLikelyDeclarationStatementStart(tokens, functionNameIndex - 1);
  }

  private static int skipPostSignatureQualifiers(List<CppToken> tokens, int startIndex) {
    int scanIndex = startIndex;
    while (scanIndex < tokens.size()
        && tokens.get(scanIndex).kind() == CppTokenKind.IDENTIFIER
        && FUNCTION_POST_QUALIFIERS.contains(tokens.get(scanIndex).text())) {
      scanIndex++;
    }
    return scanIndex;
  }

  private static boolean isLikelyDeclarationStatementStart(
      List<CppToken> tokens, int returnTypeEndIndex) {
    int scanIndex = returnTypeEndIndex;
    while (scanIndex > 0) {
      CppToken precedingToken = tokens.get(scanIndex - 1);
      if (isStatementBoundary(precedingToken.kind())) {
        return true;
      }
      if (isPartOfReturnTypeSpecification(precedingToken)) {
        scanIndex--;
        continue;
      }
      return false;
    }
    return true;
  }

  private static boolean isStatementBoundary(CppTokenKind kind) {
    return kind == CppTokenKind.SEMICOLON
        || kind == CppTokenKind.LEFT_BRACE
        || kind == CppTokenKind.RIGHT_BRACE
        || kind == CppTokenKind.INCLUDE_DIRECTIVE;
  }

  private static boolean isPartOfReturnTypeSpecification(CppToken token) {
    return token.kind() == CppTokenKind.IDENTIFIER
        || token.kind() == CppTokenKind.SCOPE_RESOLUTION
        || token.kind() == CppTokenKind.LEFT_ANGLE
        || token.kind() == CppTokenKind.RIGHT_ANGLE
        || isPointerOrReferenceSymbol(token);
  }

  private static boolean isReturnTypeEndingToken(CppToken token) {
    return token.kind() == CppTokenKind.IDENTIFIER
        || token.kind() == CppTokenKind.RIGHT_ANGLE
        || isPointerOrReferenceSymbol(token);
  }

  private static boolean isPointerOrReferenceSymbol(CppToken token) {
    return token.kind() == CppTokenKind.OTHER_SYMBOL
        && (POINTER_SYMBOL.equals(token.text()) || REFERENCE_SYMBOL.equals(token.text()));
  }

  private static int findMatchingRightParen(List<CppToken> tokens, int openParenIndex) {
    int parenDepth = 0;
    for (int index = openParenIndex; index < tokens.size(); index++) {
      CppTokenKind currentKind = tokens.get(index).kind();
      if (currentKind == CppTokenKind.LEFT_PAREN) {
        parenDepth++;
      } else if (currentKind == CppTokenKind.RIGHT_PAREN) {
        parenDepth--;
        if (parenDepth == 0) {
          return index;
        }
      }
    }
    return -1;
  }

  private static boolean isPrimitiveTypedVariableDeclaration(List<CppToken> tokens, int nameIndex) {
    if (PRIMITIVE_TYPE_SPECIFIERS.contains(tokens.get(nameIndex).text())) {
      return false;
    }
    if (nameIndex + 1 < tokens.size()) {
      CppTokenKind nextKind = tokens.get(nameIndex + 1).kind();
      if (nextKind == CppTokenKind.SCOPE_RESOLUTION || nextKind == CppTokenKind.LEFT_ANGLE) {
        return false;
      }
    }
    int cursor = nameIndex - 1;
    while (cursor >= 0 && isPointerOrReferenceSymbol(tokens.get(cursor))) {
      cursor--;
    }
    return cursor >= 0
        && tokens.get(cursor).kind() == CppTokenKind.IDENTIFIER
        && PRIMITIVE_TYPE_SPECIFIERS.contains(tokens.get(cursor).text());
  }

  private static boolean isIdentifierWithText(CppToken token, String expectedText) {
    return token.kind() == CppTokenKind.IDENTIFIER && expectedText.equals(token.text());
  }

  private record PathParseResult(List<String> path, int lastTokenIndex) {}

  private record AliasParseResult(String aliasName, List<String> targetPath, int lastTokenIndex) {}
}
