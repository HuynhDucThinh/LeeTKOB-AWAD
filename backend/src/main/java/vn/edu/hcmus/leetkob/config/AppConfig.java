package vn.edu.hcmus.leetkob.config;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.edu.hcmus.leetkob.security.AuthResolver;
import vn.edu.hcmus.leetkob.security.DemoAuthAdapter;

@Configuration
public class AppConfig {

  @Bean
  @ConditionalOnMissingBean(Clock.class)
  public Clock systemClock() {
    return Clock.systemUTC();
  }

  /**
   * Fail-closed authentication (AI-LOG A09). There is no fallback identity table in production
   * code: a missing or weak {@code DEMO_AUTH_TOKEN} stops startup. Test identities live only in
   * {@code src/test}, where the contract tests register their own {@code @Primary} resolver.
   */
  @Bean
  @ConditionalOnMissingBean(AuthResolver.class)
  public AuthResolver defaultAuthResolver(@Value("${DEMO_AUTH_TOKEN:}") String demoToken) {
    return DemoAuthAdapter.fromToken(demoToken);
  }
}
