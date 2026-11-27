package com.leetkob.checker.hard;

import com.leetkob.checker.CheckerConstants;
import com.leetkob.checker.ConstraintSpec;
import com.leetkob.checker.ConstraintVerdictResult;
import com.leetkob.checker.EvidenceLine;
import com.leetkob.checker.lexical.CppSourceNormalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Syntax-aware hard-rule evaluator for ban-list constraints (descriptions starting with {@code "Do
 * not use"} or {@code "Do not call"} with backticked targets). Returns {@link Optional#empty()} for
 * semantic constraints so only undecided semantic constraints are sent to the LLM.
 */
@Builder
@AllArgsConstructor
public class BanListHardRuleChecker {

  private static final Pattern INCLUDE_HEADER_PATTERN =
      Pattern.compile("#\\s*include\\s*([<\"][^>\"]+[>\"])");

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

  private void collectTargetViolations(
      BannedTarget bannedTarget,
      List<CppToken> tokens,
      CppScopeContext scopeContext,
      String[] originalSourceLines,
      Map<Integer, String> evidenceByLineNumber) {
    switch (bannedTarget.kind()) {
      case INCLUDE_HEADER ->
          collectHeaderViolations(
              bannedTarget.targetName(), tokens, originalSourceLines, evidenceByLineNumber);
      case STD_QUALIFIED_SYMBOL ->
          collectSymbolViolations(
              bannedTarget.targetName(),
              true,
              tokens,
              scopeContext,
              originalSourceLines,
              evidenceByLineNumber);
      case UNQUALIFIED_SYMBOL ->
          collectSymbolViolations(
              bannedTarget.targetName(),
              false,
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

  private static void collectSymbolViolations(
      String bannedSymbol,
      boolean requireStdOrigin,
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

      QualificationKind qualification =
          resolveNamespaceQualification(tokens, tokenIndex, scopeContext);
      if (isBannedSymbolUsage(qualification, bannedSymbol, requireStdOrigin, scopeContext)) {
        recordEvidenceLine(candidateToken.line(), originalSourceLines, evidenceByLineNumber);
      }
    }
  }

  private static boolean isBannedSymbolUsage(
      QualificationKind qualification,
      String symbolName,
      boolean requireStdOrigin,
      CppScopeContext scopeContext) {
    return switch (qualification) {
      case STD_NAMESPACE -> true;
      case OTHER_NAMESPACE -> false;
      case GLOBAL_SCOPE -> !requireStdOrigin && !scopeContext.isShadowedByUserFunction(symbolName);
      case UNQUALIFIED ->
          !scopeContext.isShadowedByUserFunction(symbolName)
              && (!requireStdOrigin || scopeContext.isImportedFromStd(symbolName));
    };
  }

  private static QualificationKind resolveNamespaceQualification(
      List<CppToken> tokens, int symbolIndex, CppScopeContext scopeContext) {
    if (symbolIndex == 0 || tokens.get(symbolIndex - 1).kind() != CppTokenKind.SCOPE_RESOLUTION) {
      return QualificationKind.UNQUALIFIED;
    }

    if (symbolIndex == 1 || tokens.get(symbolIndex - 2).kind() != CppTokenKind.IDENTIFIER) {
      return QualificationKind.GLOBAL_SCOPE;
    }

    int qualifierIndex = symbolIndex - 2;
    boolean isNestedUnderOuterNamespace =
        qualifierIndex >= 2
            && tokens.get(qualifierIndex - 1).kind() == CppTokenKind.SCOPE_RESOLUTION
            && tokens.get(qualifierIndex - 2).kind() == CppTokenKind.IDENTIFIER;
    if (isNestedUnderOuterNamespace) {
      return QualificationKind.OTHER_NAMESPACE;
    }

    String qualifierName = tokens.get(qualifierIndex).text();
    return scopeContext.isStdNamespaceOrAlias(qualifierName)
        ? QualificationKind.STD_NAMESPACE
        : QualificationKind.OTHER_NAMESPACE;
  }

  private static boolean isMemberAccessTarget(List<CppToken> tokens, int tokenIndex) {
    return tokenIndex >= 1 && tokens.get(tokenIndex - 1).kind() == CppTokenKind.MEMBER_ACCESS;
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

  private enum QualificationKind {
    STD_NAMESPACE,
    OTHER_NAMESPACE,
    GLOBAL_SCOPE,
    UNQUALIFIED
  }
}
