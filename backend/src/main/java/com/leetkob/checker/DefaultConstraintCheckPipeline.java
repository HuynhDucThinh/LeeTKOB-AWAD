package com.leetkob.checker;

import com.leetkob.checker.guardrail.EvidenceVerificationOutcome;
import com.leetkob.checker.guardrail.EvidenceVerifier;
import com.leetkob.checker.guardrail.ParsedConstraintVerdict;
import com.leetkob.checker.guardrail.ResponseSchemaValidator;
import com.leetkob.checker.guardrail.SchemaValidationOutcome;
import com.leetkob.checker.hard.BanListHardRuleChecker;
import com.leetkob.checker.lexical.CppSourceNormalizer;
import com.leetkob.checker.llm.LlmClient;
import com.leetkob.checker.llm.LlmPromptPayload;
import com.leetkob.checker.llm.LlmProviderException;
import com.leetkob.checker.llm.LlmRawResponse;
import com.leetkob.checker.llm.LlmTimeoutException;
import com.leetkob.checker.llm.PromptBuilder;
import com.leetkob.checker.strip.CppCommentPiiStripper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * Production {@link ConstraintCheckPipeline} shared identically between the async {@code
 * JudgeWorker} and the standalone Checker CLI.
 *
 * <p>Execution order per constraint:
 *
 * <ol>
 *   <li>Strip C++ comments and student PII while preserving 1-based line numbers.
 *   <li>Evaluate syntax-aware ban-list hard rules first ({@code 0} LLM calls when decided).
 *   <li>Honor {@code MODEL_OFF} ({@code 0} network calls, returns {@code unsure (model_off)}).
 *   <li>Enforce per-lab cost cap G4 ({@code 0} network calls once {@code remainingBudgetUsd <= 0}).
 *   <li>Invoke {@link LlmClient} with sanitized prompt, catching G3 timeouts and provider errors.
 *   <li>Validate G1/G5 strict JSON schema and G2 stripped-source line evidence.
 * </ol>
 */
@Slf4j
@Builder
@AllArgsConstructor
public class DefaultConstraintCheckPipeline implements ConstraintCheckPipeline {

  private final LlmClient llmClient;

  @Builder.Default
  private final CppCommentPiiStripper stripper = CppCommentPiiStripper.builder().build();

  @Builder.Default
  private final BanListHardRuleChecker hardRuleChecker = BanListHardRuleChecker.builder().build();

  @Builder.Default private final PromptBuilder promptBuilder = PromptBuilder.builder().build();

  @Builder.Default
  private final ResponseSchemaValidator schemaValidator = new ResponseSchemaValidator();

  @Builder.Default
  private final EvidenceVerifier evidenceVerifier = EvidenceVerifier.builder().build();

