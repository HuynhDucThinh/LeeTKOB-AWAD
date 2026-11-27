package com.leetkob.controller;

import com.leetkob.dto.admin.AdminUserResponse;
import com.leetkob.dto.admin.ClassRoomResponse;
import com.leetkob.dto.admin.CreateClassRequest;
import com.leetkob.dto.admin.CreateUserRequest;
import com.leetkob.dto.admin.UpdateClassMembersRequest;
import com.leetkob.security.V1AuthHelper;
import com.leetkob.service.AdminService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class AdminController {

  private final V1AuthHelper authHelper;
  private final AdminService adminService;

  @GetMapping("/classes")
  public ResponseEntity<List<ClassRoomResponse>> listClasses() {
    authHelper.requireRole("admin");
    return ResponseEntity.ok(adminService.listAllClasses());
  }

  @PostMapping("/classes")
  public ResponseEntity<ClassRoomResponse> createClass(@RequestBody CreateClassRequest request) {
    authHelper.requireRole("admin");
    return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createClass(request));
  }

  @PutMapping("/classes/{classId}/members")
  public ResponseEntity<ClassRoomResponse> updateClassMembers(
      @PathVariable int classId, @RequestBody UpdateClassMembersRequest request) {
    authHelper.requireRole("admin");
    return ResponseEntity.ok(adminService.updateClassMembers(classId, request));
  }

  @GetMapping("/users")
  public ResponseEntity<List<AdminUserResponse>> listUsers(
      @RequestParam(required = false) String role) {
    authHelper.requireRole("admin");
    return ResponseEntity.ok(adminService.listUsers(role));
  }

  @PostMapping("/users")
  public ResponseEntity<AdminUserResponse> createUser(@RequestBody CreateUserRequest request) {
    authHelper.requireRole("admin");
    return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createUser(request));
  }
}
