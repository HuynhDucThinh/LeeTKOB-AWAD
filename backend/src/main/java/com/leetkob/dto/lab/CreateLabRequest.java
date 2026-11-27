package com.leetkob.dto.lab;

import java.util.List;

public record CreateLabRequest(
    Integer classId,
    String title,
    String deadline,
    Double costCapUsd,
    String statementMd,
    List<CreateLabTestRequest> tests,
    List<LabConstraintDto> constraints) {}
