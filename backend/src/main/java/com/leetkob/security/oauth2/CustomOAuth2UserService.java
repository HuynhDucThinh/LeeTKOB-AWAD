package com.leetkob.security.oauth2;

import com.leetkob.entities.User;
import com.leetkob.enums.OauthProvider;
import com.leetkob.security.CustomUserDetails;
import com.leetkob.services.OAuthAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private final OAuthAccountService oAuthAccountService;

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(userRequest);

    String registrationId = userRequest.getClientRegistration().getRegistrationId();
    OauthProvider provider = OauthProvider.valueOf(registrationId.toUpperCase());

    String email = oAuth2User.getAttribute("email");
    String name = oAuth2User.getAttribute("name");
    String pictureUrl = oAuth2User.getAttribute("picture");
    String providerUserId = oAuth2User.getAttribute("sub");

    if (providerUserId == null) {
      providerUserId = oAuth2User.getName();
    }

    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("OAuth provider did not return an email address");
    }

    User user =
        oAuthAccountService.findOrCreateOAuthUser(
            email, name, pictureUrl, provider, providerUserId);

    return new CustomUserDetails(user, oAuth2User.getAttributes());
  }
}
