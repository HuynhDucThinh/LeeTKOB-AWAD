package com.leetkob.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.leetkob.config.AppConfig;
import com.leetkob.entities.User;
import com.leetkob.exceptions.problem.ProblemException;
import com.leetkob.exceptions.problem.ProblemKind;
import com.leetkob.security.jwt.JwtUtil;
import com.leetkob.security.publish.AuthResolver;
import com.leetkob.security.publish.Principal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class V1FoundationUnitTest {

  private static final String VALID_DEMO_TOKEN = "d".repeat(40);
  private static final String VALID_JWT_SECRET = "j".repeat(40);

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void jwtUtilFailsClosedWhenSecretMissingOrTooShort() {
    assertThrows(IllegalArgumentException.class, () -> new JwtUtil("", 3_600_000L));
    assertThrows(IllegalArgumentException.class, () -> new JwtUtil("too-short", 3_600_000L));
  }

  @Test
  void compositeAuthResolverAcceptsRealJwtAndDemoTokenWhileRejectingInvalid() {
    JwtUtil jwtUtil = new JwtUtil(VALID_JWT_SECRET, 3_600_000L);
    new ApplicationContextRunner()
        .withUserConfiguration(AppConfig.class)
        .withBean(JwtUtil.class, () -> jwtUtil)
        .withPropertyValues("DEMO_AUTH_TOKEN=" + VALID_DEMO_TOKEN)
        .run(
            ctx -> {
              AuthResolver resolver = ctx.getBean(AuthResolver.class);

              String jwt = jwtUtil.generateToken(UUID.randomUUID(), "ta1@leetkob.edu", "ta");
              Optional<Principal> fromJwt = resolver.authenticate("Bearer " + jwt);
              assertTrue(fromJwt.isPresent());
              assertEquals(new Principal("ta1@leetkob.edu", "ta"), fromJwt.get());

              Optional<Principal> fromDemo = resolver.authenticate("Bearer " + VALID_DEMO_TOKEN);
              assertTrue(fromDemo.isPresent());
              assertEquals(new Principal("ta-demo", "ta"), fromDemo.get());

              assertTrue(resolver.authenticate("Bearer invalid-token").isEmpty());
              assertTrue(resolver.authenticate("Bearer own").isEmpty());
              assertTrue(resolver.authenticate(null).isEmpty());
            });
  }

  @Test
  void v1AuthHelperEnforcesAuthenticationAndRolesWithRfc9457Kinds() {
    V1AuthHelper helper = new V1AuthHelper();

    ProblemException unauthenticated =
        assertThrows(ProblemException.class, helper::requireAuthenticated);
    assertEquals(ProblemKind.AUTHENTICATION_REQUIRED, unauthenticated.kind());

    User student =
        User.builder().id(UUID.randomUUID()).email("student1@leetkob.edu").role("student").build();
    CustomUserDetails details = new CustomUserDetails(student);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));

    assertEquals(student.getId(), helper.requireRole("student", "ta").getId());

    ProblemException forbidden =
        assertThrows(ProblemException.class, () -> helper.requireRole("ta"));
    assertEquals(ProblemKind.FORBIDDEN, forbidden.kind());
  }

  @Test
  void defaultAuthResolverWorksWithoutJwtBean() {
    AppConfig config = new AppConfig();
    ObjectProvider<JwtUtil> emptyProvider =
        new ObjectProvider<>() {
          @Override
          public JwtUtil getObject(Object... args) {
            return null;
          }

          @Override
          public JwtUtil getIfAvailable() {
            return null;
          }

          @Override
          public JwtUtil getIfUnique() {
            return null;
          }

          @Override
          public JwtUtil getObject() {
            return null;
          }
        };
    AuthResolver resolver = config.defaultAuthResolver(VALID_DEMO_TOKEN, emptyProvider);
    assertTrue(resolver.authenticate("Bearer " + VALID_DEMO_TOKEN).isPresent());
    assertTrue(resolver.authenticate("Bearer own").isEmpty());
  }
}
