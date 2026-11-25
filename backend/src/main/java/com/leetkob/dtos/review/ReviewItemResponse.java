package com.leetkob.dtos.review;

import com.leetkob.checker.EvidenceLine;
import java.util.List;

public record ReviewItemResponse(
    int submissionId,
    int labId,
    String studentId,
    String constraintId,
    String constraintType,
    String constraintDescription,
    int deductionTenths,
    String verdict,
    String source,
    List<EvidenceLine> evidence,
    String guardrailReason,
    boolean sampledPass,
    String decision,
    String decidedBy,
    String decidedAt,
    String sourceCode,
    String strippedCode) {}
