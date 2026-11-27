package com.leetkob.mapper;

import com.leetkob.dto.auth.SignupRequest;
import com.leetkob.dto.auth.UserResponse;
import com.leetkob.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

  @Mapping(target = "classIds", ignore = true)
  UserResponse toUserResponse(User user);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "passwordHash", ignore = true)
  @Mapping(target = "avatarUrl", ignore = true)
  @Mapping(target = "role", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  User toUser(SignupRequest request);
}
