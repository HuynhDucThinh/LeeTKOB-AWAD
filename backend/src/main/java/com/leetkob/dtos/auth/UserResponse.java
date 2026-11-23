package com.leetkob.dtos.auth;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

  private UUID id;
  private String email;
  private String fullName;
  private String avatarUrl;
  private OffsetDateTime createdAt;
}
