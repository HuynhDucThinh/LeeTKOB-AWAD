package vn.edu.hcmus.leetkob.security;

import java.util.Optional;

@FunctionalInterface
public interface AuthResolver {
  Optional<Principal> authenticate(String authorizationHeader);
}
