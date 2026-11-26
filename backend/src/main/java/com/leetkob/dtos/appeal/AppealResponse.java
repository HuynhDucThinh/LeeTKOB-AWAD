package com.leetkob.dtos.appeal;

public record AppealResponse(
    long id,
    int labId,
    int submissionId,
    String studentId,
    String constraintId,
    String constraintDescription,
    int deductionTenths,
    String reason,
    String status,
    String decisionReason,
    String decidedBy,
    String decidedAt,
    String createdAt) {}
