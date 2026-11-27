package com.leetkob.checker;

import lombok.Builder;

/** 1-based original source line citation supporting a checker verdict. */
@Builder
public record EvidenceLine(int line, String text) {}
