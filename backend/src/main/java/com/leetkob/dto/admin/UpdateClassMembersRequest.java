package com.leetkob.dto.admin;

import java.util.List;

public record UpdateClassMembersRequest(List<String> taIds, List<String> studentIds) {}
