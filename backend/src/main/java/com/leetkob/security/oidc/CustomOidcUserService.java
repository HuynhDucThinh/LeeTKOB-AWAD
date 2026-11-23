package com.leetkob.security.oidc;

import com.leetkob.entities.User;
import com.leetkob.enums.OauthProvider;
import com.leetkob.security.CustomUserDetails;
import com.leetkob.services.user.OAuthAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

  private final OAuthAccountService oAuthAccountService;

  @Override
  @Transactional
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    OidcUser oidcUser = super.loadUser(userRequest);

    String registrationId = userRequest.getClientRegistration().getRegistrationId();
    OauthProvider provider = OauthProvider.valueOf(registrationId.toUpperCase());

    String email = oidcUser.getAttribute("email");
    String name = oidcUser.getAttribute("name");
    String pictureUrl = oidcUser.getAttribute("picture");
    String providerUserId = oidcUser.getAttribute("sub");

    if (providerUserId == null) {
      providerUserId = oidcUser.getName();
    }

    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("OAuth provider did not return an email address");
    }

    User user =
        oAuthAccountService.findOrCreateOAuthUser(
            email, name, pictureUrl, provider, providerUserId);

    return new CustomUserDetails(user, oidcUser.getAttributes(), oidcUser.getIdToken());
  }
}
