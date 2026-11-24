package com.leetkob.security;

import com.leetkob.entities.User;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Getter
public class CustomUserDetails implements UserDetails, OidcUser {

  private final User user;
  private Map<String, Object> attributes;
  private OidcIdToken idToken;
  private OidcUserInfo userInfo;

  public CustomUserDetails(User user) {
    this.user = user;
  }

  public CustomUserDetails(User user, Map<String, Object> attributes) {
    this.user = user;
    this.attributes = attributes;
    if (attributes != null) {
      this.userInfo = new OidcUserInfo(attributes);
    }
  }

  public CustomUserDetails(User user, Map<String, Object> attributes, OidcIdToken idToken) {
    this.user = user;
    this.attributes = attributes;
    this.idToken = idToken;
    if (attributes != null) {
      this.userInfo = new OidcUserInfo(attributes);
    }
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

  // OidcUser methods
  @Override
  public Map<String, Object> getClaims() {
    return attributes != null ? attributes : Collections.emptyMap();
  }

  @Override
  public OidcUserInfo getUserInfo() {
    return userInfo;
  }

  @Override
  public OidcIdToken getIdToken() {
    return idToken;
  }

  // Google / OAuth2User methods
  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public String getName() {
    return user.getEmail();
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
