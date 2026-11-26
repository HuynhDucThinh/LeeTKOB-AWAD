package com.leetkob.services;

import com.leetkob.dtos.admin.AdminUserResponse;
import com.leetkob.dtos.admin.ClassRoomResponse;
import com.leetkob.dtos.admin.CreateClassRequest;
import com.leetkob.dtos.admin.CreateUserRequest;
import com.leetkob.dtos.admin.UpdateClassMembersRequest;
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
