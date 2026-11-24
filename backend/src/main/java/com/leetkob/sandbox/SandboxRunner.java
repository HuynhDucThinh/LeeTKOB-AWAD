package com.leetkob.sandbox;

import java.util.List;

/**
 * Isolated C++ compilation and execution contract (M1-M9). Implemented by {@code
 * DockerSandboxRunner} for production and CI sandbox tests.
 */
public interface SandboxRunner {
  SandboxRunResult compileAndRun(String sourceCode, List<TestCaseSpec> tests);
}
