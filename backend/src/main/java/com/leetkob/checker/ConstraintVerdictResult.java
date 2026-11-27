package com.leetkob.checker;

import java.util.List;
import lombok.Builder;

/** Per-constraint verdict produced by hard rules, the LLM, or a guardrail fallback. */
@Builder
public record ConstraintVerdictResult(
    String constraintId,
    String verdict,
    String source,
    List<EvidenceLine> evidence,
    String guardrailReason) {}
