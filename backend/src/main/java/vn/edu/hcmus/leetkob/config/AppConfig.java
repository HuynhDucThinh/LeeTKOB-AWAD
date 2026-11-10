package vn.edu.hcmus.leetkob.config;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.edu.hcmus.leetkob.security.AuthResolver;
import vn.edu.hcmus.leetkob.security.DemoAuthAdapter;
import vn.edu.hcmus.leetkob.security.Principal;

@Configuration
public class AppConfig {

  private static final Map<String, Principal> TEST_IDENTITIES =
      Map.of(
          "own", new Principal("ta-own", "ta"),
          "colleague", new Principal("ta-colleague", "ta"),
          "foreign", new Principal("ta-foreign", "ta"),
          "student", new Principal("student-1", "student"),
          "admin", new Principal("admin-1", "admin"));

  @Bean
  @ConditionalOnMissingBean(Clock.class)
  public Clock systemClock() {
    return Clock.systemUTC();
  }

  @Bean
  @ConditionalOnMissingBean(AuthResolver.class)
  public AuthResolver defaultAuthResolver(
      @Value("${leetkob.auth.mode:test}") String authMode,
      @Value("${DEMO_AUTH_TOKEN:}") String demoToken) {
    if ("demo".equalsIgnoreCase(authMode)) {
      return DemoAuthAdapter.fromToken(demoToken);
    }
    return authorizationHeader -> {
      if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
        return Optional.empty();
      }
      String token = authorizationHeader.substring("Bearer ".length());
      return Optional.ofNullable(TEST_IDENTITIES.get(token));
    };
  }
}
