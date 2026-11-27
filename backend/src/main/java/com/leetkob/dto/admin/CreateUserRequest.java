package com.leetkob.dto.admin;

public record CreateUserRequest(
    String username, String displayName, String role, String password) {}
