package com.leetkob.checker;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

/** Input bundle for running hard rules, LLM evaluation, and guardrails on an Accepted solution. */
@Builder
public record CheckRequest(
    String solutionKey,
    String statementMd,
    String sourceCode,
    List<String> piiTokensToStrip,
    List<ConstraintSpec> constraints,
    BigDecimal remainingBudgetUsd,
    boolean modelOff) {}
