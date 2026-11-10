package vn.edu.hcmus.leetkob.domain;

import java.util.List;

/** Pure domain rules for LeeTKOB mark calculation and TA review queue routing. */
public final class MarkCalculator {

  public enum Verdict {
    PASS,
    VIOLATION,
    UNSURE
  }

  public record ConstraintDecision(
      String constraintId, Verdict verdict, double deduction, boolean overturnedByTa) {}

  private MarkCalculator() {}

  /**
   * Calculates the final mark on a 10.0 scale. Confirmed violations deduct marks, overturned
   * violations cost 0, and mark never drops below 0.0.
   */
  public static double calculateMark(double maxMark, List<ConstraintDecision> decisions) {
    if (maxMark < 0) {
      throw new IllegalArgumentException("maxMark must be non-negative");
    }
    double totalDeduction = 0.0;
    for (ConstraintDecision decision : decisions) {
      if (decision.verdict() == Verdict.VIOLATION && !decision.overturnedByTa()) {
        totalDeduction += Math.max(0.0, decision.deduction());
      }
    }
    return Math.max(0.0, maxMark - totalDeduction);
  }

  /** Every VIOLATION, every UNSURE, or a sampled PASS requires TA review before publishing. */
  public static boolean requiresTaReview(Verdict verdict, boolean sampledPass) {
    if (verdict == Verdict.VIOLATION || verdict == Verdict.UNSURE) {
      return true;
    }
    return sampledPass;
  }
}
