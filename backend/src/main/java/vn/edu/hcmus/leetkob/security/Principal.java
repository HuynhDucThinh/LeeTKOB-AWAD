package vn.edu.hcmus.leetkob.security;

/** Authenticated actor identity resolved from the HTTP Authorization header. */
public record Principal(String id, String role) {}
