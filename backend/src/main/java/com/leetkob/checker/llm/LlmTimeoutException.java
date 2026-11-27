package com.leetkob.checker.llm;

import lombok.Getter;

/** Thrown when an LLM provider call exceeds the configured connect or request timeout (G3). */
@Getter
public class LlmTimeoutException extends RuntimeException {

  private final String provider;
  private final String model;
  private final int latencyMs;

  public LlmTimeoutException(String provider, String model, int latencyMs, String message) {
    super(message);
    this.provider = provider;
    this.model = model;
    this.latencyMs = Math.max(latencyMs, 0);
  }

  public LlmTimeoutException(
      String provider, String model, int latencyMs, String message, Throwable cause) {
    super(message, cause);
    this.provider = provider;
    this.model = model;
    this.latencyMs = Math.max(latencyMs, 0);
  }
}
