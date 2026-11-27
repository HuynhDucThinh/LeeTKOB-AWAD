package com.leetkob.checker.llm;

import java.math.BigDecimal;
import lombok.Builder;

/** Raw HTTP completion metadata and content returned by an LLM provider prior to guardrails. */
@Builder
public record LlmRawResponse(
    String provider,
    String model,
    String providerRequestId,
    String rawContent,
    int promptTokens,
    int completionTokens,
    int latencyMs,
    BigDecimal costUsd) {}
