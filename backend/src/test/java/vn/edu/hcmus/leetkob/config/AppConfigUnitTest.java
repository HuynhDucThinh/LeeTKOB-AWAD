package vn.edu.hcmus.leetkob.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import vn.edu.hcmus.leetkob.security.AuthResolver;

/**
 * Guards the production wiring of {@link AppConfig}: authentication must fail closed (AI-LOG A09).
 * A missing or weak {@code DEMO_AUTH_TOKEN} must stop the application from starting, and the labels
 * used by the contract tests ({@code own}, {@code admin}, ...) must never authenticate outside
 * {@code src/test}.
 */
class AppConfigUnitTest {

  private static final List<String> TEST_LABELS =
      List.of("own", "colleague", "foreign", "student", "admin");

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(AppConfig.class);

  @Test
  void missingDemoTokenFailsStartupInsteadOfFallingBackToTestIdentities() {
    runner.withPropertyValues("DEMO_AUTH_TOKEN=").run(ctx -> assertThat(ctx).hasFailed());
  }

  @Test
  void weakDemoTokenFailsStartup() {
    runner
        .withPropertyValues("DEMO_AUTH_TOKEN=short-token")
        .run(ctx -> assertThat(ctx).hasFailed());
  }

  @Test
  void hardcodedTestLabelsAreNeverAcceptedByMainConfiguration() {
    runner
        .withPropertyValues("DEMO_AUTH_TOKEN=" + "x".repeat(40))
        .run(
            ctx -> {
              assertThat(ctx).hasNotFailed();
              AuthResolver resolver = ctx.getBean(AuthResolver.class);
              for (String label : TEST_LABELS) {
                assertThat(resolver.authenticate("Bearer " + label)).isEmpty();
              }
            });
  }
}
