package com.leetkob.checker.hard;

import java.util.List;
import lombok.Builder;

/** Parsed representation of a single banned header or C++ symbol in a ban-list constraint. */
@Builder
public record BannedTarget(BannedTargetKind kind, List<String> namespacePath, String targetName) {}
