package com.leetkob.checker.llm;

import lombok.Getter;

/**
 * Thrown when an LLM provider rejects a request, fails network I/O, or lacks required credentials.
 * Never carries API keys or raw Authorization headers.
 */
@Getter
public class LlmProviderException extends RuntimeException {

  private final String provider;
  private final String model;
  private final int statusCode;
  private final int latencyMs;

  public LlmProviderException(
      String provider, String model, int statusCode, int latencyMs, String message) {
    super(message);
    this.provider = provider;
    this.model = model;
    this.statusCode = statusCode;
    this.latencyMs = Math.max(latencyMs, 0);
  }

  public LlmProviderException(
      String provider,
      String model,
      int statusCode,
      int latencyMs,
      String message,
      Throwable cause) {
    super(message, cause);
    this.provider = provider;
    this.model = model;
    this.statusCode = statusCode;
    this.latencyMs = Math.max(latencyMs, 0);
  }
}
