package com.leetkob.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReviewQueueSamplerUnitTest {

  @Test
  void violationsAndUnsureAlwaysEnterQueueWithoutSampledPassFlag() {
    ReviewQueueSampler sampler = new ReviewQueueSampler(42L);
    for (int i = 0; i < 100; i++) {
      ReviewQueueSampler.SamplingOutcome violation = sampler.evaluate("violation");
      assertTrue(violation.inQueue());
      assertFalse(violation.sampledPass());

      ReviewQueueSampler.SamplingOutcome unsure = sampler.evaluate("unsure");
      assertTrue(unsure.inQueue());
      assertFalse(unsure.sampledPass());
    }
  }

  @Test
  void passSamplingWithFixedSeedIsDeterministicAndApproximatelyTenPercent() {
    ReviewQueueSampler firstRun = new ReviewQueueSampler(20261125L);
    ReviewQueueSampler secondRun = new ReviewQueueSampler(20261125L);

    int sampledCount = 0;
    int total = 1000;
    for (int i = 0; i < total; i++) {
      ReviewQueueSampler.SamplingOutcome o1 = firstRun.evaluate("pass");
      ReviewQueueSampler.SamplingOutcome o2 = secondRun.evaluate("pass");
      assertEquals(o1, o2);
      assertEquals(o1.inQueue(), o1.sampledPass());
      if (o1.sampledPass()) {
        sampledCount++;
      }
    }

    assertTrue(
        sampledCount >= 70 && sampledCount <= 130,
        "Expected ~10% of 1000 pass verdicts to be sampled, got " + sampledCount);
  }
}
