package com.leetkob.checker.hard;

import com.leetkob.checker.CheckerConstants;
import com.leetkob.checker.ConstraintSpec;
import com.leetkob.checker.ConstraintVerdictResult;
import com.leetkob.checker.EvidenceLine;
import com.leetkob.checker.lexical.CppSourceNormalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Syntax-aware hard-rule evaluator for ban-list constraints (descriptions starting with {@code "Do
 * not use"} or {@code "Do not call"} with backticked targets). Supports multi-segment C++
 * namespaces (such as {@code std::ranges::sort}), namespace aliases, {@code using} directives,
 * member calls, and C++ statement keywords, while returning {@link Optional#empty()} for semantic
 * constraints.
 */
@Builder
@AllArgsConstructor
public class BanListHardRuleChecker {

  private static final Pattern INCLUDE_HEADER_PATTERN =
      Pattern.compile("#\\s*include\\s*([<\"][^>\"]+[>\"])");

  private static final Set<String> CONTROL_FLOW_KEYWORDS =
      Set.of("for", "while", "do", "goto", "switch");

  private static final Set<String> PRIMITIVE_VARIABLE_TYPE_KEYWORDS =
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
          "uint32_t");

  @Builder.Default private final BanConstraintParser constraintParser = new BanConstraintParser();

  @Builder.Default private final CppLexer lexer = new CppLexer();

  @Builder.Default private final CppScopeAnalyzer scopeAnalyzer = new CppScopeAnalyzer();

  public BanListHardRuleChecker() {
    this(new BanConstraintParser(), new CppLexer(), new CppScopeAnalyzer());
  }

  public boolean isBanListConstraint(ConstraintSpec constraint) {
    return constraintParser.isBanListConstraint(constraint);
  }

  public List<String> extractBannedTargets(String description) {
    return constraintParser.extractRawTargets(description);
  }

  public Optional<ConstraintVerdictResult> evaluate(
      ConstraintSpec constraint, String rawSourceCode) {
    if (!isBanListConstraint(constraint)) {
      return Optional.empty();
    }

    String normalizedSource = CppSourceNormalizer.normalizeLineEndings(rawSourceCode);
    String[] originalSourceLines = normalizedSource.split("\n", -1);
    List<CppToken> tokens = lexer.tokenizeSource(normalizedSource);
    CppScopeContext scopeContext = scopeAnalyzer.analyze(tokens);

    List<BannedTarget> bannedTargets =
        constraintParser.parseBannedTargets(constraint.description());
    Map<Integer, String> evidenceByLineNumber = new TreeMap<>();

    for (BannedTarget bannedTarget : bannedTargets) {
      collectTargetViolations(
          bannedTarget, tokens, scopeContext, originalSourceLines, evidenceByLineNumber);
    }

    if (evidenceByLineNumber.isEmpty()) {
      return Optional.of(
          ConstraintVerdictResult.builder()
              .constraintId(constraint.constraintId())
              .verdict(CheckerConstants.VERDICT_PASS)
              .source(CheckerConstants.SOURCE_HARD_RULE)
              .evidence(List.of())
              .guardrailReason(null)
              .build());
    }

    List<EvidenceLine> sortedEvidence = buildEvidenceLines(evidenceByLineNumber);
    return Optional.of(
        ConstraintVerdictResult.builder()
            .constraintId(constraint.constraintId())
            .verdict(CheckerConstants.VERDICT_VIOLATION)
            .source(CheckerConstants.SOURCE_HARD_RULE)
            .evidence(sortedEvidence)
            .guardrailReason(null)
            .build());
  }

  private static void collectTargetViolations(
      BannedTarget bannedTarget,
      List<CppToken> tokens,
      CppScopeContext scopeContext,
      String[] originalSourceLines,
      Map<Integer, String> evidenceByLineNumber) {
    switch (bannedTarget.kind()) {
      case INCLUDE_HEADER ->
          collectHeaderViolations(
              bannedTarget.targetName(), tokens, originalSourceLines, evidenceByLineNumber);
      case QUALIFIED_SYMBOL ->
          collectQualifiedSymbolViolations(
              bannedTarget.namespacePath(),
              bannedTarget.targetName(),
              tokens,
              scopeContext,
              originalSourceLines,
              evidenceByLineNumber);
      case UNQUALIFIED_SYMBOL ->
          collectUnqualifiedSymbolViolations(
              bannedTarget.targetName(),
              tokens,
              scopeContext,
              originalSourceLines,
              evidenceByLineNumber);
    }
  }

  private static void collectHeaderViolations(
      String bannedHeader,
      List<CppToken> tokens,
      String[] originalSourceLines,
      Map<Integer, String> evidenceByLineNumber) {
    for (CppToken token : tokens) {
      if (token.kind() != CppTokenKind.INCLUDE_DIRECTIVE) {
        continue;
      }
      Matcher headerMatcher = INCLUDE_HEADER_PATTERN.matcher(token.text());
      if (headerMatcher.find() && bannedHeader.equals(headerMatcher.group(1))) {
        recordEvidenceLine(token.line(), originalSourceLines, evidenceByLineNumber);
      }
    }
  }

  private static void collectQualifiedSymbolViolations(
      List<String> targetNamespacePath,
      String bannedSymbol,
      List<CppToken> tokens,
      CppScopeContext scopeContext,
      String[] originalSourceLines,
      Map<Integer, String> evidenceByLineNumber) {
    int tokenCount = tokens.size();
    for (int tokenIndex = 0; tokenIndex < tokenCount; tokenIndex++) {
      CppToken candidateToken = tokens.get(tokenIndex);
      if (candidateToken.kind() != CppTokenKind.IDENTIFIER
          || !bannedSymbol.equals(candidateToken.text())
          || scopeContext.isDeclarationOrDirectiveToken(tokenIndex)
          || isMemberAccessTarget(tokens, tokenIndex)) {
        continue;
      }

      ExplicitQualification explicit = extractExplicitQualification(tokens, tokenIndex);
      boolean matches;
      if (!explicit.qualifiers().isEmpty()) {
        List<String> expanded =
            scopeContext.expandQualifiers(explicit.qualifiers(), explicit.leadingGlobalScope());
        matches =
            expanded.equals(targetNamespacePath)
                || (!explicit.leadingGlobalScope()
                    && scopeContext.matchesQualifiedViaUsingNamespace(
                        expanded, targetNamespacePath));
      } else if (explicit.leadingGlobalScope()) {
        matches = false;
      } else {
        matches =
            !scopeContext.isShadowedByUserSymbol(bannedSymbol)
                && !isPrimitiveTypedVariableName(tokens, tokenIndex)
                && scopeContext.isImportedSymbol(targetNamespacePath, bannedSymbol);
      }

      if (matches) {
        recordEvidenceLine(candidateToken.line(), originalSourceLines, evidenceByLineNumber);
      }
    }
  }

  private static void collectUnqualifiedSymbolViolations(
      String bannedSymbol,
      List<CppToken> tokens,
      CppScopeContext scopeContext,
      String[] originalSourceLines,
      Map<Integer, String> evidenceByLineNumber) {
    boolean isControlKeyword = CONTROL_FLOW_KEYWORDS.contains(bannedSymbol);
    int tokenCount = tokens.size();
    for (int tokenIndex = 0; tokenIndex < tokenCount; tokenIndex++) {
      CppToken candidateToken = tokens.get(tokenIndex);
      if (candidateToken.kind() != CppTokenKind.IDENTIFIER
          || !bannedSymbol.equals(candidateToken.text())) {
        continue;
      }

      if (isControlKeyword) {
        recordEvidenceLine(candidateToken.line(), originalSourceLines, evidenceByLineNumber);
        continue;
      }

      if (scopeContext.isDeclarationOrDirectiveToken(tokenIndex)) {
        continue;
      }

      if (isMemberAccessTarget(tokens, tokenIndex)) {
        boolean isMethodInvocation =
            tokenIndex + 1 < tokenCount
                && tokens.get(tokenIndex + 1).kind() == CppTokenKind.LEFT_PAREN
                && !scopeContext.isShadowedByUserFunction(bannedSymbol);
        if (isMethodInvocation) {
          recordEvidenceLine(candidateToken.line(), originalSourceLines, evidenceByLineNumber);
        }
        continue;
      }

      ExplicitQualification explicit = extractExplicitQualification(tokens, tokenIndex);
      boolean matches;
      if (!explicit.qualifiers().isEmpty()) {
        List<String> expanded =
            scopeContext.expandQualifiers(explicit.qualifiers(), explicit.leadingGlobalScope());
        matches = !expanded.isEmpty() && "std".equals(expanded.get(0));
      } else {
        matches =
            !scopeContext.isShadowedByUserSymbol(bannedSymbol)
                && !isPrimitiveTypedVariableName(tokens, tokenIndex);
      }

      if (matches) {
        recordEvidenceLine(candidateToken.line(), originalSourceLines, evidenceByLineNumber);
      }
    }
  }

  private static ExplicitQualification extractExplicitQualification(
      List<CppToken> tokens, int symbolIndex) {
    if (symbolIndex == 0 || tokens.get(symbolIndex - 1).kind() != CppTokenKind.SCOPE_RESOLUTION) {
      return new ExplicitQualification(List.of(), false);
    }

    List<String> reversedSegments = new ArrayList<>();
    int cursor = symbolIndex - 1;
    while (cursor >= 0 && tokens.get(cursor).kind() == CppTokenKind.SCOPE_RESOLUTION) {
      if (cursor - 1 >= 0 && tokens.get(cursor - 1).kind() == CppTokenKind.IDENTIFIER) {
        reversedSegments.add(tokens.get(cursor - 1).text());
        cursor -= 2;
      } else {
        Collections.reverse(reversedSegments);
        return new ExplicitQualification(List.copyOf(reversedSegments), true);
      }
    }

    Collections.reverse(reversedSegments);
    return new ExplicitQualification(List.copyOf(reversedSegments), false);
  }

  private static boolean isMemberAccessTarget(List<CppToken> tokens, int tokenIndex) {
    return tokenIndex >= 1 && tokens.get(tokenIndex - 1).kind() == CppTokenKind.MEMBER_ACCESS;
  }

  private static boolean isPrimitiveTypedVariableName(List<CppToken> tokens, int tokenIndex) {
    int cursor = tokenIndex - 1;
    while (cursor >= 0
        && tokens.get(cursor).kind() == CppTokenKind.OTHER_SYMBOL
        && ("*".equals(tokens.get(cursor).text()) || "&".equals(tokens.get(cursor).text()))) {
      cursor--;
    }
    return cursor >= 0
        && tokens.get(cursor).kind() == CppTokenKind.IDENTIFIER
        && PRIMITIVE_VARIABLE_TYPE_KEYWORDS.contains(tokens.get(cursor).text());
  }

  private static void recordEvidenceLine(
      int oneBasedLineNumber,
      String[] originalSourceLines,
      Map<Integer, String> evidenceByLineNumber) {
    if (oneBasedLineNumber >= 1 && oneBasedLineNumber <= originalSourceLines.length) {
      evidenceByLineNumber.putIfAbsent(
          oneBasedLineNumber, originalSourceLines[oneBasedLineNumber - 1].trim());
    }
  }

  private static List<EvidenceLine> buildEvidenceLines(Map<Integer, String> evidenceByLineNumber) {
    List<EvidenceLine> evidenceLines = new ArrayList<>(evidenceByLineNumber.size());
    for (Map.Entry<Integer, String> entry : evidenceByLineNumber.entrySet()) {
      evidenceLines.add(EvidenceLine.builder().line(entry.getKey()).text(entry.getValue()).build());
    }
    return evidenceLines;
  }

  private record ExplicitQualification(List<String> qualifiers, boolean leadingGlobalScope) {}
}
