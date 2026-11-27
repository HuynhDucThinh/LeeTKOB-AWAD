package com.leetkob.service;

import com.leetkob.dto.admin.AdminUserResponse;
import com.leetkob.dto.admin.ClassRoomResponse;
import com.leetkob.dto.admin.CreateClassRequest;
import com.leetkob.dto.admin.CreateUserRequest;
import com.leetkob.dto.admin.UpdateClassMembersRequest;
import com.leetkob.security.CustomUserDetails;
import java.util.List;

public interface AdminService {

  List<ClassRoomResponse> listAllClasses();

  List<ClassRoomResponse> listClassesForUser(CustomUserDetails user);

  ClassRoomResponse createClass(CreateClassRequest request);

  ClassRoomResponse updateClassMembers(int classId, UpdateClassMembersRequest request);

  List<AdminUserResponse> listUsers(String roleFilter);

  AdminUserResponse createUser(CreateUserRequest request);
}
