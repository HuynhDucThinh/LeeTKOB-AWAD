package com.leetkob.controller;

import com.leetkob.dto.admin.ClassRoomResponse;
import com.leetkob.security.CustomUserDetails;
import com.leetkob.security.V1AuthHelper;
import com.leetkob.service.AdminService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/classes")
@RequiredArgsConstructor
public class ClassController {

  private final V1AuthHelper authHelper;
  private final AdminService adminService;

  @GetMapping
  public ResponseEntity<List<ClassRoomResponse>> listMyClasses() {
    CustomUserDetails user = authHelper.requireRole("admin", "ta", "student");
    return ResponseEntity.ok(adminService.listClassesForUser(user));
  }
}
