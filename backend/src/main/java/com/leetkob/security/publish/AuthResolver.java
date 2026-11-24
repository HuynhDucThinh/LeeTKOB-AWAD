package com.leetkob.security.publish;

import java.util.Optional;

@FunctionalInterface
public interface AuthResolver {
  Optional<Principal> authenticate(String authorizationHeader);
}
