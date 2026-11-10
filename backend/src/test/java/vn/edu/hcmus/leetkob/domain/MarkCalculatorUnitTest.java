package vn.edu.hcmus.leetkob.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import vn.edu.hcmus.leetkob.domain.MarkCalculator.ConstraintDecision;
import vn.edu.hcmus.leetkob.domain.MarkCalculator.Verdict;

class MarkCalculatorUnitTest {

  @Test
  void compliantSubmissionKeepsFullMark() {
    List<ConstraintDecision> decisions =
        List.of(
            new ConstraintDecision("C1", Verdict.PASS, 3.0, false),
            new ConstraintDecision("C2", Verdict.PASS, 2.0, false));
    assertEquals(10.0, MarkCalculator.calculateMark(10.0, decisions));
  }

  @Test
  void confirmedViolationDeductsConfiguredPoints() {
    List<ConstraintDecision> decisions =
        List.of(
            new ConstraintDecision("C1", Verdict.VIOLATION, 3.0, false),
            new ConstraintDecision("C2", Verdict.PASS, 2.0, false));
    assertEquals(7.0, MarkCalculator.calculateMark(10.0, decisions));
  }

  @Test
  void overturnedViolationCostsNoMarks() {
    List<ConstraintDecision> decisions =
        List.of(new ConstraintDecision("C1", Verdict.VIOLATION, 4.0, true));
    assertEquals(10.0, MarkCalculator.calculateMark(10.0, decisions));
  }

  @Test
  void markNeverDropsBelowZero() {
    List<ConstraintDecision> decisions =
        List.of(
            new ConstraintDecision("C1", Verdict.VIOLATION, 6.0, false),
            new ConstraintDecision("C2", Verdict.VIOLATION, 6.0, false));
    assertEquals(0.0, MarkCalculator.calculateMark(10.0, decisions));
  }

  @Test
  void violationsAndUnsureVerdictsAlwaysRequireTaReview() {
    assertTrue(MarkCalculator.requiresTaReview(Verdict.VIOLATION, false));
    assertTrue(MarkCalculator.requiresTaReview(Verdict.UNSURE, false));
    assertTrue(MarkCalculator.requiresTaReview(Verdict.PASS, true));
    assertFalse(MarkCalculator.requiresTaReview(Verdict.PASS, false));
  }
}
