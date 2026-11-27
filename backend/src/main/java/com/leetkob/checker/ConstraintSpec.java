package com.leetkob.checker;

import lombok.Builder;

/** Single constraint definition declared by a TA or loaded from constraints.json. */
@Builder
public record ConstraintSpec(
    String constraintId, String type, String description, int deductionTenths) {}
