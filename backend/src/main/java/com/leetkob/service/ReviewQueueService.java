package com.leetkob.service;

import com.leetkob.dto.review.ReviewItemResponse;
import com.leetkob.dto.review.ReviewQueuePageResponse;
import com.leetkob.dto.review.SubmitTaDecisionRequest;
import com.leetkob.security.CustomUserDetails;

public interface ReviewQueueService {

  ReviewQueuePageResponse getReviewQueue(int labId, int page, int size, CustomUserDetails taUser);

  ReviewItemResponse submitDecision(
      int labId,
      int submissionId,
      String constraintId,
      SubmitTaDecisionRequest request,
      CustomUserDetails taUser);
}
