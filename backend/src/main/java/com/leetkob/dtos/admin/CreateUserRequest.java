package com.leetkob.dtos.admin;

public record CreateUserRequest(
    String username, String displayName, String role, String password) {}
