package com.leetkob.repositories;

import com.leetkob.entities.OauthAccount;
import com.leetkob.entities.User;
import com.leetkob.enums.OauthProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OauthAccountRepository extends JpaRepository<OauthAccount, UUID> {

  @EntityGraph(attributePaths = {"user"})
  Optional<OauthAccount> findByProviderAndProviderUserId(
      OauthProvider provider, String providerUserId);

  @EntityGraph(attributePaths = {"user"})
  Optional<OauthAccount> findByUserAndProvider(User user, OauthProvider provider);
}
