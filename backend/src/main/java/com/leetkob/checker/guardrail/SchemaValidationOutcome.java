package com.leetkob.checker.guardrail;

import lombok.Builder;

/** Result of G1/G5 strict JSON schema validation on a raw LLM response. */
@Builder
public record SchemaValidationOutcome(
    boolean valid, ParsedConstraintVerdict parsedVerdict, String errorDetail) {}
