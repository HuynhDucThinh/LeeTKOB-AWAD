package com.leetkob.checker;

import com.leetkob.checker.hard.BanListHardRuleChecker;
import com.leetkob.checker.strip.CppCommentPiiStripper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Fallback {@link ConstraintCheckPipeline} registered via {@code AppConfig} until the full LLM +
 * Guardrails pipeline (PR 3B) is wired. Runs the real {@link CppCommentPiiStripper} and {@link
 * BanListHardRuleChecker} first for all ban-list constraints, and defaults undecided semantic
 * constraints to {@code pass} with 0 LLM calls.
 */
public class MockConstraintCheckPipeline implements ConstraintCheckPipeline {

  private final CppCommentPiiStripper stripper = CppCommentPiiStripper.builder().build();
  private final BanListHardRuleChecker hardRuleChecker = BanListHardRuleChecker.builder().build();

  @Override
  public CheckPipelineResult check(CheckRequest request) {
    if (request == null) {
      return CheckPipelineResult.builder()
          .strippedCode("")
          .verdicts(Collections.emptyList())
          .callLogs(Collections.emptyList())
          .build();
    }

    String strippedCode = stripper.stripSource(request.sourceCode(), request.piiTokensToStrip());
    if (request.constraints() == null || request.constraints().isEmpty()) {
      return CheckPipelineResult.builder()
          .strippedCode(strippedCode)
          .verdicts(Collections.emptyList())
          .callLogs(Collections.emptyList())
          .build();
    }

    List<ConstraintVerdictResult> verdicts = new ArrayList<>(request.constraints().size());
    for (ConstraintSpec constraint : request.constraints()) {
      Optional<ConstraintVerdictResult> hardRuleVerdict =
          hardRuleChecker.evaluate(constraint, request.sourceCode());
      verdicts.add(
          hardRuleVerdict.orElseGet(
              () ->
                  ConstraintVerdictResult.builder()
                      .constraintId(constraint.constraintId())
                      .verdict(CheckerConstants.VERDICT_PASS)
                      .source(CheckerConstants.SOURCE_HARD_RULE)
                      .evidence(Collections.emptyList())
                      .guardrailReason(null)
                      .build()));
    }

    return CheckPipelineResult.builder()
        .strippedCode(strippedCode)
        .verdicts(verdicts)
        .callLogs(Collections.emptyList())
        .build();
  }
}
