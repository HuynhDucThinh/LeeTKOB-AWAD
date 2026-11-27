package com.leetkob.checker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.leetkob.checker.strip.CppCommentPiiStripper;
import com.leetkob.checker.strip.StudentPiiRedactor;
import java.util.List;
import org.junit.jupiter.api.Test;

class StripperLineMapUnitTest {

  private final CppCommentPiiStripper stripper = CppCommentPiiStripper.builder().build();

  @Test
  void stripsLineAndBlockCommentsWhilePreservingExactLineNumbers() {
    String rawSource =
        String.join(
            "\n",
            "// Student: Nguyen Van A (23120199)",
            "#include <iostream>",
            "/*",
            " * Multi-line comment",
            " * std::sort(a, a + n);",
            " */",
            "int main() {",
            "  int x = 1; // inline comment",
            "  int/*mid*/y = 2;",
            "  return x + y;",
            "}");

    String strippedSource = stripper.stripSource(rawSource, List.of());
    String[] rawLines = rawSource.split("\n", -1);
    String[] strippedLines = strippedSource.split("\n", -1);

    assertEquals(rawLines.length, strippedLines.length);
    assertEquals("", strippedLines[0].trim());
    assertEquals("#include <iostream>", strippedLines[1]);
    assertEquals("", strippedLines[2].trim());
    assertEquals("", strippedLines[3].trim());
    assertEquals("", strippedLines[4].trim());
    assertEquals("", strippedLines[5].trim());
    assertEquals("int main() {", strippedLines[6]);
    assertEquals("int x = 1;", strippedLines[7].trim());
    assertEquals("int y = 2;", strippedLines[8].trim());
    assertEquals("return x + y;", strippedLines[9].trim());
    assertEquals("}", strippedLines[10]);
  }

  @Test
  void preservesCommentLikeSequencesInsideStringsAndRawStrings() {
    String rawSource =
        String.join(
            "\n",
            "#include <string>",
            "std::string a = \"https://example.com // keep\";",
            "std::string b = \"/* not a comment */\";",
            "std::string c = R\"delim(// raw comment /* keep */)delim\";",
            "char q = '/'; // trailing comment");

    String strippedSource = stripper.stripSource(rawSource, List.of());
    String[] strippedLines = strippedSource.split("\n", -1);

    assertEquals(5, strippedLines.length);
    assertTrue(strippedLines[1].contains("https://example.com // keep"));
    assertTrue(strippedLines[2].contains("/* not a comment */"));
    assertTrue(strippedLines[3].contains("// raw comment /* keep */"));
    assertFalse(strippedLines[4].contains("trailing comment"));
    assertTrue(strippedLines[4].contains("char q = '/';"));
  }

  @Test
  void stripsStudentPiiTokensWithoutCorruptingCppKeywordsOrShiftingLines() {
    String rawSource =
        String.join(
            "\n",
            "#include <cstdio>",
            "int main() {",
            "  const char* author = \"STUDENT1@leetkob.edu - sv_23120199 - An\";",
            "  int inputValue = 0;",
            "  std::scanf(\"%d\", &inputValue);",
            "  return inputValue;",
            "}");

    String strippedSource =
        stripper.stripSource(rawSource, List.of("student1@leetkob.edu", "23120199", "An", "in"));
    String[] strippedLines = strippedSource.split("\n", -1);

    assertEquals(7, strippedLines.length);
    assertEquals("int main() {", strippedLines[1]);
    assertTrue(strippedLines[3].contains("int inputValue = 0;"));
    assertTrue(strippedLines[4].contains("std::scanf"));
    assertFalse(strippedSource.toLowerCase().contains("student1@leetkob.edu"));
    assertFalse(strippedSource.contains("23120199"));
    assertTrue(strippedLines[2].contains(StudentPiiRedactor.REDACTED_PLACEHOLDER));
  }
}
