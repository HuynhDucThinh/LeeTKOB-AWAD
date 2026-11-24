package com.leetkob.sandbox;

/** Result of compiling and executing a student's C++ submission inside the sandbox. */
public record SandboxRunResult(
    String judgeStatus,
    int passedTests,
    int totalTests,
    Integer executionTimeMs,
    Integer memoryKb,
    String compileError) {}
