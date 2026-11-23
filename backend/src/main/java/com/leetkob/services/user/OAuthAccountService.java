package com.leetkob.services.user;

import com.leetkob.entities.OauthAccount;
import com.leetkob.entities.User;
import com.leetkob.enums.OauthProvider;
import com.leetkob.repositories.OauthAccountRepository;
import com.leetkob.repositories.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthAccountService {

  private final UserRepository userRepository;
  private final OauthAccountRepository oauthAccountRepository;

  @Transactional
  public User findOrCreateOAuthUser(
      String email, String name, String pictureUrl, OauthProvider provider, String providerUserId) {
    Optional<OauthAccount> oauthOpt =
        oauthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId);
    if (oauthOpt.isPresent()) {
      return oauthOpt.get().getUser();
    }

    Optional<User> userOpt = userRepository.findByEmail(email);
    User user;

    if (userOpt.isPresent()) {
      user = userOpt.get();
    } else {
      user =
          User.builder()
              .email(email)
              .fullName(name != null ? name : "Anonymous")
              .avatarUrl(pictureUrl)
              .passwordHash(null)
              .build();
      user = userRepository.save(user);
    }

    OauthAccount oauthAccount =
        OauthAccount.builder().user(user).provider(provider).providerUserId(providerUserId).build();
    oauthAccountRepository.save(oauthAccount);

    return user;
  }
}
