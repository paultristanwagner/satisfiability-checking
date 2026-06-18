import me.paultristanwagner.satchecking.command.impl.SmtLibCommand;
import me.paultristanwagner.satchecking.command.impl.SmtLibCommand.Verdict;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the SMT-LIB front-end extensions: Bool-sorted variables as propositional atoms,
 * {@code define-fun} macros (Bool and term-valued, with and without parameters, nested), and
 * {@code push}/{@code pop} scoping with multiple {@code (check-sat)} commands (the last verdict
 * being returned).
 *
 * @author Paul Tristan Wagner &lt;paultristanwagner@gmail.com&gt;
 */
public class SmtLibDefineFunBoolTest {

  private static Verdict run(String script) {
    return SmtLibCommand.runScript(script, false);
  }

  // ---- Bool-sorted variables as propositional atoms -------------------------

  @Test
  public void testBoolVarMixedWithEqualityUnsat() {
    // p free; (p | a=b) & ~p & ~(a=b) is unsat.
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_UF)(declare-sort U 0)(declare-const a U)(declare-const b U)"
                + "(declare-const p Bool)(assert (or p (= a b)))(assert (not p))"
                + "(assert (not (= a b)))(check-sat)"));
  }

  @Test
  public void testBoolVarMixedWithEqualitySat() {
    // Dropping the last conjunct makes it satisfiable (a = b, p false).
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_UF)(declare-sort U 0)(declare-const a U)(declare-const b U)"
                + "(declare-const p Bool)(assert (or p (= a b)))(assert (not p))(check-sat)"));
  }

  @Test
  public void testBoolVarViaDeclareFun() {
    // (declare-fun p () Bool) is also a propositional variable.
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_UF)(declare-fun p () Bool)(declare-fun q () Bool)"
                + "(assert (or p q))(check-sat)"));
  }

  @Test
  public void testBoolEqualityIsBiconditional() {
    // '=' over Bool operands is a biconditional: (= p (= q q)) forces p true, contradicting (not p).
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_UF)(declare-const p Bool)(declare-const q Bool)"
                + "(assert (= p (= q q)))(assert (not p))(check-sat)"));
  }

  @Test
  public void testBoolPredicateApplicationSharesAtom() {
    // (f x) used twice is the same propositional atom; (f x) & ~(f x) is unsat.
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_UF)(declare-sort U 0)(declare-const x U)(declare-fun f (U) Bool)"
                + "(assert (f x))(assert (not (f x)))(check-sat)"));
  }

  // ---- define-fun macros ----------------------------------------------------

  @Test
  public void testDefineFunBoolUnsat() {
    // pos := x>0; assert pos and x<0 is unsat.
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_LRA)(declare-const x Real)(define-fun pos () Bool (> x 0))"
                + "(assert pos)(assert (< x 0))(check-sat)"));
  }

  @Test
  public void testDefineFunTermWithParamsSat() {
    // s(u,v) := u+v; (s x y)=1 and x>1 imply y<0; satisfiable.
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_LRA)(declare-const x Real)(declare-const y Real)"
                + "(define-fun s ((u Real)(v Real)) Real (+ u v))"
                + "(assert (= (s x y) 1))(assert (> x 1))(check-sat)"));
  }

  @Test
  public void testDefineFunNestedAndInteractionWithBoolVar() {
    // a := x>0; b := a & x<5; assert b and x>10 is unsat (b requires x<5).
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_LRA)(declare-const x Real)(define-fun a () Bool (> x 0))"
                + "(define-fun b () Bool (and a (< x 5)))(assert b)(assert (> x 10))(check-sat)"));
  }

  @Test
  public void testDefineFunInteractsWithLet() {
    // s(u,v) := u+v; let-bound t = (s x y); assert t=1 and x>1 -> sat.
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_LRA)(declare-const x Real)(declare-const y Real)"
                + "(define-fun s ((u Real)(v Real)) Real (+ u v))"
                + "(assert (let ((t (s x y))) (= t 1)))(assert (> x 1))(check-sat)"));
  }

  // ---- push / pop scoping ---------------------------------------------------

  @Test
  public void testPushPopReturnsLastCheckSat() {
    // First check (x>0 & x<0) is unsat; after pop only x>0 remains, so second check is sat.
    // runScript returns the verdict of the LAST (check-sat).
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_LRA)(declare-const x Real)(assert (> x 0))(push 1)(assert (< x 0))"
                + "(check-sat)(pop 1)(check-sat)"));
  }

  @Test
  public void testPopDiscardsDeclarationsAndDefs() {
    // q and r are declared inside the pushed frame; after pop they are gone, and the remaining
    // assertion (p) over the still-declared p is satisfiable.
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_UF)(declare-const p Bool)(push 1)(declare-const q Bool)"
                + "(define-fun r () Bool (and p q))(assert r)(pop 1)(assert p)(check-sat)"));
  }

  // ---- regressions ----------------------------------------------------------

  @Test
  public void testRegressionArithmeticBooleanStructure() {
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_LRA)(declare-const x Real)"
                + "(assert (or (< x 0) (> x 0)))(assert (= x 0))(check-sat)"));
  }

  @Test
  public void testRegressionUfCongruence() {
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_UF)(declare-sort U 0)(declare-const a U)(declare-const b U)"
                + "(declare-fun f (U) U)(assert (= a b))(assert (not (= (f a) (f b))))(check-sat)"));
  }
}
