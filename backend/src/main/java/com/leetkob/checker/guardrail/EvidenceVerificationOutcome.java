package com.leetkob.checker.guardrail;

import com.leetkob.checker.EvidenceLine;
import java.util.List;
import lombok.Builder;

/** Result of G2 line-number and stripped-source evidence verification. */
@Builder
public record EvidenceVerificationOutcome(
    boolean valid, List<EvidenceLine> verifiedEvidence, String errorDetail) {}
