package com.leetkob.domain;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Human-in-the-loop review queue sampling policy (H1): every {@code violation} and {@code unsure}
 * verdict always enters the TA review queue; {@code pass} verdicts are sampled at 10% so TAs can
 * spot-check false negatives. Supports fixed-seed {@link Random} for deterministic tests.
 */
public final class ReviewQueueSampler {

  public static final double PASS_SAMPLE_RATE = 0.10;

  private final Random random;

  public ReviewQueueSampler() {
    this(null);
  }

  public ReviewQueueSampler(long seed) {
    this(new Random(seed));
  }

  private ReviewQueueSampler(Random random) {
    this.random = random;
  }

  public SamplingOutcome evaluate(String verdict) {
    if ("violation".equals(verdict) || "unsure".equals(verdict)) {
      return new SamplingOutcome(true, false);
    }
    if ("pass".equals(verdict)) {
      double sample =
          random != null ? random.nextDouble() : ThreadLocalRandom.current().nextDouble();
      boolean sampled = sample < PASS_SAMPLE_RATE;
      return new SamplingOutcome(sampled, sampled);
    }
    return new SamplingOutcome(true, false);
  }

  public record SamplingOutcome(boolean inQueue, boolean sampledPass) {}
}
