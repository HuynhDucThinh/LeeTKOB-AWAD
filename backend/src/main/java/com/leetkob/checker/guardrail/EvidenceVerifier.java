package com.leetkob.checker.guardrail;

import com.leetkob.checker.CheckerConstants;
import com.leetkob.checker.EvidenceLine;
import com.leetkob.checker.lexical.CppSourceNormalizer;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Enforces G2 (evidence grounding against 1-based lines of comment/PII-stripped C++ source code).
 * Rejects empty evidence on {@code violation} verdicts, out-of-range line numbers, fabricated
 * snippets, snippets cited on the wrong line, and snippets that only existed in stripped comments.
 */
@Builder
@AllArgsConstructor
public class EvidenceVerifier {

  public EvidenceVerificationOutcome verify(
      String verdict, List<EvidenceLine> citedEvidence, String strippedCode) {
    String normalizedStripped =
        CppSourceNormalizer.normalizeLineEndings(strippedCode == null ? "" : strippedCode);
    String[] strippedLines =
        normalizedStripped.isEmpty() ? new String[0] : normalizedStripped.split("\n", -1);

    List<EvidenceLine> safeEvidence = citedEvidence == null ? List.of() : citedEvidence;
    if (CheckerConstants.VERDICT_VIOLATION.equals(verdict) && safeEvidence.isEmpty()) {
      return invalid("Violation verdict must cite at least one evidence line");
    }

    List<EvidenceLine> verified = new ArrayList<>(safeEvidence.size());
    for (EvidenceLine item : safeEvidence) {
      if (item == null) {
        return invalid("Null evidence item");
      }
      int line = item.line();
      if (line < 1 || line > strippedLines.length) {
        return invalid("Cited line " + line + " is out of range (1.." + strippedLines.length + ")");
      }

      String citedTrimmed = item.text() == null ? "" : item.text().trim();
      if (citedTrimmed.isEmpty()) {
        return invalid("Cited evidence text at line " + line + " is blank");
      }

      String actualTrimmed = strippedLines[line - 1].trim();
      if (actualTrimmed.isEmpty()) {
        return invalid("Line " + line + " is empty after comment stripping");
      }

      String normalizedActual = collapseHorizontalWhitespace(actualTrimmed);
      String normalizedCited = collapseHorizontalWhitespace(citedTrimmed);
      if (!normalizedActual.contains(normalizedCited)) {
        return invalid("Cited text does not match stripped source on line " + line);
      }

      verified.add(EvidenceLine.builder().line(line).text(citedTrimmed).build());
    }

    return EvidenceVerificationOutcome.builder()
        .valid(true)
        .verifiedEvidence(List.copyOf(verified))
        .errorDetail(null)
        .build();
  }

  private static String collapseHorizontalWhitespace(String input) {
    StringBuilder out = new StringBuilder(input.length());
    boolean previousWasSpace = false;
    for (int i = 0; i < input.length(); i++) {
      char ch = input.charAt(i);
      if (ch == ' ' || ch == '\t') {
        if (!previousWasSpace) {
          out.append(' ');
          previousWasSpace = true;
        }
      } else {
        out.append(ch);
        previousWasSpace = false;
      }
    }
    return out.toString();
  }

  private static EvidenceVerificationOutcome invalid(String detail) {
    return EvidenceVerificationOutcome.builder()
        .valid(false)
        .verifiedEvidence(List.of())
        .errorDetail(detail)
        .build();
  }
}
