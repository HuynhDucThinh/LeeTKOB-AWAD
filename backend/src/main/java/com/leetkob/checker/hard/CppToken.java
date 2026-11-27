package com.leetkob.checker.hard;

import lombok.Builder;

/** Immutable C++ lexical token carrying its 1-based original source line number. */
@Builder
public record CppToken(CppTokenKind kind, String text, int line) {}
