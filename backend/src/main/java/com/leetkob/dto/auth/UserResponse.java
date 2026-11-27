package com.leetkob.dto.auth;

import java.time.OffsetDateTime;
import java.util.List;
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
  private String role;
  private List<Integer> classIds;
  private String avatarUrl;
  private OffsetDateTime createdAt;
}
