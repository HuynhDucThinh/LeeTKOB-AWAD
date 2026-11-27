package com.leetkob.security;

import com.leetkob.exception.problem.ProblemException;
import com.leetkob.exception.problem.ProblemKind;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Shared authentication and role-enforcement helper for /v1/** controllers (RFC 9457). */
@Component
public class V1AuthHelper {

  public CustomUserDetails requireAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
      throw new ProblemException(ProblemKind.AUTHENTICATION_REQUIRED);
    }
    return userDetails;
  }

  public CustomUserDetails requireRole(String... allowedRoles) {
    CustomUserDetails user = requireAuthenticated();
    Set<String> allowed = Set.of(allowedRoles);
    if (!allowed.contains(user.getRole())) {
      throw new ProblemException(ProblemKind.FORBIDDEN);
    }
    return user;
  }
}
