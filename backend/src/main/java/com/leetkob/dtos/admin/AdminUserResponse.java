package com.leetkob.dtos.admin;

import java.util.List;

public record AdminUserResponse(
    String id,
    String username,
    String displayName,
    String role,
    List<Integer> classIds,
    String createdAt) {}
