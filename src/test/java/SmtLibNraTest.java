import me.paultristanwagner.satchecking.command.impl.SmtLibCommand;
import me.paultristanwagner.satchecking.command.impl.SmtLibCommand.Verdict;
import me.paultristanwagner.satchecking.theory.Constraint;
import me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomial;
import me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomialConstraint;
import me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomialConstraint.Comparison;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static me.paultristanwagner.satchecking.theory.arithmetic.Number.number;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the QF_NRA (non-linear real arithmetic) front-end of the SMT-LIB parser, wiring real
 * polynomial constraints through to the experimental CAD solver, plus the {@link
 * MultivariatePolynomialConstraint} negation / value-equality contract that makes the atom map
 * reliable.
 *
 * <p>The CAD solver is experimental and may not terminate on harder inputs; every test here uses
 * only small instances and a generous per-test timeout.
 *
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 */
public class SmtLibNraTest {

  private static Verdict run(String script) {
    return SmtLibCommand.runScript(script, false);
  }

  // ---- hand cases through the full pipeline --------------------------------

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  public void testCircleAndCubicSat() {
    // x^2 + y^2 = 1 and x^2 + y^3 = 1/2 (README example).
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_NRA)(declare-const x Real)(declare-const y Real)"
                + "(assert (= (+ (* x x) (* y y)) 1))"
                + "(assert (= (+ (* x x) (* y y y)) (/ 1 2)))(check-sat)"));
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  public void testThreeProductsUnsat() {
    // xy>0, yz>0, xz>0 force x,y,z same sign, contradicting x+y+z=0.
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_NRA)(declare-const x Real)(declare-const y Real)(declare-const z Real)"
                + "(assert (> (* x y) 0))(assert (> (* y z) 0))(assert (> (* x z) 0))"
                + "(assert (= (+ x y z) 0))(check-sat)"));
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  public void testXSquaredEqualsTwoSat() {
    assertEquals(
        Verdict.SAT,
        run("(set-logic QF_NRA)(declare-const x Real)(assert (= (* x x) 2))(check-sat)"));
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  public void testXSquaredTwoAndThreeUnsat() {
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_NRA)(declare-const x Real)"
                + "(assert (and (= (* x x) 2) (= (* x x) 3)))(check-sat)"));
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  public void testBooleanStructureWithNegationUnsat() {
    // x^2 < 0 is impossible and x > 5 contradicts x <= 5, so the disjunction with x<=5 is unsat.
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_NRA)(declare-const x Real)"
                + "(assert (or (< (* x x) 0) (> x 5)))(assert (<= x 5))(check-sat)"));
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  public void testPowerOperatorSat() {
    // (^ x 2) should behave like x*x.
    assertEquals(
        Verdict.SAT,
        run("(set-logic QF_NRA)(declare-const x Real)(assert (= (^ x 2) 4))(check-sat)"));
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  public void testNotEqualsNegationSat() {
    // negated equality: (not (= (* x x) 0)) is x^2 != 0, satisfiable.
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_NRA)(declare-const x Real)"
                + "(assert (not (= (* x x) 0)))(check-sat)"));
  }

  // ---- constraint negation / value-equality contract -----------------------

  @Test
  public void testNegateFlipsComparisonOnSamePolynomial() {
    MultivariatePolynomial p = MultivariatePolynomial.variable("x");
    MultivariatePolynomialConstraint lt =
        MultivariatePolynomialConstraint.multivariatePolynomialConstraint(p, Comparison.LESS_THAN);

    assertTrue(lt.isNegatable());
    Constraint negated = lt.negate();
    assertEquals(
        Comparison.GREATER_THAN_OR_EQUALS,
        ((MultivariatePolynomialConstraint) negated).getComparison());
    // The polynomial is unchanged by negation.
    assertEquals(p, ((MultivariatePolynomialConstraint) negated).getPolynomial());
  }

  @Test
  public void testDoubleNegateRoundTrips() {
    MultivariatePolynomial p =
        MultivariatePolynomial.variable("x")
            .multiply(MultivariatePolynomial.variable("x"))
            .subtract(MultivariatePolynomial.constant(number(2)));
    for (Comparison c : Comparison.values()) {
      MultivariatePolynomialConstraint constraint =
          MultivariatePolynomialConstraint.multivariatePolynomialConstraint(p, c);
      assertEquals(constraint, constraint.negate().negate(), "round-trip for " + c);
    }
  }

  @Test
  public void testValueEqualityIsVariableOrderIndependent() {
    MultivariatePolynomial x = MultivariatePolynomial.variable("x");
    MultivariatePolynomial y = MultivariatePolynomial.variable("y");
    // x^2 + y^2 - 1 built in two different variable orders.
    MultivariatePolynomial p1 =
        x.multiply(x).add(y.multiply(y)).subtract(MultivariatePolynomial.constant(number(1)));
    MultivariatePolynomial p2 =
        y.multiply(y).add(x.multiply(x)).subtract(MultivariatePolynomial.constant(number(1)));

    MultivariatePolynomialConstraint c1 =
        MultivariatePolynomialConstraint.multivariatePolynomialConstraint(p1, Comparison.EQUALS);
    MultivariatePolynomialConstraint c2 =
        MultivariatePolynomialConstraint.multivariatePolynomialConstraint(p2, Comparison.EQUALS);

    assertEquals(c1, c2);
    assertEquals(c1.hashCode(), c2.hashCode());

    // Different comparison must not be equal.
    MultivariatePolynomialConstraint c3 =
        MultivariatePolynomialConstraint.multivariatePolynomialConstraint(p1, Comparison.NOT_EQUALS);
    assertNotEquals(c1, c3);
  }

  @Test
  public void testDedupInHashSet() {
    MultivariatePolynomial x = MultivariatePolynomial.variable("x");
    MultivariatePolynomialConstraint a =
        MultivariatePolynomialConstraint.multivariatePolynomialConstraint(
            x.multiply(x), Comparison.GREATER_THAN);
    MultivariatePolynomialConstraint b =
        MultivariatePolynomialConstraint.multivariatePolynomialConstraint(
            x.multiply(x), Comparison.GREATER_THAN);

    Set<Constraint> set = new HashSet<>();
    set.add(a);
    set.add(b);
    assertEquals(1, set.size());
  }
}
