package com.leetkob.checker.hard;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Analyzes a C++ token stream to extract {@code using namespace std}, {@code using std::symbol},
 * {@code namespace alias = std}, and student-authored function declarations/definitions so hard
 * rules never misclassify user functions as standard-library violations.
 */
public class CppScopeAnalyzer {

  private static final String KEYWORD_USING = "using";
  private static final String KEYWORD_NAMESPACE = "namespace";
  private static final String NAMESPACE_STD = "std";
  private static final String POINTER_SYMBOL = "*";
  private static final String REFERENCE_SYMBOL = "&";

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
    boolean hasUsingNamespaceStd = false;
    Set<String> importedStdSymbols = new HashSet<>();
    Set<String> stdNamespaceAliases = new HashSet<>();
    Set<Integer> directiveAndDeclarationTokenIndices = new HashSet<>();
    Set<String> userDefinedFunctionNames = new HashSet<>();

    int tokenCount = tokens.size();
    for (int tokenIndex = 0; tokenIndex < tokenCount; tokenIndex++) {
      CppToken currentToken = tokens.get(tokenIndex);

      if (isUsingNamespaceStd(tokens, tokenIndex, stdNamespaceAliases)) {
        hasUsingNamespaceStd = true;
        continue;
      }

      int importedSymbolIndex = findUsingStdSymbolIndex(tokens, tokenIndex, stdNamespaceAliases);
      if (importedSymbolIndex >= 0) {
        importedStdSymbols.add(tokens.get(importedSymbolIndex).text());
        directiveAndDeclarationTokenIndices.add(importedSymbolIndex);
        continue;
      }

      String aliasName = extractStdNamespaceAlias(tokens, tokenIndex, stdNamespaceAliases);
      if (aliasName != null) {
        stdNamespaceAliases.add(aliasName);
        continue;
      }

      if (currentToken.kind() == CppTokenKind.IDENTIFIER
          && tokenIndex + 1 < tokenCount
          && tokens.get(tokenIndex + 1).kind() == CppTokenKind.LEFT_PAREN
          && isFunctionDeclarationOrDefinition(tokens, tokenIndex)) {
        directiveAndDeclarationTokenIndices.add(tokenIndex);
        userDefinedFunctionNames.add(currentToken.text());
      }
    }

    return CppScopeContext.builder()
        .hasUsingNamespaceStd(hasUsingNamespaceStd)
        .importedStdSymbols(Set.copyOf(importedStdSymbols))
        .stdNamespaceAliases(Set.copyOf(stdNamespaceAliases))
        .directiveAndDeclarationTokenIndices(Set.copyOf(directiveAndDeclarationTokenIndices))
        .userDefinedFunctionNames(Set.copyOf(userDefinedFunctionNames))
        .build();
  }

  private static boolean isUsingNamespaceStd(
      List<CppToken> tokens, int index, Set<String> stdAliases) {
    int tokenCount = tokens.size();
    if (index + 2 >= tokenCount
        || !isIdentifierWithText(tokens.get(index), KEYWORD_USING)
        || !isIdentifierWithText(tokens.get(index + 1), KEYWORD_NAMESPACE)) {
      return false;
    }

    int targetNamespaceIndex = index + 2;
    if (tokens.get(targetNamespaceIndex).kind() == CppTokenKind.SCOPE_RESOLUTION) {
      targetNamespaceIndex++;
    }
    return targetNamespaceIndex < tokenCount
        && tokens.get(targetNamespaceIndex).kind() == CppTokenKind.IDENTIFIER
        && isStdOrAlias(tokens.get(targetNamespaceIndex).text(), stdAliases);
  }

  private static int findUsingStdSymbolIndex(
      List<CppToken> tokens, int index, Set<String> stdAliases) {
    int tokenCount = tokens.size();
    if (!isIdentifierWithText(tokens.get(index), KEYWORD_USING)) {
      return -1;
    }

    int namespaceIndex = index + 1;
    if (namespaceIndex < tokenCount
        && tokens.get(namespaceIndex).kind() == CppTokenKind.SCOPE_RESOLUTION) {
      namespaceIndex++;
    }
    if (namespaceIndex + 2 >= tokenCount) {
      return -1;
    }

    CppToken namespaceToken = tokens.get(namespaceIndex);
    CppToken scopeToken = tokens.get(namespaceIndex + 1);
    CppToken symbolToken = tokens.get(namespaceIndex + 2);

    boolean isValidUsingStdDeclaration =
        namespaceToken.kind() == CppTokenKind.IDENTIFIER
            && isStdOrAlias(namespaceToken.text(), stdAliases)
            && scopeToken.kind() == CppTokenKind.SCOPE_RESOLUTION
            && symbolToken.kind() == CppTokenKind.IDENTIFIER;
    return isValidUsingStdDeclaration ? namespaceIndex + 2 : -1;
  }

  private static String extractStdNamespaceAlias(
      List<CppToken> tokens, int index, Set<String> stdAliases) {
    int tokenCount = tokens.size();
    if (index + 3 >= tokenCount
        || !isIdentifierWithText(tokens.get(index), KEYWORD_NAMESPACE)
        || tokens.get(index + 1).kind() != CppTokenKind.IDENTIFIER
        || tokens.get(index + 2).kind() != CppTokenKind.ASSIGN) {
      return null;
    }

    int targetNamespaceIndex = index + 3;
    if (tokens.get(targetNamespaceIndex).kind() == CppTokenKind.SCOPE_RESOLUTION) {
      targetNamespaceIndex++;
    }
    if (targetNamespaceIndex < tokenCount
        && tokens.get(targetNamespaceIndex).kind() == CppTokenKind.IDENTIFIER
        && isStdOrAlias(tokens.get(targetNamespaceIndex).text(), stdAliases)) {
      return tokens.get(index + 1).text();
    }
    return null;
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

  private static boolean isIdentifierWithText(CppToken token, String expectedText) {
    return token.kind() == CppTokenKind.IDENTIFIER && expectedText.equals(token.text());
  }

  private static boolean isStdOrAlias(String namespaceName, Set<String> stdAliases) {
    return NAMESPACE_STD.equals(namespaceName) || stdAliases.contains(namespaceName);
  }
}
