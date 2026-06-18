import me.paultristanwagner.satchecking.command.impl.SmtLibCommand;
import me.paultristanwagner.satchecking.command.impl.SmtLibCommand.Verdict;
import me.paultristanwagner.satchecking.parse.SyntaxError;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SMT-LIB 2.6 subset parser and the {@code smtlib} command. Each case asserts the
 * verdict matches the script's declared {@code :status}, and that unsupported constructs throw a
 * clear {@link SyntaxError}.
 *
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class SmtLibParserTest {

  private static Verdict run(String script) {
    return SmtLibCommand.runScript(script, false);
  }

  @Test
  public void testQfLraSat() {
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_LRA)(declare-const x Real)(declare-const y Real)"
                + "(assert (<= x 5))(assert (= (+ x y) 3))(set-info :status sat)(check-sat)"));
  }

  @Test
  public void testQfLraUnsat() {
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_LRA)(declare-const x Real)"
                + "(assert (<= x 0))(assert (>= x 5))(set-info :status unsat)(check-sat)"));
  }

  @Test
  public void testQfLraDisjunctionSat() {
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_LRA)(declare-const x Real)"
                + "(assert (or (<= x 0) (>= x 5)))(assert (>= x 7))"
                + "(set-info :status sat)(check-sat)"));
  }

  @Test
  public void testQfLraDisjunctionUnsat() {
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_LRA)(declare-const x Real)"
                + "(assert (or (<= x 0) (>= x 5)))(assert (>= x 1))(assert (<= x 4))"
                + "(set-info :status unsat)(check-sat)"));
  }

  @Test
  public void testQfLraRationalAndProduct() {
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_LRA)(declare-const x Real)(declare-const y Real)"
                + "(assert (= (* 2 x) y))(assert (= x (/ 3 2)))(assert (= y 3))"
                + "(set-info :status sat)(check-sat)"));
  }

  @Test
  public void testQfLiaSat() {
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_LIA)(declare-const x Int)(declare-const y Int)"
                + "(assert (<= x 5))(assert (= (+ x y) 3))(set-info :status sat)(check-sat)"));
  }

  @Test
  public void testQfLiaUnsat() {
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_LIA)(declare-const x Int)"
                + "(assert (>= x 1))(assert (<= x 0))(set-info :status unsat)(check-sat)"));
  }

  @Test
  public void testQfEqUnsat() {
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_EQ)(declare-const a U)(declare-const b U)(declare-const c U)"
                + "(assert (= a b))(assert (= b c))(assert (distinct a c))"
                + "(set-info :status unsat)(check-sat)"));
  }

  @Test
  public void testQfEqSat() {
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_EQ)(declare-const a U)(declare-const b U)(declare-const c U)"
                + "(assert (= a b))(assert (distinct b c))(set-info :status sat)(check-sat)"));
  }

  @Test
  public void testQfUfCongruenceUnsat() {
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_UF)(declare-sort U 0)(declare-const a U)(declare-const b U)"
                + "(declare-fun f (U) U)(assert (= a b))(assert (distinct (f a) (f b)))"
                + "(set-info :status unsat)(check-sat)"));
  }

  @Test
  public void testQfUfSat() {
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_UF)(declare-sort U 0)(declare-const a U)(declare-const b U)"
                + "(declare-fun f (U) U)(assert (= (f a) (f b)))"
                + "(set-info :status sat)(check-sat)"));
  }

  @Test
  public void testStrictInequalityAccepted() {
    // Strict inequalities are now supported for the arithmetic logics.
    assertEquals(
        Verdict.SAT,
        run("(set-logic QF_LRA)(declare-const x Real)(assert (< x 5))(check-sat)"));
  }

  @Test
  public void testNegationAccepted() {
    // Boolean negation over atoms is supported in all logics.
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_EQ)(declare-const a U)(declare-const b U)"
                + "(assert (not (= a b)))(check-sat)"));
  }

  @Test
  public void testImpliesAccepted() {
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_EQ)(declare-const a U)(declare-const b U)(declare-const c U)"
                + "(assert (=> (= a b) (= b c)))(check-sat)"));
  }

  @Test
  public void testNonlinearRejected() {
    SyntaxError error =
        assertThrows(
            SyntaxError.class,
            () ->
                run(
                    "(set-logic QF_LRA)(declare-const x Real)(declare-const y Real)"
                        + "(assert (= (* x y) 1))(check-sat)"));
    assertTrue(error.getMessage().contains("nonlinear term"), error.getMessage());
  }
}
