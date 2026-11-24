package com.leetkob.services;

import com.leetkob.dtos.auth.LoginRequest;
import com.leetkob.dtos.auth.SignupRequest;
import com.leetkob.dtos.auth.UserResponse;
import java.util.UUID;

public interface AuthService {

  AuthResult signup(SignupRequest request);

  AuthResult login(LoginRequest request);

  AuthResult refreshSession(UUID userId);

  UserResponse getUserById(UUID userId);

  record AuthResult(UserResponse userResponse, String token) {}
}
