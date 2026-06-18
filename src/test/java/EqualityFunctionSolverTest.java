import me.paultristanwagner.satchecking.theory.EqualityFunctionConstraint;
import me.paultristanwagner.satchecking.theory.EqualityFunctionConstraint.Function;
import me.paultristanwagner.satchecking.theory.TheoryResult;
import me.paultristanwagner.satchecking.theory.solver.EqualityFunctionSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EqualityFunctionSolverTest {

  private static Function v(String name) {
    return Function.of(name);
  }

  private static Function f(Function arg) {
    return Function.of("f", arg);
  }

  private static EqualityFunctionConstraint eq(Function l, Function r) {
    return new EqualityFunctionConstraint(l, r, true);
  }

  private static EqualityFunctionConstraint neq(Function l, Function r) {
    return new EqualityFunctionConstraint(l, r, false);
  }

  private static TheoryResult<EqualityFunctionConstraint> solve(
      EqualityFunctionConstraint... constraints) {
    EqualityFunctionSolver solver = new EqualityFunctionSolver();
    for (EqualityFunctionConstraint c : constraints) {
      solver.addConstraint(c);
    }
    return solver.solve();
  }

  /** Single-hop congruence: a=b implies f(a)=f(b). */
  @Test
  public void testDirectCongruenceUnsat() {
    Function a = v("a"), b = v("b");
    TheoryResult<EqualityFunctionConstraint> result = solve(eq(a, b), neq(f(a), f(b)));
    assertFalse(result.isSatisfiable(), "a=b & f(a)!=f(b) must be UNSAT");
  }

  /**
   * Regression for issue #10: congruence over a multi-hop equality class. The union-find tree built
   * from these unions has a's parent (b) different from its root (d), so the previous implementation
   * compared direct parent pointers instead of class roots, missed the f(a)=f(c) congruence, and
   * wrongly reported SAT.
   *
   * <p>a=b, c=d, b=d makes {a,b,c,d} one class, hence f(a)=f(c), contradicting f(a)!=f(c).
   */
  @Test
  public void testTransitiveCongruenceUnsat() {
    Function a = v("a"), b = v("b"), c = v("c"), d = v("d");
    TheoryResult<EqualityFunctionConstraint> result =
        solve(eq(a, b), eq(c, d), eq(b, d), neq(f(a), f(c)));
    assertFalse(
        result.isSatisfiable(), "a=b & c=d & b=d & f(a)!=f(c) must be UNSAT (transitive congruence)");
  }

  /** A genuinely satisfiable instance must still be reported SAT. */
  @Test
  public void testSat() {
    Function a = v("a"), b = v("b"), c = v("c");
    TheoryResult<EqualityFunctionConstraint> result =
        solve(eq(a, b), neq(f(a), f(c)));
    assertTrue(result.isSatisfiable(), "a=b & f(a)!=f(c) is satisfiable (a,c may differ)");
  }
}
