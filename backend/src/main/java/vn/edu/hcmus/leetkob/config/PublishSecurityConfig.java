package vn.edu.hcmus.leetkob.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Delegates authentication and authorization for the PA#2 publish boundary to {@code
 * PublishController} and {@code AuthResolver}, which enforce fail-closed Bearer auth and RFC 9457
 * problem responses.
 */
@Configuration
@ConditionalOnWebApplication
public class PublishSecurityConfig {

  @Bean
  @ConditionalOnMissingBean(SecurityFilterChain.class)
  public SecurityFilterChain publishSecurityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .build();
  }
}
