package com.leetkob.checker.guardrail;

import com.leetkob.checker.EvidenceLine;
import java.util.List;
import lombok.Builder;

/** Schema-validated semantic verdict extracted from raw LLM JSON output before G2 line checks. */
@Builder
public record ParsedConstraintVerdict(
    String constraintId, String verdict, List<EvidenceLine> evidence) {}
