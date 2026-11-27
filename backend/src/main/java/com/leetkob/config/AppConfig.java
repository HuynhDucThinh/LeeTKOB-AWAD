package com.leetkob.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.leetkob.checker.ConstraintCheckPipeline;
import com.leetkob.checker.MockConstraintCheckPipeline;
import com.leetkob.domain.ReviewQueueSampler;
import com.leetkob.security.jwt.JwtUtil;
import com.leetkob.security.publish.AuthResolver;
import com.leetkob.security.publish.DemoAuthAdapter;
import com.leetkob.security.publish.Principal;
import java.time.Clock;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

  @Bean
  @ConditionalOnMissingBean(Clock.class)
  public Clock systemClock() {
    return Clock.systemUTC();
  }

  @Bean
  @ConditionalOnMissingBean(ReviewQueueSampler.class)
  public ReviewQueueSampler reviewQueueSampler() {
    return new ReviewQueueSampler();
  }

  @Bean
  @ConditionalOnMissingBean(ConstraintCheckPipeline.class)
  public ConstraintCheckPipeline fallbackConstraintCheckPipeline() {
    return new MockConstraintCheckPipeline();
  }

  @Bean
  @ConditionalOnMissingBean(ObjectMapper.class)
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }

  /**
   * Fail-closed authentication (AI-LOG A09). There is no fallback identity table in production
   * code: a missing or weak {@code DEMO_AUTH_TOKEN} stops startup. When {@link JwtUtil} is present
   * in the application context, valid signed JWT Bearer tokens are also resolved to their real user
   * email and role. Test identities live only in {@code src/test}, where contract tests register
   * their own {@code @Primary} resolver.
   */
  @Bean
  @ConditionalOnMissingBean(AuthResolver.class)
  public AuthResolver defaultAuthResolver(
      @Value("${DEMO_AUTH_TOKEN:}") String demoToken, ObjectProvider<JwtUtil> jwtUtilProvider) {
    AuthResolver demoResolver = DemoAuthAdapter.fromToken(demoToken);
    JwtUtil jwtUtil = jwtUtilProvider.getIfAvailable();
    if (jwtUtil == null) {
      return demoResolver;
    }
    return authorizationHeader -> {
      Optional<Principal> demoPrincipal = demoResolver.authenticate(authorizationHeader);
      if (demoPrincipal.isPresent()) {
        return demoPrincipal;
      }
      if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
        return Optional.empty();
      }
      String token = authorizationHeader.substring("Bearer ".length()).trim();
      if (token.isEmpty() || !jwtUtil.validateToken(token)) {
        return Optional.empty();
      }
      String email = jwtUtil.getEmailFromToken(token);
      String role = jwtUtil.getRoleFromToken(token);
      if (email == null || email.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(new Principal(email, role));
    };
  }
}
