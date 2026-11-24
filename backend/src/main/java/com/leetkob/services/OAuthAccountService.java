package com.leetkob.services;

import com.leetkob.entities.User;
import com.leetkob.enums.OauthProvider;

public interface OAuthAccountService {

  User findOrCreateOAuthUser(
      String email, String name, String pictureUrl, OauthProvider provider, String providerUserId);
}
