package com.leetkob.dtos.review;

import java.util.List;

public record ReviewQueuePageResponse(
    List<ReviewItemResponse> items, long total, int page, int size) {}
