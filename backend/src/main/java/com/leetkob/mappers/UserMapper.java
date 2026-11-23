package com.leetkob.mappers;

import com.leetkob.dtos.auth.SignupRequest;
import com.leetkob.dtos.auth.UserResponse;
import com.leetkob.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    User toUser(SignupRequest request);
}
