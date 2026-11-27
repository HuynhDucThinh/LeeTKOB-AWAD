package com.leetkob.service;

import com.leetkob.dto.auth.LoginRequest;
import com.leetkob.dto.auth.SignupRequest;
import com.leetkob.dto.auth.UserResponse;
import java.util.UUID;

public interface AuthService {

  AuthResult signup(SignupRequest request);

  AuthResult login(LoginRequest request);

  AuthResult refreshSession(UUID userId);

  UserResponse getUserById(UUID userId);

  record AuthResult(UserResponse userResponse, String token) {}
}
