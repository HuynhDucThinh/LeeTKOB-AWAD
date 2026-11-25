package com.leetkob.services;

import com.leetkob.dtos.review.ReviewItemResponse;
import com.leetkob.dtos.review.ReviewQueuePageResponse;
import com.leetkob.dtos.review.SubmitTaDecisionRequest;
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
