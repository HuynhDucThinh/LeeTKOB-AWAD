package com.leetkob.dtos.lab;

public record LabSummaryResponse(
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
    int unreviewedCount) {}
