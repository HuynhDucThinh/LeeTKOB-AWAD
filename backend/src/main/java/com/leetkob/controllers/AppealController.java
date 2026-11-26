package com.leetkob.controllers;

import com.leetkob.dtos.appeal.AppealResponse;
import com.leetkob.dtos.appeal.CreateAppealRequest;
import com.leetkob.dtos.appeal.DecideAppealRequest;
import com.leetkob.security.CustomUserDetails;
import com.leetkob.security.V1AuthHelper;
import com.leetkob.services.AppealService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class AppealController {

  private final V1AuthHelper authHelper;
  private final AppealService appealService;

  @GetMapping("/labs/{labId}/appeals")
  public ResponseEntity<List<AppealResponse>> listAppeals(@PathVariable int labId) {
    CustomUserDetails user = authHelper.requireRole("ta", "student");
    return ResponseEntity.ok(appealService.listAppeals(labId, user));
  }

  @PostMapping("/submissions/{submissionId}/appeals")
  public ResponseEntity<AppealResponse> createAppeal(
      @PathVariable int submissionId, @RequestBody CreateAppealRequest request) {
    CustomUserDetails studentUser = authHelper.requireRole("student");
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(appealService.createAppeal(submissionId, request, studentUser));
  }

  @PutMapping("/appeals/{appealId}/decision")
  public ResponseEntity<AppealResponse> decideAppeal(
      @PathVariable long appealId, @RequestBody DecideAppealRequest request) {
    CustomUserDetails taUser = authHelper.requireRole("ta");
    return ResponseEntity.ok(appealService.decideAppeal(appealId, request, taUser));
  }
}
