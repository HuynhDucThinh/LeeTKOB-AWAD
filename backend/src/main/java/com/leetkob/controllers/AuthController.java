package com.leetkob.controllers;

import com.leetkob.dtos.ApiResponse;
import com.leetkob.dtos.auth.AuthResponse;
import com.leetkob.dtos.auth.LoginRequest;
import com.leetkob.dtos.auth.SignupRequest;
import com.leetkob.dtos.auth.UserResponse;
import com.leetkob.security.CustomUserDetails;
import com.leetkob.security.jwt.CookieUtil;
import com.leetkob.services.user.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final CookieUtil cookieUtil;

  @PostMapping("/signup")
  public ResponseEntity<ApiResponse<AuthResponse>> signup(
      @Valid @RequestBody SignupRequest request) {
    AuthService.AuthResult result = authService.signup(request);
    ResponseCookie cookie = cookieUtil.createAccessTokenCookie(result.token());
    AuthResponse payload =
        AuthResponse.builder().accessToken(result.token()).user(result.userResponse()).build();

    return ResponseEntity.status(HttpStatus.CREATED)
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(ApiResponse.success("User registered successfully", payload));
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
    AuthService.AuthResult result = authService.login(request);
    ResponseCookie cookie = cookieUtil.createAccessTokenCookie(result.token());
    AuthResponse payload =
        AuthResponse.builder().accessToken(result.token()).user(result.userResponse()).build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(ApiResponse.success("Login successful", payload));
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    if (userDetails == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.error("Not authenticated"));
    }
    AuthService.AuthResult result = authService.refreshSession(userDetails.getId());
    ResponseCookie cookie = cookieUtil.createAccessTokenCookie(result.token());
    AuthResponse payload =
        AuthResponse.builder().accessToken(result.token()).user(result.userResponse()).build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(ApiResponse.success("Session refreshed", payload));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout() {
    ResponseCookie cleanCookie = cookieUtil.createCleanAccessTokenCookie();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
        .body(ApiResponse.success("Logged out successfully", null));
  }

  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    if (userDetails == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.error("Not authenticated"));
    }
    UserResponse response = authService.getUserById(userDetails.getId());
    return ResponseEntity.ok(ApiResponse.success("User profile retrieved", response));
  }
}
