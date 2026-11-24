package com.leetkob.checker;

import java.math.BigDecimal;

/** Immutable audit record for a single real LLM provider call. */
public record LlmCallRecord(
    String constraintId,
    String provider,
    String model,
    String providerRequestId,
    int promptTokens,
    int completionTokens,
    int latencyMs,
    BigDecimal costUsd,
    String sanitizedPrompt,
    String rawResponse,
    String verdict,
    String guardrailReason) {}
