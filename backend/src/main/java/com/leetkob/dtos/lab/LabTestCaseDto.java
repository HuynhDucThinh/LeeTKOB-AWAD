package com.leetkob.dtos.lab;

public record LabTestCaseDto(
    long id,
    String name,
    String input,
    String expectedOutput,
    int timeLimitMs,
    int memoryLimitMb,
    boolean isSample) {}
