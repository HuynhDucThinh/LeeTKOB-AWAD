package com.leetkob.checker;

import java.util.List;

/** Per-constraint verdict produced by hard rules, the LLM, or a guardrail fallback. */
public record ConstraintVerdictResult(
    String constraintId,
    String verdict,
    String source,
    List<EvidenceLine> evidence,
    String guardrailReason) {}
