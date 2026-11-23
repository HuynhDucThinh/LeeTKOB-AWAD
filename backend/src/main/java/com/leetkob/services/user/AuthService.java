package com.leetkob.services.user;

import com.leetkob.dtos.auth.LoginRequest;
import com.leetkob.dtos.auth.SignupRequest;
import com.leetkob.dtos.auth.UserResponse;
import com.leetkob.entities.User;
import com.leetkob.exceptions.EmailAlreadyExistsException;
import com.leetkob.exceptions.InvalidCredentialsException;
import com.leetkob.exceptions.OAuthAccountException;
import com.leetkob.exceptions.ResourceNotFoundException;
import com.leetkob.mappers.UserMapper;
import com.leetkob.repositories.UserRepository;
import com.leetkob.security.CustomUserDetails;
import com.leetkob.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Transactional
    public AuthResult signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        User user = userMapper.toUser(request);
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);
        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getEmail());

        return new AuthResult(userMapper.toUserResponse(savedUser), token);
    }

    public AuthResult login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase().trim(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        if (user.getPasswordHash() == null) {
            throw new OAuthAccountException("Account was created via Google OAuth. Please sign in with Google.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return new AuthResult(userMapper.toUserResponse(user), token);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return userMapper.toUserResponse(user);
    }

    public record AuthResult(UserResponse userResponse, String token) {}
}
