package vn.edu.hcmus.leetkob.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class DemoAuthAdapterUnitTest {

  @Test
  void demoAuthenticationFailsClosedAndAcceptsOnlyConfiguredLongToken() {
    assertThrows(IllegalArgumentException.class, () -> DemoAuthAdapter.fromToken(null));
    assertThrows(IllegalArgumentException.class, () -> DemoAuthAdapter.fromToken(""));
    assertThrows(IllegalArgumentException.class, () -> DemoAuthAdapter.fromToken("short-token"));
    assertThrows(
        IllegalArgumentException.class,
        () -> DemoAuthAdapter.fromToken("token with space 12345678901234567890"));

    String secret = "demo-secret-token-with-at-least-32-chars";
    AuthResolver resolver = DemoAuthAdapter.fromToken(secret);

    assertTrue(resolver.authenticate(null).isEmpty());
    assertTrue(resolver.authenticate("Basic " + secret).isEmpty());
    assertTrue(resolver.authenticate("Bearer own").isEmpty());
    assertTrue(resolver.authenticate("Bearer wrong-secret-token-with-at-least-32-chars").isEmpty());

    Optional<Principal> authenticated = resolver.authenticate("Bearer " + secret);
    assertTrue(authenticated.isPresent());
    assertEquals(new Principal("ta-demo", "ta"), authenticated.get());
  }
}
