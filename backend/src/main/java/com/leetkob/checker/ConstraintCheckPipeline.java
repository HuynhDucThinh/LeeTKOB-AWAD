package com.leetkob.checker;

/**
 * Shared constraint-checking pipeline invoked identically by the async JudgeWorker and the
 * standalone Checker CLI (MODEL_OFF=1 / real model).
 */
public interface ConstraintCheckPipeline {
  CheckPipelineResult check(CheckRequest request);
}
