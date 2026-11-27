package com.leetkob.checker;

/** Shared wire-format constants for constraint verdicts and verdict sources. */
public final class CheckerConstants {

  public static final String VERDICT_PASS = "pass";
  public static final String VERDICT_VIOLATION = "violation";
  public static final String VERDICT_UNSURE = "unsure";

  public static final String SOURCE_HARD_RULE = "hard_rule";
  public static final String SOURCE_LLM = "llm";
  public static final String SOURCE_GUARDRAIL = "guardrail";

  private CheckerConstants() {}
}
