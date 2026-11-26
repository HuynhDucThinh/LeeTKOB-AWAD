package com.leetkob.controllers;

import com.leetkob.dtos.review.ReviewItemResponse;
import com.leetkob.dtos.review.ReviewQueuePageResponse;
import com.leetkob.dtos.review.SubmitTaDecisionRequest;
import com.leetkob.security.CustomUserDetails;
import com.leetkob.security.V1AuthHelper;
import com.leetkob.services.ReviewQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/labs/{labId}/review-queue")
@RequiredArgsConstructor
public class ReviewQueueController {

  private final V1AuthHelper authHelper;
  private final ReviewQueueService reviewQueueService;

  @GetMapping
  public ResponseEntity<ReviewQueuePageResponse> getReviewQueue(
      @PathVariable int labId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    CustomUserDetails taUser = authHelper.requireRole("ta");
    return ResponseEntity.ok(reviewQueueService.getReviewQueue(labId, page, size, taUser));
  }

  @PutMapping("/{submissionId}/{constraintId}")
  public ResponseEntity<ReviewItemResponse> submitDecision(
      @PathVariable int labId,
      @PathVariable int submissionId,
      @PathVariable String constraintId,
      @RequestBody SubmitTaDecisionRequest request) {
    CustomUserDetails taUser = authHelper.requireRole("ta");
    return ResponseEntity.ok(
        reviewQueueService.submitDecision(labId, submissionId, constraintId, request, taUser));
  }
}
