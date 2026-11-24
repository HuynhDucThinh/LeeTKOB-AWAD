package com.leetkob.sandbox;

/** Single test case specification passed to the C++ sandbox runner. */
public record TestCaseSpec(
    String name, String inputData, String expectedOutput, int timeLimitMs, int memoryLimitMb) {}
