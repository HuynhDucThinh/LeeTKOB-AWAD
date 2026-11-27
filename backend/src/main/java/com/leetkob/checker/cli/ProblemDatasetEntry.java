package com.leetkob.checker.cli;

import com.leetkob.checker.ConstraintSpec;
import java.nio.file.Path;
import java.util.List;
import lombok.Builder;

/** In-memory representation of a single problem and its C++ solutions loaded from a dataset dir. */
@Builder
public record ProblemDatasetEntry(
    String problemId,
    String statementMd,
    List<ConstraintSpec> constraints,
    List<SolutionSourceEntry> solutions) {

  @Builder
  public record SolutionSourceEntry(String solutionKey, Path sourcePath, String sourceCode) {}
}
