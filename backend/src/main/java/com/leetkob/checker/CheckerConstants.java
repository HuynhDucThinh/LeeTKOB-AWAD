package com.leetkob.checker;

/** Shared wire-format constants for constraint verdicts and verdict sources. */
public final class CheckerConstants {

  public static final String VERDICT_PASS = "pass";
  public static final String VERDICT_VIOLATION = "violation";
  public static final String VERDICT_UNSURE = "unsure";

  public static final String SOURCE_HARD_RULE = "hard_rule";
  public static final String SOURCE_LLM = "llm";
  public static final String SOURCE_GUARDRAIL = "guardrail";

  public static final String REASON_SCHEMA_INVALID = "schema_invalid";
  public static final String REASON_EVIDENCE_MISMATCH = "evidence_mismatch";
  public static final String REASON_TIMEOUT = "timeout";
  public static final String REASON_COST_CAP_EXCEEDED = "cost_cap_exceeded";
  public static final String REASON_MODEL_OFF = "model_off";
  public static final String REASON_PROVIDER_ERROR = "provider_error";

  private CheckerConstants() {}
}
