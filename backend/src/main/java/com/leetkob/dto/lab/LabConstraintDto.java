package com.leetkob.dto.lab;

public record LabConstraintDto(
    String constraintId, String type, String description, int deductionTenths) {}
