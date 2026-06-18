import me.paultristanwagner.satchecking.command.impl.SmtLibCommand;
import me.paultristanwagner.satchecking.command.impl.SmtLibCommand.Verdict;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for general boolean structure (and/or/not/=>/ite/xor) over equality atoms in the equality
 * logics (QF_EQ, QF_UF/QF_EQUF). Covers negated-atom handling (issue #9) and the boolean-structure
 * increment of #33.
 *
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 */
public class SmtLibBooleanStructureTest {

  private static Verdict run(String script) {
    return SmtLibCommand.runScript(script, false);
  }

  @Test
  public void testQfUfCongruenceUnderNegation() {
    // a = b implies f(a) = f(b); asserting f(a) != f(b) is unsat by congruence.
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_UF)(declare-sort U 0)(declare-const a U)(declare-const b U)"
                + "(declare-fun f (U) U)(assert (= a b))(assert (not (= (f a) (f b))))(check-sat)"));
  }

  @Test
  public void testQfEqOrWithNegatedAtom() {
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_EQ)(declare-const a U)(declare-const b U)(declare-const c U)"
                + "(assert (or (= a b) (= b c)))(assert (not (= a c)))(check-sat)"));
  }

  @Test
  public void testQfEqPropositionalContradiction() {
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_EQ)(declare-const a U)(declare-const b U)"
                + "(assert (and (= a b) (not (= a b))))(check-sat)"));
  }

  @Test
  public void testQfUfImplication() {
    // (= a b) => (= b c), (= a b), (not (= b c)) is unsat.
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_UF)(declare-sort U 0)(declare-const a U)(declare-const b U)"
                + "(declare-const c U)(assert (=> (= a b) (= b c)))(assert (= a b))"
                + "(assert (not (= b c)))(check-sat)"));
  }

  @Test
  public void testQfUfDistinctUnsat() {
    // distinct over three terms is consistent only if all are in different classes; forcing two
    // equal with a chain makes it unsat.
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_UF)(declare-sort U 0)(declare-const a U)(declare-const b U)"
                + "(declare-const c U)(assert (distinct a b c))(assert (= a b))(check-sat)"));
  }

  @Test
  public void testQfUfIteSat() {
    // (ite (= a b) (= b c) (= a c)) with (= a b) true forces (= b c).
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_UF)(declare-sort U 0)(declare-const a U)(declare-const b U)"
                + "(declare-const c U)(assert (ite (= a b) (= b c) (= a c)))(check-sat)"));
  }

  @Test
  public void testArithmeticNowAcceptsStrictAndNegation() {
    // Strict inequalities and negation are now supported for the arithmetic logics (the strict
    // foundation made the bounds real). See SmtLibArithmeticBooleanTest for behavioural coverage.
    assertEquals(
        Verdict.SAT,
        run("(set-logic QF_LRA)(declare-const x Real)(assert (< x 5))(check-sat)"));
    assertEquals(
        Verdict.SAT,
        run("(set-logic QF_LRA)(declare-const x Real)(assert (not (<= x 5)))(check-sat)"));
  }
}
