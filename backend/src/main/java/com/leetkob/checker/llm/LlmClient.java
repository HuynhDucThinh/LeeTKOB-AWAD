package com.leetkob.checker.llm;

/**
 * Contract for calling an LLM provider on a single sanitized semantic constraint prompt. Allows
 * deterministic fake/stub implementations in unit tests with zero network access.
 */
public interface LlmClient {

  String providerName();

  String modelName();

  LlmRawResponse evaluateConstraint(LlmPromptPayload payload);
}
