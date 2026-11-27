package com.leetkob.checker.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetkob.checker.ConstraintSpec;
import com.leetkob.checker.ConstraintVerdictResult;
import com.leetkob.checker.hard.BanListHardRuleChecker;
import com.leetkob.checker.llm.LlmClient;
import com.leetkob.checker.llm.LlmPromptPayload;
import com.leetkob.checker.llm.LlmRawResponse;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link CheckerCliRunner}, {@link CliArguments}, and extended C++ syntax-aware
 * ban-list patterns (multi-segment namespaces, member calls, control-flow keywords, and original
 * source line mapping).
 */
class CheckerCliMainUnitTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void modelOffNeverConstructsLlmClientDecidesHardRulesAndMarksSemanticUnsure(@TempDir Path tempDir)
      throws Exception {
    Path datasetDir = tempDir.resolve("dataset");
    createSampleDataset(datasetDir);

    Path predictionsPath = tempDir.resolve("out").resolve("predictions.jsonl");
    AtomicInteger factoryCalls = new AtomicInteger(0);

    CheckerCliRunner runner =
        CheckerCliRunner.builder()
            .llmClientFactory(
                env -> {
                  factoryCalls.incrementAndGet();
                  throw new AssertionError("LlmClient must never be constructed when MODEL_OFF=1");
                })
            .build();

    CliArguments args =
        CliArguments.parse(new String[] {datasetDir.toString(), predictionsPath.toString()});
    runner.execute(args, Map.of("MODEL_OFF", "1"));

    assertEquals(0, factoryCalls.get());
    List<String> lines = Files.readAllLines(predictionsPath, StandardCharsets.UTF_8);
    assertEquals(4, lines.size());

    JsonNode row0 = objectMapper.readTree(lines.get(0));
    assertEquals("mini-sort/s01", row0.get("solution").asText());
    assertEquals("C1", row0.get("constraint").asText());
    assertEquals("pass", row0.get("verdict").asText());
    assertEquals(0, row0.get("evidence").size());

    JsonNode row1 = objectMapper.readTree(lines.get(1));
    assertEquals("mini-sort/s01", row1.get("solution").asText());
    assertEquals("C2", row1.get("constraint").asText());
    assertEquals("unsure", row1.get("verdict").asText());

    JsonNode row2 = objectMapper.readTree(lines.get(2));
    assertEquals("mini-sort/s02", row2.get("solution").asText());
    assertEquals("C1", row2.get("constraint").asText());
    assertEquals("violation", row2.get("verdict").asText());
    assertEquals(1, row2.get("evidence").size());
    assertEquals(4, row2.get("evidence").get(0).get("line").asInt());
    assertEquals(
        "std::sort(a, a + 3); // forbidden call", row2.get("evidence").get(0).get("text").asText());

    JsonNode row3 = objectMapper.readTree(lines.get(3));
    assertEquals("mini-sort/s02", row3.get("solution").asText());
    assertEquals("C2", row3.get("constraint").asText());
    assertEquals("unsure", row3.get("verdict").asText());
  }

  @Test
  void modelEnabledMapsVerifiedEvidenceBackToOriginalLineAndEnforcesRunBudgetCap(
      @TempDir Path tempDir) throws Exception {
    Path datasetDir = tempDir.resolve("dataset");
    createSampleDataset(datasetDir);

    Path predictionsPath = tempDir.resolve("out").resolve("predictions.jsonl");
    Path callLogPath = tempDir.resolve("out").resolve("call-log.jsonl");
    List<LlmPromptPayload> capturedPrompts = new ArrayList<>();

    LlmClient fakeClient =
        new LlmClient() {
          @Override
          public String providerName() {
            return "groq";
          }

          @Override
          public String modelName() {
            return "openai/gpt-oss-120b";
          }

          @Override
          public LlmRawResponse evaluateConstraint(LlmPromptPayload payload) {
            capturedPrompts.add(payload);
            // Cite stripped code on line 4 (without the trailing comment); pipeline must map back
            // to the full original line including "// bubble step" for eval.mjs compatibility.
            String content =
                "{\"verdicts\":[{\"constraintId\":\"C2\",\"verdict\":\"violation\","
                    + "\"evidence\":[{\"line\":4,\"text\":\"int x = 1;\"}]}]}";
            return LlmRawResponse.builder()
                .provider("groq")
                .model("openai/gpt-oss-120b")
                .providerRequestId("req_cli_test_1")
                .rawContent(content)
                .promptTokens(300)
                .completionTokens(60)
                .latencyMs(210)
                .costUsd(new BigDecimal("0.000500"))
                .build();
          }
        };

    CheckerCliRunner runner =
        CheckerCliRunner.builder().llmClientFactory(env -> fakeClient).build();

    CliArguments args =
        CliArguments.parse(
            new String[] {
              "--data",
              datasetDir.toString(),
              "--predictions",
              predictionsPath.toString(),
              "--call-log",
              callLogPath.toString()
            });

    // Budget $0.000400 allows s01 C2 ($0.000500 spent -> remaining -0.000100), so s02 C2 is cut off
    runner.execute(args, Map.of("MODEL_OFF", "0", "CHECKER_BUDGET_USD", "0.000400"));

    assertEquals(1, capturedPrompts.size());
    List<String> predLines = Files.readAllLines(predictionsPath, StandardCharsets.UTF_8);
    assertEquals(4, predLines.size());

    JsonNode s01C2 = objectMapper.readTree(predLines.get(1));
    assertEquals("violation", s01C2.get("verdict").asText());
    assertEquals(4, s01C2.get("evidence").get(0).get("line").asInt());
    assertEquals("int x = 1; // bubble step", s01C2.get("evidence").get(0).get("text").asText());

    JsonNode s02C2 = objectMapper.readTree(predLines.get(3));
    assertEquals("unsure", s02C2.get("verdict").asText());

    List<String> logLines = Files.readAllLines(callLogPath, StandardCharsets.UTF_8);
    assertEquals(1, logLines.size());
    assertTrue(logLines.get(0).contains("req_cli_test_1"));
  }

  @Test
  void multiSegmentNamespacesMemberCallsKeywordsAndVariableTrapsBehaveAccurately() {
    BanListHardRuleChecker checker = BanListHardRuleChecker.builder().build();

    ConstraintSpec rangesBan =
        ConstraintSpec.builder()
            .constraintId("C_RANGES")
            .type("hard")
            .description("Do not call `std::ranges::sort` or `rbegin`.")
            .deductionTenths(30)
            .build();

    String disguisedRangesAndMember =
        """
        #include <algorithm>
        #include <string>
        namespace rg = std::ranges;
        int main() {
            std::string s = "abc";
            auto it = s.rbegin();
            int a[3] = {3, 1, 2};
            rg::sort(a);
            return *it;
        }
        """;

    Optional<ConstraintVerdictResult> violation =
        checker.evaluate(rangesBan, disguisedRangesAndMember);
    assertTrue(violation.isPresent());
    assertEquals("violation", violation.get().verdict());
    assertEquals(2, violation.get().evidence().size());
    assertEquals(6, violation.get().evidence().get(0).line());
    assertEquals(8, violation.get().evidence().get(1).line());

    // Variable named `array` with `using namespace std;` must NOT falsely trigger `std::array`
    ConstraintSpec containerBan =
        ConstraintSpec.builder()
            .constraintId("C_CONT")
            .type("hard")
            .description("Do not use `std::array` or `std::vector`.")
            .deductionTenths(40)
            .build();
    String variableTrap =
        """
        #include <iostream>
        using namespace std;
        int main() {
            int* array = new int[4];
            array[0] = 7;
            cout << array[0];
            delete[] array;
            return 0;
        }
        """;
    Optional<ConstraintVerdictResult> trapResult = checker.evaluate(containerBan, variableTrap);
    assertTrue(trapResult.isPresent());
    assertEquals("pass", trapResult.get().verdict());

    // Loop keyword ban (`for`, `while`, `do`)
    ConstraintSpec loopBan =
        ConstraintSpec.builder()
            .constraintId("C_LOOP")
            .type("hard")
            .description("Do not use loop statements: `for`, `while`, `do`-`while`, or `goto`.")
            .deductionTenths(40)
            .build();
    Optional<ConstraintVerdictResult> loopResult =
        checker.evaluate(
            loopBan, "int main() {\n  // while in comment\n  for (int i=0;i<3;++i){}\n}");
    assertTrue(loopResult.isPresent());
    assertEquals("violation", loopResult.get().verdict());
    assertEquals(3, loopResult.get().evidence().get(0).line());
    assertFalse(loopResult.get().evidence().stream().anyMatch(e -> e.line() == 2));
  }

  private static void createSampleDataset(Path datasetDir) throws Exception {
    Path problemDir = datasetDir.resolve("problems").resolve("mini-sort");
    Path solutionsDir = datasetDir.resolve("solutions").resolve("mini-sort");
    Files.createDirectories(problemDir);
    Files.createDirectories(solutionsDir);

    Files.writeString(
        problemDir.resolve("statement.md"), "# Mini Sort\nSort integers.", StandardCharsets.UTF_8);
    Files.writeString(
        problemDir.resolve("constraints.json"),
        """
        [
          {"id": "C1", "text": "Do not call `std::sort`.", "deduction": 3},
          {"id": "C2", "text": "Sort using recursive merge sort.", "deduction": 4}
        ]
        """,
        StandardCharsets.UTF_8);

    Files.writeString(
        solutionsDir.resolve("s01.cpp"),
        """
        #include <iostream>
        // std::sort in comment must not fire
        int main() {
            int x = 1; // bubble step
            return x - 1;
        }
        """,
        StandardCharsets.UTF_8);

    Files.writeString(
        solutionsDir.resolve("s02.cpp"),
        """
        #include <algorithm>
        int main() {
            int a[3] = {3, 2, 1};
            std::sort(a, a + 3); // forbidden call
            return 0;
        }
        """,
        StandardCharsets.UTF_8);
  }
}
