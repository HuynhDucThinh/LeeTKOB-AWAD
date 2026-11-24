package com.leetkob.publish.security;

/** Authenticated actor identity resolved from the HTTP Authorization header. */
public record Principal(String id, String role) {}
