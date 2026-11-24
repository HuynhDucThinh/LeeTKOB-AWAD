package com.leetkob.security.oauth2;

import com.leetkob.entities.User;
import com.leetkob.security.CustomUserDetails;
import com.leetkob.security.jwt.CookieUtil;
import com.leetkob.security.jwt.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final JwtUtil jwtUtil;
  private final CookieUtil cookieUtil;

  @Value("${app.oauth2.redirect-uri:http://localhost:5173/}")
  private String redirectUri;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {

    if (response.isCommitted()) {
      log.debug("Response has already been committed. Unable to redirect to " + redirectUri);
      return;
    }

    if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
      log.error("Principal is not CustomUserDetails: {}", authentication.getPrincipal().getClass());
      throw new IllegalStateException(
          "Unexpected principal type: " + authentication.getPrincipal().getClass());
    }

    User user = userDetails.getUser();

    // 1. Generate JWT token
    String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());

    // 2. Set HttpOnly JWT cookie
    ResponseCookie cookie = cookieUtil.createAccessTokenCookie(token);
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    // 3. Redirect to Frontend
    getRedirectStrategy().sendRedirect(request, response, redirectUri);
  }
}
