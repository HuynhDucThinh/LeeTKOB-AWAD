package com.leetkob.security.publish;

/** Authenticated actor identity resolved from the HTTP Authorization header. */
public record Principal(String id, String role) {}
