package com.leetkob.dto.review;

import java.util.List;

public record ReviewQueuePageResponse(
    List<ReviewItemResponse> items, long total, int page, int size) {}
