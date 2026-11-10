package vn.edu.hcmus.leetkob.auth;

import java.util.Optional;

@FunctionalInterface
public interface AuthResolver {
  Optional<Principal> authenticate(String authorizationHeader);
}
