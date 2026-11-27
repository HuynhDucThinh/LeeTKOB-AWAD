package com.leetkob.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.leetkob.dto.auth.LoginRequest;
import com.leetkob.dto.auth.SignupRequest;
import com.leetkob.dto.auth.UserResponse;
import com.leetkob.entity.User;
import com.leetkob.exception.InvalidCredentialsException;
import com.leetkob.mapper.UserMapper;
import com.leetkob.repository.ClassMembershipRepository;
import com.leetkob.repository.UserRepository;
import com.leetkob.security.jwt.JwtUtil;
import com.leetkob.service.AuthService;
import com.leetkob.service.impl.AuthServiceImpl;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class JwtAndAuthRoleUnitTest {

  private static final String TEST_SIGNING_MATERIAL = "t".repeat(40);

  @Test
  void jwtEmbedsAndExtractsRoleAndCustomUserDetailsMapsAuthority() {
    JwtUtil jwtUtil = new JwtUtil(TEST_SIGNING_MATERIAL, 3_600_000L);
    UUID userId = UUID.randomUUID();

    String taToken = jwtUtil.generateToken(userId, "ta1@leetkob.edu", "ta");
    assertTrue(jwtUtil.validateToken(taToken));
    assertEquals(userId, jwtUtil.getUserIdFromToken(taToken));
    assertEquals("ta1@leetkob.edu", jwtUtil.getEmailFromToken(taToken));
    assertEquals("ta", jwtUtil.getRoleFromToken(taToken));

    CustomUserDetails taDetails =
        new CustomUserDetails(
            User.builder().id(userId).email("ta1@leetkob.edu").role("ta").build());
    assertTrue(
        taDetails.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_TA".equals(authority.getAuthority())));

    CustomUserDetails adminDetails =
        new CustomUserDetails(
            User.builder().id(userId).email("admin@leetkob.edu").role("admin").build());
    assertTrue(
        adminDetails.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
  }

  @Test
  void signupAlwaysAssignsStudentRoleAndReturnsJwtWithStudentClaim() {
    UserRepository userRepository = mock(UserRepository.class);
    ClassMembershipRepository membershipRepository = mock(ClassMembershipRepository.class);
    AuthenticationManager authManager = mock(AuthenticationManager.class);
    UserMapper userMapper = mock(UserMapper.class);
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    JwtUtil jwtUtil = new JwtUtil(TEST_SIGNING_MATERIAL, 3_600_000L);

    UUID newId = UUID.randomUUID();
    SignupRequest req =
        SignupRequest.builder()
            .email("StudentNew@HCMUS.edu.vn")
            .password("secret123")
            .fullName("New Student")
            .build();

    when(userRepository.existsByEmail("studentnew@hcmus.edu.vn")).thenReturn(false);
    when(userMapper.toUser(req))
        .thenReturn(User.builder().email(req.getEmail()).fullName(req.getFullName()).build());
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            inv -> {
              User u = inv.getArgument(0);
              u.setId(newId);
              return u;
            });
    when(userMapper.toUserResponse(any(User.class)))
        .thenAnswer(
            inv -> {
              User u = inv.getArgument(0);
              return UserResponse.builder()
                  .id(u.getId())
                  .email(u.getEmail())
                  .fullName(u.getFullName())
                  .role(u.getRole())
                  .build();
            });

    AuthService service =
        new AuthServiceImpl(
            userRepository,
            membershipRepository,
            passwordEncoder,
            authManager,
            jwtUtil,
            userMapper);

    AuthService.AuthResult result = service.signup(req);
    assertEquals("student", result.userResponse().getRole());
    assertEquals(List.of(), result.userResponse().getClassIds());
    assertEquals("student", jwtUtil.getRoleFromToken(result.token()));
  }

  @Test
  void loginReturnsRoleClassIdsAndRejectsBadCredentials() {
    UserRepository userRepository = mock(UserRepository.class);
    ClassMembershipRepository membershipRepository = mock(ClassMembershipRepository.class);
    AuthenticationManager authManager = mock(AuthenticationManager.class);
    UserMapper userMapper = mock(UserMapper.class);
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    JwtUtil jwtUtil = new JwtUtil(TEST_SIGNING_MATERIAL, 3_600_000L);

    UUID taId = UUID.randomUUID();
    User taUser =
        User.builder()
            .id(taId)
            .email("ta1@leetkob.edu")
            .fullName("TA Class 10")
            .role("ta")
            .passwordHash(passwordEncoder.encode("Ta1@1234"))
            .build();
    CustomUserDetails details = new CustomUserDetails(taUser);

    when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(
            new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    when(membershipRepository.findClassIdsByUserId(taId)).thenReturn(List.of(10));
    when(userMapper.toUserResponse(taUser))
        .thenReturn(
            UserResponse.builder()
                .id(taId)
                .email("ta1@leetkob.edu")
                .fullName("TA Class 10")
                .role("ta")
                .build());

    AuthService service =
        new AuthServiceImpl(
            userRepository,
            membershipRepository,
            passwordEncoder,
            authManager,
            jwtUtil,
            userMapper);

    AuthService.AuthResult result =
        service.login(LoginRequest.builder().email("ta1@leetkob.edu").password("Ta1@1234").build());
    assertEquals("ta", result.userResponse().getRole());
    assertEquals(List.of(10), result.userResponse().getClassIds());
    assertEquals("ta", jwtUtil.getRoleFromToken(result.token()));

    when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("bad"));
    assertThrows(
        InvalidCredentialsException.class,
        () ->
            service.login(
                LoginRequest.builder().email("ta1@leetkob.edu").password("wrong").build()));
  }
}
