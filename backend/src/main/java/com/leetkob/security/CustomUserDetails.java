package com.leetkob.security;

import com.leetkob.entity.User;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class CustomUserDetails implements UserDetails {

  private final User user;

  public CustomUserDetails(User user) {
    this.user = user;
  }

  public UUID getId() {
    return user.getId();
  }

  public String getEmail() {
    return user.getEmail();
  }

  public String getRole() {
    return user.getRole();
  }

  @Override
  public String getUsername() {
    return user.getEmail();
  }

  @Override
  public String getPassword() {
    return user.getPasswordHash();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    String normalizedRole =
        (user.getRole() == null || user.getRole().isBlank())
            ? "STUDENT"
            : user.getRole().trim().toUpperCase();
    return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + normalizedRole));
  }
}
