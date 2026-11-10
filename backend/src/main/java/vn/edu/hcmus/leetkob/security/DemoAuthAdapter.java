package vn.edu.hcmus.leetkob.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

/** Fail-closed standalone authentication adapter for local PA#2 demo execution. */
public final class DemoAuthAdapter {

  private static final int MIN_TOKEN_LENGTH = 32;
  private static final Principal DEMO_TA = new Principal("ta-demo", "ta");

  private DemoAuthAdapter() {}

  public static AuthResolver fromToken(String rawConfiguredToken) {
    if (rawConfiguredToken == null) {
      throw new IllegalArgumentException(
          "DEMO_AUTH_TOKEN is required to start the standalone demo authenticator.");
    }
    String expected = rawConfiguredToken.trim();
    if (expected.length() < MIN_TOKEN_LENGTH
        || expected.chars().anyMatch(Character::isWhitespace)) {
      throw new IllegalArgumentException(
          "DEMO_AUTH_TOKEN must be at least 32 non-whitespace characters.");
    }
    byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);

    return authorizationHeader -> {
      if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
        return Optional.empty();
      }
      String token = authorizationHeader.substring("Bearer ".length());
      if (token.isEmpty() || token.chars().anyMatch(Character::isWhitespace)) {
        return Optional.empty();
      }
      byte[] candidateBytes = token.getBytes(StandardCharsets.UTF_8);
      if (MessageDigest.isEqual(candidateBytes, expectedBytes)) {
        return Optional.of(DEMO_TA);
      }
      return Optional.empty();
    };
  }
}
