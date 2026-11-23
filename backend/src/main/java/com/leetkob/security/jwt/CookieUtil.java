package com.leetkob.security.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

  public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";

  @Value("${jwt.cookie.secure:false}")
  private boolean secure;

  @Value("${jwt.expiration-ms:86400000}")
  private long expirationMs;

  public ResponseCookie createAccessTokenCookie(String token) {
    return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, token)
        .httpOnly(true)
        .secure(secure)
        .path("/")
        .maxAge(Duration.ofMillis(expirationMs))
        .sameSite("Lax")
        .build();
  }

  public ResponseCookie createCleanAccessTokenCookie() {
    return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, "")
        .httpOnly(true)
        .secure(secure)
        .path("/")
        .maxAge(0)
        .sameSite("Lax")
        .build();
  }

  public String extractTokenFromCookie(HttpServletRequest request) {
    if (request.getCookies() == null) {
      return null;
    }
    for (Cookie cookie : request.getCookies()) {
      if (ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
