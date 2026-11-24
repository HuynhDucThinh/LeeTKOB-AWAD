package com.leetkob.dtos.admin;

import java.util.List;

public record ClassRoomResponse(
    int id, String name, List<String> taIds, List<String> studentIds, int labCount) {}
