package com.leetkob.checker.llm;

import lombok.Builder;

/** Sanitized prompt bundle prepared for evaluating a single semantic constraint via an LLM. */
@Builder
public record LlmPromptPayload(
    String constraintId, String systemPrompt, String userPrompt, String combinedSanitizedPrompt) {}
