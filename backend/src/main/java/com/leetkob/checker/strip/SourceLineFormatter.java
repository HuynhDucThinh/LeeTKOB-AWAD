package com.leetkob.checker.strip;

import com.leetkob.checker.lexical.CppSourceNormalizer;

/** Formats C++ source code with 1-based line numbers for LLM semantic evaluation prompts. */
public class SourceLineFormatter {

  private static final int LINE_PREFIX_CAPACITY_OVERHEAD = 6;
  private static final String LINE_NUMBER_SEPARATOR = ": ";

  public String formatWithLineNumbers(String sourceCode) {
    if (sourceCode == null || sourceCode.isEmpty()) {
      return "";
    }

    String normalizedSource = CppSourceNormalizer.normalizeLineEndings(sourceCode);
    String[] lines = normalizedSource.split("\n", -1);
    StringBuilder formattedOutput =
        new StringBuilder(normalizedSource.length() + lines.length * LINE_PREFIX_CAPACITY_OVERHEAD);

    for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
      if (lineIndex > 0) {
        formattedOutput.append('\n');
      }
      formattedOutput.append(lineIndex + 1).append(LINE_NUMBER_SEPARATOR).append(lines[lineIndex]);
    }
    return formattedOutput.toString();
  }
}
