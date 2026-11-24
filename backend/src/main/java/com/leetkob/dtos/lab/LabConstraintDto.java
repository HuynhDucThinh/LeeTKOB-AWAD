package com.leetkob.dtos.lab;

public record LabConstraintDto(
    String constraintId, String type, String description, int deductionTenths) {}
