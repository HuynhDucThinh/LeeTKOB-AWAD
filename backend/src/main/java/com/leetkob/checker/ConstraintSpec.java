package com.leetkob.checker;

/** Single constraint definition declared by a TA or loaded from constraints.json. */
public record ConstraintSpec(
    String constraintId, String type, String description, int deductionTenths) {}
