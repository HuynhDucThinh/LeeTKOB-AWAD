package com.leetkob.checker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.leetkob.checker.hard.BanListHardRuleChecker;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HardRuleTrapsUnitTest {

  private final BanListHardRuleChecker hardRuleChecker = BanListHardRuleChecker.builder().build();

  private static final ConstraintSpec BAN_STD_SORT =
      ConstraintSpec.builder()
          .constraintId("C1")
          .type("hard")
          .description("Do not call `std::sort` or `std::stable_sort` from the standard library.")
          .deductionTenths(30)
          .build();

  @Test
  void detectsDirectStdSortUsingNamespaceStdAndStdNamespaceAliasWithExactLines() {
    String directCallCode =
        String.join(
            "\n",
            "#include <algorithm>",
            "#include <vector>",
            "int main() {",
            "  std::vector<int> values = {3, 1, 2};",
            "  std::sort(values.begin(), values.end());",
            "  return 0;",
            "}");

    Optional<ConstraintVerdictResult> directResult =
        hardRuleChecker.evaluate(BAN_STD_SORT, directCallCode);
    assertTrue(directResult.isPresent());
    assertEquals(CheckerConstants.VERDICT_VIOLATION, directResult.get().verdict());
    assertEquals(CheckerConstants.SOURCE_HARD_RULE, directResult.get().source());
    assertEquals(1, directResult.get().evidence().size());
    assertEquals(5, directResult.get().evidence().getFirst().line());
    assertEquals(
        "std::sort(values.begin(), values.end());",
        directResult.get().evidence().getFirst().text());

    String usingAndAliasCode =
        String.join(
            "\n",
            "#include <algorithm>",
            "using namespace std;",
            "namespace std_alias = ::std;",
            "int main() {",
            "  int values[3] = {3, 1, 2};",
            "  stable_sort(values, values + 3);",
            "  std_alias::sort(values, values + 3);",
            "}");

    Optional<ConstraintVerdictResult> usingResult =
        hardRuleChecker.evaluate(BAN_STD_SORT, usingAndAliasCode);
    assertTrue(usingResult.isPresent());
    assertEquals(CheckerConstants.VERDICT_VIOLATION, usingResult.get().verdict());
    assertEquals(CheckerConstants.SOURCE_HARD_RULE, usingResult.get().source());
    assertEquals(2, usingResult.get().evidence().size());
    assertEquals(6, usingResult.get().evidence().get(0).line());
    assertEquals(7, usingResult.get().evidence().get(1).line());
  }

  @Test
  void trapDoesNotFireOnCommentsStringsOrAlgorithmHeaderUsedOnlyForSwap() {
    String compliantWithTraps =
        String.join(
            "\n",
            "#include <algorithm> // only for std::swap",
            "#include <iostream>",
            "/* Do not call std::sort(a, a + n); */",
            "int main() {",
            "  int left = 2, right = 1;",
            "  std::swap(left, right);",
            "  std::cout << \"std::sort(left, right)\";",
            "  return 0;",
            "}");

    Optional<ConstraintVerdictResult> verdict =
        hardRuleChecker.evaluate(BAN_STD_SORT, compliantWithTraps);
    assertTrue(verdict.isPresent());
    assertEquals(CheckerConstants.VERDICT_PASS, verdict.get().verdict());
    assertEquals(CheckerConstants.SOURCE_HARD_RULE, verdict.get().source());
    assertTrue(verdict.get().evidence().isEmpty());
  }

  @Test
  void trapDoesNotFireOnStudentOwnSortCustomNamespaceOrNestedNonStdNamespace() {
    String customNamespaceAndFunctionCode =
        String.join(
            "\n",
            "#include <algorithm>",
            "using namespace std;",
            "namespace myalgo {",
            "  void sort(int* values, int size) {",
            "    if (size > 1 && values[0] > values[1]) std::swap(values[0], values[1]);",
            "  }",
            "  namespace std {",
            "    void sort(int* values) {}",
            "  }",
            "}",
            "void sort(int* values, int left, int right) {",
            "  if (left < right) myalgo::sort(values + left, right - left + 1);",
            "}",
            "int main() {",
            "  int values[2] = {2, 1};",
            "  sort(values, 0, 1);",
            "  myalgo::sort(values, 2);",
            "  myalgo::std::sort(values);",
            "}");

    Optional<ConstraintVerdictResult> verdict =
        hardRuleChecker.evaluate(BAN_STD_SORT, customNamespaceAndFunctionCode);
    assertTrue(verdict.isPresent());
    assertEquals(CheckerConstants.VERDICT_PASS, verdict.get().verdict());
    assertEquals(CheckerConstants.SOURCE_HARD_RULE, verdict.get().source());
    assertTrue(verdict.get().evidence().isEmpty());
  }

  @Test
  void detectsBannedContainerHeaderMacroAliasAndGlobalUnqualifiedSymbol() {
    ConstraintSpec banVector =
        ConstraintSpec.builder()
            .constraintId("C2")
            .type("hard")
            .description("Do not use `std::vector` or `<vector>`.")
            .deductionTenths(20)
            .build();

    String vectorCode =
        String.join("\n", "#include <vector>", "int main() {", "  std::vector<int> numbers;", "}");

    Optional<ConstraintVerdictResult> vectorResult =
        hardRuleChecker.evaluate(banVector, vectorCode);
    assertTrue(vectorResult.isPresent());
    assertEquals(CheckerConstants.VERDICT_VIOLATION, vectorResult.get().verdict());
    assertEquals(CheckerConstants.SOURCE_HARD_RULE, vectorResult.get().source());
    assertEquals(2, vectorResult.get().evidence().size());
    assertEquals(1, vectorResult.get().evidence().get(0).line());
    assertEquals(3, vectorResult.get().evidence().get(1).line());

    String macroAliasCode =
        String.join(
            "\n",
            "#include <algorithm>",
            "#define MY_SORT std::sort",
            "int main() {",
            "  int values[2] = {2, 1};",
            "  MY_SORT(values, values + 2);",
            "}");
    Optional<ConstraintVerdictResult> macroResult =
        hardRuleChecker.evaluate(BAN_STD_SORT, macroAliasCode);
    assertTrue(macroResult.isPresent());
    assertEquals(CheckerConstants.VERDICT_VIOLATION, macroResult.get().verdict());
    assertEquals(2, macroResult.get().evidence().getFirst().line());

    ConstraintSpec banQsort =
        ConstraintSpec.builder()
            .constraintId("C3")
            .type("hard")
            .description("Do not call `qsort()`.")
            .deductionTenths(30)
            .build();
    String globalQsortCode =
        String.join(
            "\n", "#include <cstdlib>", "int main() {", "  ::qsort(nullptr, 0, 1, nullptr);", "}");
    Optional<ConstraintVerdictResult> qsortResult =
        hardRuleChecker.evaluate(banQsort, globalQsortCode);
    assertTrue(qsortResult.isPresent());
    assertEquals(CheckerConstants.VERDICT_VIOLATION, qsortResult.get().verdict());
    assertEquals(3, qsortResult.get().evidence().getFirst().line());
  }

  @Test
  void semanticConstraintReturnsEmptySoPipelineDelegatesToLlm() {
    ConstraintSpec semanticConstraint =
        ConstraintSpec.builder()
            .constraintId("C2")
            .type("semantic")
            .description(
                "Implement merge sort using divide-and-conquer rather than quadratic sorting.")
            .deductionTenths(40)
            .build();

    String compliantCode = "int main() { return 0; }";
    assertFalse(hardRuleChecker.isBanListConstraint(semanticConstraint));
    assertTrue(hardRuleChecker.evaluate(semanticConstraint, compliantCode).isEmpty());
  }
}
