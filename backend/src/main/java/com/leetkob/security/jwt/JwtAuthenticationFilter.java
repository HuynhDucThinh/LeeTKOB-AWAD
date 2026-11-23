package com.leetkob.security.jwt;

import com.leetkob.entities.User;
import com.leetkob.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final CookieUtil cookieUtil;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String token = resolveToken(request);

    if (token != null && jwtUtil.validateToken(token)) {
      UUID userId = jwtUtil.getUserIdFromToken(token);
      String email = jwtUtil.getEmailFromToken(token);

      if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        User user = User.builder().id(userId).email(email).build();

        CustomUserDetails userDetails = new CustomUserDetails(user);

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    filterChain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
    // 1. Try to get token from HttpOnly cookie
    String token = cookieUtil.extractTokenFromCookie(request);
    if (token != null && !token.isBlank()) {
      return token;
    }

    // 2. Fallback to Authorization Header (Bearer ...)
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }

    return null;
  }
}
