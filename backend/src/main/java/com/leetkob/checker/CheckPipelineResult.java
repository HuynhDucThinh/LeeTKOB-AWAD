package com.leetkob.checker;

import java.util.List;
import lombok.Builder;

/** Full output of the shared constraint checking pipeline. */
@Builder
public record CheckPipelineResult(
    String strippedCode, List<ConstraintVerdictResult> verdicts, List<LlmCallRecord> callLogs) {}
