import me.paultristanwagner.satchecking.command.impl.SmtLibCommand;
import me.paultristanwagner.satchecking.command.impl.SmtLibCommand.Verdict;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for full boolean structure (and/or/not/=>/ite/xor), {@code let} bindings, {@code =}-splitting
 * and strict inequalities over arithmetic atoms in the arithmetic logics (QF_LRA, QF_LIA).
 *
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 */
public class SmtLibArithmeticBooleanTest {

  private static Verdict run(String script) {
    return SmtLibCommand.runScript(script, false);
  }

  @Test
  public void testLraEqualitySplitContradictsStrictDisjunction() {
    // (= x 0) splits into x<=0 and x>=0, contradicting x<0 or x>0.
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_LRA)(declare-const x Real)"
                + "(assert (or (< x 0) (> x 0)))(assert (= x 0))(check-sat)"));
  }

  @Test
  public void testLraNegatedNonStrictBecomesStrict() {
    // (not (<= x 3)) is x>3; together with x<=5 this is satisfiable (3<x<=5).
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_LRA)(declare-const x Real)"
                + "(assert (and (<= x 5) (not (<= x 3))))(check-sat)"));
  }

  @Test
  public void testLraImplication() {
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_LRA)(declare-const x Real)"
                + "(assert (=> (<= x 0) (<= x (- 1))))(assert (<= x 0))(check-sat)"));
  }

  @Test
  public void testLraLetBindingOverTerm() {
    // let binds s = x + y; assert s = 1 (split) and x < 0.
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_LRA)(declare-const x Real)(declare-const y Real)"
                + "(assert (let ((s (+ x y))) (and (<= s 1) (>= s 1) (< x 0))))(check-sat)"));
  }

  @Test
  public void testLiaStrictRangeUnsat() {
    // No integer strictly between 0 and 1.
    assertEquals(
        Verdict.UNSAT,
        run("(set-logic QF_LIA)(declare-const x Int)(assert (and (> x 0) (< x 1)))(check-sat)"));
  }

  @Test
  public void testLiaDisjunctionWithNegatedEquality() {
    // x=2 or x=3, and not x=2  =>  x=3.
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_LIA)(declare-const x Int)"
                + "(assert (or (= x 2) (= x 3)))(assert (not (= x 2)))(check-sat)"));
  }

  @Test
  public void testEqualityLogicRegression() {
    // a = b implies f(a) = f(b); asserting f(a) != f(b) is unsat by congruence.
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_UF)(declare-sort U 0)(declare-const a U)(declare-const b U)"
                + "(declare-fun f (U) U)(assert (= a b))(assert (not (= (f a) (f b))))(check-sat)"));
  }
}
