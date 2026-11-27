package com.leetkob.checker.cli;

import com.leetkob.checker.CheckPipelineResult;
import com.leetkob.checker.CheckRequest;
import com.leetkob.checker.CheckerConstants;
import com.leetkob.checker.ConstraintSpec;
import com.leetkob.checker.ConstraintVerdictResult;
import com.leetkob.checker.DefaultConstraintCheckPipeline;
import com.leetkob.checker.LlmCallRecord;
import com.leetkob.checker.cli.PredictionWriter.PredictionRow;
import com.leetkob.checker.cli.ProblemDatasetEntry.SolutionSourceEntry;
import com.leetkob.checker.llm.GroqLlmClient;
import com.leetkob.checker.llm.LlmCallLogger;
import com.leetkob.checker.llm.LlmClient;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * Standalone dataset-evaluation runner backing {@link CheckerCliMain}. Reuses the exact same {@link
 * DefaultConstraintCheckPipeline} as {@code JudgeWorker}, enforces {@code MODEL_OFF=1} without
 * instantiating any network client, and caps cumulative LLM spend via {@code CHECKER_BUDGET_USD}.
 */
@Slf4j
@Builder
@AllArgsConstructor
public class CheckerCliRunner {

  public static final BigDecimal DEFAULT_BUDGET_USD = new BigDecimal("0.500000");

  @Builder.Default private final DatasetLoader datasetLoader = new DatasetLoader();

  @Builder.Default private final PredictionWriter predictionWriter = new PredictionWriter();

  @Builder.Default private final LlmCallLogger callLogger = new LlmCallLogger();

  @Builder.Default
  private final Function<Map<String, String>, LlmClient> llmClientFactory =
      CheckerCliRunner::createDefaultLlmClient;

  public CheckerCliRunner() {
    this(
        new DatasetLoader(),
        new PredictionWriter(),
        new LlmCallLogger(),
        CheckerCliRunner::createDefaultLlmClient);
  }

  public void execute(CliArguments cliArgs, Map<String, String> env) throws IOException {
    Map<String, String> safeEnv = env == null ? Map.of() : env;
    boolean modelOff = isTruthy(safeEnv.get("MODEL_OFF"));
    BigDecimal runningBudgetUsd = parseBudgetUsd(safeEnv.get("CHECKER_BUDGET_USD"));

    // Structural offline guarantee: when MODEL_OFF is enabled, never construct an LlmClient.
    LlmClient llmClient = modelOff ? null : llmClientFactory.apply(safeEnv);
    DefaultConstraintCheckPipeline pipeline =
        DefaultConstraintCheckPipeline.builder().llmClient(llmClient).build();

    if (cliArgs.callLogFile() != null) {
      Files.deleteIfExists(cliArgs.callLogFile());
    }

    List<ProblemDatasetEntry> problems = datasetLoader.loadDataset(cliArgs.datasetDir());
    List<PredictionRow> predictionRows = new ArrayList<>();

    for (ProblemDatasetEntry problem : problems) {
      for (SolutionSourceEntry solution : problem.solutions()) {
        try {
          CheckRequest request =
              CheckRequest.builder()
                  .solutionKey(solution.solutionKey())
                  .statementMd(problem.statementMd())
                  .sourceCode(solution.sourceCode())
                  .piiTokensToStrip(List.of())
                  .constraints(problem.constraints())
                  .remainingBudgetUsd(runningBudgetUsd)
                  .modelOff(modelOff)
                  .build();

          CheckPipelineResult result = pipeline.check(request);
          for (ConstraintVerdictResult verdict : result.verdicts()) {
            predictionRows.add(new PredictionRow(solution.solutionKey(), verdict));
          }

          if (result.callLogs() != null && !result.callLogs().isEmpty()) {
            for (LlmCallRecord record : result.callLogs()) {
              if (record.costUsd() != null) {
                runningBudgetUsd = runningBudgetUsd.subtract(record.costUsd());
              }
            }
            if (cliArgs.callLogFile() != null) {
              callLogger.appendRecords(
                  cliArgs.callLogFile(), solution.solutionKey(), result.callLogs());
            }
          }
        } catch (RuntimeException ex) {
          log.warn(
              "Unexpected error evaluating solution '{}', emitting unsure fallback: {}",
              solution.solutionKey(),
              ex.getMessage());
          for (ConstraintSpec constraint : problem.constraints()) {
            predictionRows.add(
                new PredictionRow(
                    solution.solutionKey(),
                    ConstraintVerdictResult.builder()
                        .constraintId(constraint.constraintId())
                        .verdict(CheckerConstants.VERDICT_UNSURE)
                        .source(CheckerConstants.SOURCE_GUARDRAIL)
                        .evidence(List.of())
                        .guardrailReason(CheckerConstants.REASON_PROVIDER_ERROR)
                        .build()));
          }
        }
      }
    }

    predictionWriter.writePredictions(cliArgs.predictionsFile(), predictionRows);
  }

  private static LlmClient createDefaultLlmClient(Map<String, String> env) {
    return GroqLlmClient.builder()
        .provider(env.getOrDefault("MODEL_PROVIDER", GroqLlmClient.DEFAULT_PROVIDER))
        .model(env.getOrDefault("MODEL_NAME", GroqLlmClient.DEFAULT_MODEL))
        .apiKey(env.getOrDefault("MODEL_API_KEY", ""))
        .build();
  }

  private static boolean isTruthy(String raw) {
    if (raw == null) {
      return false;
    }
    String trimmed = raw.trim();
    return "1".equals(trimmed) || "true".equalsIgnoreCase(trimmed);
  }

  private static BigDecimal parseBudgetUsd(String raw) {
    if (raw == null || raw.isBlank()) {
      return DEFAULT_BUDGET_USD;
    }
    try {
      return new BigDecimal(raw.trim());
    } catch (NumberFormatException ex) {
      return DEFAULT_BUDGET_USD;
    }
  }
}