  @Override
  public CheckPipelineResult check(CheckRequest request) {
    if (request == null) {
      return CheckPipelineResult.builder()
          .strippedCode("")
          .verdicts(Collections.emptyList())
          .callLogs(Collections.emptyList())
          .build();
    }

    String rawSource = request.sourceCode() == null ? "" : request.sourceCode();
    String[] originalSourceLines =
        CppSourceNormalizer.normalizeLineEndings(rawSource).split("\n", -1);
    String strippedCode = stripper.stripSource(rawSource, request.piiTokensToStrip());

    List<ConstraintSpec> constraints = request.constraints();
    if (constraints == null || constraints.isEmpty()) {
      return CheckPipelineResult.builder()
          .strippedCode(strippedCode)
          .verdicts(Collections.emptyList())
          .callLogs(Collections.emptyList())
          .build();
    }

    List<ConstraintVerdictResult> verdicts = new ArrayList<>(constraints.size());
    List<LlmCallRecord> callLogs = new ArrayList<>();
    BigDecimal runningRemainingBudget = request.remainingBudgetUsd();

    for (ConstraintSpec constraint : constraints) {
      // 1. Hard-rule first: ban-list constraints are decided deterministically with 0 LLM calls.
      Optional<ConstraintVerdictResult> hardVerdict =
          hardRuleChecker.evaluate(constraint, rawSource);
      if (hardVerdict.isPresent()) {
        verdicts.add(hardVerdict.get());
        continue;
      }

      // 2. MODEL_OFF mode: never open network connections for semantic constraints.
      if (request.modelOff()) {
        verdicts.add(guardrailUnsure(constraint.constraintId(), CheckerConstants.REASON_MODEL_OFF));
        continue;
      }

      // 3. G4 Cost Cap: fail-closed with 0 LLM calls when remaining lab budget is exhausted.
      if (runningRemainingBudget == null
          || runningRemainingBudget.compareTo(BigDecimal.ZERO) <= 0) {
        verdicts.add(
            guardrailUnsure(constraint.constraintId(), CheckerConstants.REASON_COST_CAP_EXCEEDED));
        continue;
      }

      Objects.requireNonNull(llmClient, "llmClient must be configured when modelOff is false");
      LlmPromptPayload promptPayload =
          promptBuilder.buildConstraintPrompt(request.statementMd(), constraint, strippedCode);

      LlmRawResponse rawResponse;
      try {
        rawResponse = llmClient.evaluateConstraint(promptPayload);
      } catch (LlmTimeoutException timeoutEx) {
        callLogs.add(
            buildFailedCallRecord(
                constraint.constraintId(),
                timeoutEx.getProvider(),
                timeoutEx.getModel(),
                timeoutEx.getLatencyMs(),
                promptPayload.combinedSanitizedPrompt(),
                CheckerConstants.REASON_TIMEOUT));
        verdicts.add(guardrailUnsure(constraint.constraintId(), CheckerConstants.REASON_TIMEOUT));
        continue;
      } catch (LlmProviderException providerEx) {
        log.warn(
            "LLM provider error on constraintId={}: provider={}, model={}, statusCode={}, latencyMs={}, detail={}",
            constraint.constraintId(),
            providerEx.getProvider(),
            providerEx.getModel(),
            providerEx.getStatusCode(),
            providerEx.getLatencyMs(),
            providerEx.getMessage());
        callLogs.add(
            buildFailedCallRecord(
                constraint.constraintId(),
                providerEx.getProvider(),
                providerEx.getModel(),
                providerEx.getLatencyMs(),
                promptPayload.combinedSanitizedPrompt(),
                CheckerConstants.REASON_PROVIDER_ERROR));
        verdicts.add(
            guardrailUnsure(constraint.constraintId(), CheckerConstants.REASON_PROVIDER_ERROR));
        continue;
      }

      BigDecimal callCost = rawResponse.costUsd() != null ? rawResponse.costUsd() : BigDecimal.ZERO;
      runningRemainingBudget = runningRemainingBudget.subtract(callCost);

      // 4. G1 + G5 Schema Validation
      SchemaValidationOutcome schemaOutcome =
          schemaValidator.validate(rawResponse.rawContent(), constraint.constraintId());
      if (!schemaOutcome.valid()) {
        callLogs.add(
            buildCompletedCallRecord(
                constraint.constraintId(),
                promptPayload.combinedSanitizedPrompt(),
                rawResponse,
                CheckerConstants.VERDICT_UNSURE,
                CheckerConstants.REASON_SCHEMA_INVALID));
        verdicts.add(
            guardrailUnsure(constraint.constraintId(), CheckerConstants.REASON_SCHEMA_INVALID));
        continue;
      }

      ParsedConstraintVerdict parsedVerdict = schemaOutcome.parsedVerdict();

      // 5. G2 Evidence Verification against 1-based lines of strippedCode
      EvidenceVerificationOutcome evidenceOutcome =
          evidenceVerifier.verify(parsedVerdict.verdict(), parsedVerdict.evidence(), strippedCode);
      if (!evidenceOutcome.valid()) {
        callLogs.add(
            buildCompletedCallRecord(
                constraint.constraintId(),
                promptPayload.combinedSanitizedPrompt(),
                rawResponse,
                CheckerConstants.VERDICT_UNSURE,
                CheckerConstants.REASON_EVIDENCE_MISMATCH));
        verdicts.add(
            guardrailUnsure(constraint.constraintId(), CheckerConstants.REASON_EVIDENCE_MISMATCH));
        continue;
      }

      // 6. Verified LLM Verdict (map verified lines back to trimmed original .cpp lines)
      List<EvidenceLine> mappedOriginalEvidence =
          mapEvidenceToOriginalLines(evidenceOutcome.verifiedEvidence(), originalSourceLines);
      callLogs.add(
          buildCompletedCallRecord(
              constraint.constraintId(),
              promptPayload.combinedSanitizedPrompt(),
              rawResponse,
              parsedVerdict.verdict(),
              null));
      verdicts.add(
          ConstraintVerdictResult.builder()
              .constraintId(constraint.constraintId())
              .verdict(parsedVerdict.verdict())
              .source(CheckerConstants.SOURCE_LLM)
              .evidence(mappedOriginalEvidence)
              .guardrailReason(null)
              .build());
    }

    return CheckPipelineResult.builder()
        .strippedCode(strippedCode)
        .verdicts(List.copyOf(verdicts))
        .callLogs(List.copyOf(callLogs))
        .build();
  }

