package com.leetkob.dto.lab;

public record CreateLabTestRequest(
    String name,
    String input,
    String expectedOutput,
    Integer timeLimitMs,
    Integer memoryLimitMb,
    Boolean isSample) {}
