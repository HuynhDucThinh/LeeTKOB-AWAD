package com.leetkob.checker;

import java.util.List;

/** Full output of the shared constraint checking pipeline. */
public record CheckPipelineResult(
    String strippedCode, List<ConstraintVerdictResult> verdicts, List<LlmCallRecord> callLogs) {}