  private static List<EvidenceLine> mapEvidenceToOriginalLines(
      List<EvidenceLine> verifiedEvidence, String[] originalSourceLines) {
    if (verifiedEvidence == null || verifiedEvidence.isEmpty()) {
      return List.of();
    }
    Map<Integer, String> uniqueByLine = new LinkedHashMap<>();
    for (EvidenceLine item : verifiedEvidence) {
      int line = item.line();
      if (line >= 1 && line <= originalSourceLines.length) {
        uniqueByLine.putIfAbsent(line, originalSourceLines[line - 1].trim());
      }
    }
    List<EvidenceLine> mapped = new ArrayList<>(uniqueByLine.size());
    for (Map.Entry<Integer, String> entry : uniqueByLine.entrySet()) {
      mapped.add(EvidenceLine.builder().line(entry.getKey()).text(entry.getValue()).build());
    }
    return List.copyOf(mapped);
  }

  private static ConstraintVerdictResult guardrailUnsure(String constraintId, String reason) {
    return ConstraintVerdictResult.builder()
        .constraintId(constraintId)
        .verdict(CheckerConstants.VERDICT_UNSURE)
        .source(CheckerConstants.SOURCE_GUARDRAIL)
        .evidence(List.of())
        .guardrailReason(reason)
        .build();
  }

  private LlmCallRecord buildFailedCallRecord(
      String constraintId,
      String provider,
      String model,
      int latencyMs,
      String sanitizedPrompt,
      String guardrailReason) {
    String resolvedProvider =
        (provider == null || provider.isBlank()) ? llmClient.providerName() : provider;
    String resolvedModel = (model == null || model.isBlank()) ? llmClient.modelName() : model;
    return LlmCallRecord.builder()
        .constraintId(constraintId)
        .provider(resolvedProvider)
        .model(resolvedModel)
        .providerRequestId(null)
        .promptTokens(0)
        .completionTokens(0)
        .latencyMs(Math.max(latencyMs, 0))
        .costUsd(BigDecimal.ZERO)
        .sanitizedPrompt(sanitizedPrompt)
        .rawResponse(null)
        .verdict(CheckerConstants.VERDICT_UNSURE)
        .guardrailReason(guardrailReason)
        .build();
  }

  private static LlmCallRecord buildCompletedCallRecord(
      String constraintId,
      String sanitizedPrompt,
      LlmRawResponse rawResponse,
      String finalVerdict,
      String guardrailReason) {
    return LlmCallRecord.builder()
        .constraintId(constraintId)
        .provider(rawResponse.provider())
        .model(rawResponse.model())
        .providerRequestId(rawResponse.providerRequestId())
        .promptTokens(rawResponse.promptTokens())
        .completionTokens(rawResponse.completionTokens())
        .latencyMs(rawResponse.latencyMs())
        .costUsd(rawResponse.costUsd() != null ? rawResponse.costUsd() : BigDecimal.ZERO)
        .sanitizedPrompt(sanitizedPrompt)
        .rawResponse(rawResponse.rawContent())
        .verdict(finalVerdict)
        .guardrailReason(guardrailReason)
        .build();
  }
}
