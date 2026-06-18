import me.paultristanwagner.satchecking.command.impl.SmtLibCommand;
import me.paultristanwagner.satchecking.command.impl.SmtLibCommand.Verdict;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for issue #43: theory conflict lemmas produced by {@link
 * me.paultristanwagner.satchecking.smt.solver.FullLazySMTSolver} are now learned WITH watched
 * literals (via {@code DPLLCDCLSolver.learnTheoryLemma}) so they participate in conflict-driven
 * propagation and prune the SAT search, instead of being appended inertly via {@code
 * CNF.learnClause}.
 *
 * <p>These cases all run through the full-lazy logics (QF_UF/QF_EQUF, QF_LRA, QF_LIA, QF_NRA) and
 * assert that verdicts are unchanged. The {@code eq_diamond} family is exactly the class of QF_UF
 * instances whose pruning depends on the lemmas being active in BCP: with inert lemmas these need
 * exponentially many model-blocking iterations; with watched lemmas they terminate quickly. The
 * encoded instance below is small enough to solve either way, but verifies the lemma path stays
 * sound on a structurally diamond-shaped problem.
 */
public class FullLazyWatchedLemmaTest {

  private static Verdict run(String script) {
    return SmtLibCommand.runScript(script, false);
  }

  @Test
  public void testQfUfCongruenceUnsat() {
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_UF)(declare-sort U 0)(declare-const a U)(declare-const b U)"
                + "(declare-fun f (U) U)(assert (= a b))(assert (not (= (f a) (f b))))(check-sat)"));
  }

  @Test
  public void testQfLraDisjunctionConflictUnsat() {
    // (x<0 | x>0) & x=0 : every complete Boolean model conflicts in the theory, exercising the
    // lemma-learning path; result must be unsat.
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_LRA)(declare-const x Real)(assert (or (< x 0) (> x 0)))"
                + "(assert (= x 0))(check-sat)"));
  }

  @Test
  public void testQfLraSat() {
    assertEquals(
        Verdict.SAT,
        run(
            "(set-logic QF_LRA)(declare-const x Real)(assert (or (<= x 5) (>= x 5)))(check-sat)"));
  }

  @Test
  public void testQfLiaEmptyIntervalUnsat() {
    assertEquals(
        Verdict.UNSAT,
        run("(set-logic QF_LIA)(declare-const x Int)(assert (and (> x 0) (< x 1)))(check-sat)"));
  }

  @Test
  public void testQfNraSat() {
    assertEquals(
        Verdict.SAT,
        run("(set-logic QF_NRA)(declare-const x Real)(assert (= (* x x) 2))(check-sat)"));
  }

  @Test
  public void testQfNraConflictUnsat() {
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_NRA)(declare-const x Real)"
                + "(assert (and (= (* x x) 2) (= (* x x) 3)))(check-sat)"));
  }

  @Test
  public void testEqDiamondShapedUnsat() {
    // A small "diamond": (a=b | a=c) & (b=d | c=d) & a=d ... combined with a forced inequality so
    // that the conflict lemmas must prune the alternative paths. This is the QF_UF eq_diamond
    // structure in miniature; the verdict must be unsat.
    assertEquals(
        Verdict.UNSAT,
        run(
            "(set-logic QF_UF)(declare-sort U 0)"
                + "(declare-const a U)(declare-const b U)(declare-const c U)(declare-const d U)"
                + "(assert (or (= a b) (= a c)))"
                + "(assert (or (= b d) (= c d)))"
                + "(assert (= a b))(assert (= b d))"
                + "(assert (not (= a d)))(check-sat)"));
  }
}
