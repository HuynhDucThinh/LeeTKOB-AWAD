package com.leetkob.dto.lab;

public record LabTestCaseDto(
    long id,
    String name,
    String input,
    String expectedOutput,
    int timeLimitMs,
    int memoryLimitMb,
    boolean isSample) {}
