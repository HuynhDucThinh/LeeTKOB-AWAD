package com.leetkob.controllers;

import com.leetkob.dtos.lab.CreateLabRequest;
import com.leetkob.dtos.lab.LabDetailResponse;
import com.leetkob.dtos.lab.LabSummaryResponse;
import com.leetkob.security.CustomUserDetails;
import com.leetkob.security.V1AuthHelper;
import com.leetkob.services.LabService;
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
@RequestMapping("/v1/labs")
@RequiredArgsConstructor
public class LabController {

  private final V1AuthHelper authHelper;
  private final LabService labService;

  @GetMapping
  public ResponseEntity<List<LabSummaryResponse>> listLabs() {
    CustomUserDetails user = authHelper.requireRole("admin", "ta", "student");
    return ResponseEntity.ok(labService.listLabs(user));
  }

  @GetMapping("/{labId}")
  public ResponseEntity<LabDetailResponse> getLab(@PathVariable int labId) {
    CustomUserDetails user = authHelper.requireRole("ta", "student");
    return ResponseEntity.ok(labService.getLabDetail(labId, user));
  }

  @PostMapping
  public ResponseEntity<LabDetailResponse> createLab(@RequestBody CreateLabRequest request) {
    CustomUserDetails taUser = authHelper.requireRole("ta");
    return ResponseEntity.status(HttpStatus.CREATED).body(labService.createLab(request, taUser));
  }

  @PutMapping("/{labId}")
  public ResponseEntity<LabDetailResponse> updateLab(
      @PathVariable int labId, @RequestBody CreateLabRequest request) {
    CustomUserDetails taUser = authHelper.requireRole("ta");
    return ResponseEntity.ok(labService.updateLab(labId, request, taUser));
  }
}
