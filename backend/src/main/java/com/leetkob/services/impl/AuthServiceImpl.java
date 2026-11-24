package com.leetkob.services.impl;

import com.leetkob.dtos.auth.LoginRequest;
import com.leetkob.dtos.auth.SignupRequest;
import com.leetkob.dtos.auth.UserResponse;
import com.leetkob.entities.User;
import com.leetkob.exceptions.EmailAlreadyExistsException;
import com.leetkob.exceptions.InvalidCredentialsException;
import com.leetkob.exceptions.OAuthAccountException;
import com.leetkob.exceptions.ResourceNotFoundException;
import com.leetkob.mappers.UserMapper;
import com.leetkob.repositories.ClassMembershipRepository;
import com.leetkob.repositories.UserRepository;
import com.leetkob.security.CustomUserDetails;
import com.leetkob.security.jwt.JwtUtil;
import com.leetkob.services.AuthService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepository;
  private final ClassMembershipRepository classMembershipRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtUtil jwtUtil;
  private final UserMapper userMapper;

  @Override
  @Transactional
  public AuthResult signup(SignupRequest request) {
    String normalizedEmail = request.getEmail().toLowerCase().trim();
    if (userRepository.existsByEmail(normalizedEmail)) {
      throw new EmailAlreadyExistsException("Email is already registered");
    }

    User user = userMapper.toUser(request);
    user.setEmail(normalizedEmail);
    user.setRole("student");
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

    User savedUser = userRepository.save(user);
    String token =
        jwtUtil.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

    return new AuthResult(toResponseWithClasses(savedUser, List.of()), token);
  }

  @Override
  @Transactional(readOnly = true)
  public AuthResult login(LoginRequest request) {
    Authentication authentication;
    try {
      authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  request.getEmail().toLowerCase().trim(), request.getPassword()));
    } catch (BadCredentialsException e) {
      throw new InvalidCredentialsException("Invalid email or password");
    }

    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    User user = userDetails.getUser();

    if (user.getPasswordHash() == null) {
      throw new OAuthAccountException(
          "Account was created via Google OAuth. Please sign in with Google.");
    }

    List<Integer> classIds = classMembershipRepository.findClassIdsByUserId(user.getId());
    String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
    return new AuthResult(toResponseWithClasses(user, classIds), token);
  }

  @Override
  @Transactional(readOnly = true)
  public AuthResult refreshSession(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    List<Integer> classIds = classMembershipRepository.findClassIdsByUserId(user.getId());
    String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
    return new AuthResult(toResponseWithClasses(user, classIds), token);
  }

  @Override
  @Transactional(readOnly = true)
  public UserResponse getUserById(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    List<Integer> classIds = classMembershipRepository.findClassIdsByUserId(userId);
    return toResponseWithClasses(user, classIds);
  }

  private UserResponse toResponseWithClasses(User user, List<Integer> classIds) {
    UserResponse response = userMapper.toUserResponse(user);
    response.setClassIds(classIds);
    return response;
  }
}
