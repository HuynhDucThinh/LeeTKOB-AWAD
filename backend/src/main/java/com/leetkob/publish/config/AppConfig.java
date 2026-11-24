package com.leetkob.publish.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.leetkob.publish.security.AuthResolver;
import com.leetkob.publish.security.DemoAuthAdapter;
import java.time.Clock;
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
  @ConditionalOnMissingBean(ObjectMapper.class)
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
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
