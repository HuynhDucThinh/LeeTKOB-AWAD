package com.leetkob.dtos.lab;

import com.leetkob.dtos.publish.PublicationReceipt;
import java.util.List;

public record LabDetailResponse(
    int id,
    int classId,
    String className,
    String title,
    String deadline,
    double costCapUsd,
    double costSpentUsd,
    boolean isPublished,
    String publishedAt,
    int submissionCount,
    int judgingPendingCount,
    int unreviewedCount,
    String statementMd,
    List<LabTestCaseDto> tests,
    List<LabConstraintDto> constraints,
    PublicationReceipt publicationReceipt) {}
