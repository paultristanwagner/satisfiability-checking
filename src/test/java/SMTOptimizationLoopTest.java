import me.paultristanwagner.satchecking.parse.ParseResult;
import me.paultristanwagner.satchecking.parse.TheoryCNFParser;
import me.paultristanwagner.satchecking.sat.solver.DPLLCDCLSolver;
import me.paultristanwagner.satchecking.smt.SMTResult;
import me.paultristanwagner.satchecking.smt.TheoryCNF;
import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.smt.solver.SMTSolver;
import me.paultristanwagner.satchecking.theory.Theory;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;
import me.paultristanwagner.satchecking.theory.solver.TheorySolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for issue #20: the lazy SMT loop must compare optima across all
 * theory-consistent Boolean models and return the global optimum, not the optimum of whichever
 * Boolean model the SAT solver happened to enumerate first.
 */
public class SMTOptimizationLoopTest {

  @SuppressWarnings("unchecked")
  private static SMTResult<?> solve(String formula) {
    Theory theory = Theory.get("QF_LRA");
    TheoryCNFParser parser = theory.getCNFParser();
    SMTSolver solver = theory.getSMTSolver();
    solver.setSATSolver(new DPLLCDCLSolver());
    solver.setTheorySolver((TheorySolver) theory.getTheorySolver());

    ParseResult<TheoryCNF<?>> pr = parser.parseWithRemaining(formula);
    solver.load((TheoryCNF) pr.result());
    return solver.solve();
  }

  private static Number valueOf(SMTResult<?> result, String variable) {
    VariableAssignment<Number> solution = (VariableAssignment<Number>) result.getSolution();
    return solution.getAssignment(variable);
  }

  private static void assertNumberEquals(long expected, Number actual) {
    Number e = Number.number(expected);
    assertTrue(
        actual.greaterThanOrEqual(e) && actual.lessThanOrEqual(e),
        "expected " + expected + " but was " + actual);
  }

  @Test
  public void maximumIsOrderIndependent() {
    // The optimum (max x = 100) must not depend on the order of the disjuncts.
    SMTResult<?> a = solve("(x <= 100 | x <= 5) & (max(x))");
    SMTResult<?> b = solve("(x <= 5 | x <= 100) & (max(x))");

    assertTrue(a.isSatisfiable());
    assertTrue(b.isSatisfiable());

    assertNumberEquals(100, valueOf(a, "x"));
    assertNumberEquals(100, valueOf(b, "x"));
  }

  @Test
  public void minimumPicksGlobalOptimum() {
    // x <= 0 rules out the (x >= 3) branch; the feasible branch is x in [-10, 0], so min x = -10.
    SMTResult<?> result = solve("(x >= 3 | x >= -10) & (x <= 0) & (min(x))");

    assertTrue(result.isSatisfiable());
    assertNumberEquals(-10, valueOf(result, "x"));
  }

  @Test
  public void minimumWithCoupledVariable() {
    // y = 5 and x + y >= 12 force x >= 7; the (x <= -3) branch is then infeasible, so min x = 7.
    SMTResult<?> result = solve("(x <= -3 | x >= 3) & (y = 5) & (x + y >= 12) & (min(x))");

    assertTrue(result.isSatisfiable());
    assertNumberEquals(7, valueOf(result, "x"));
  }

  @Test
  public void nonObjectiveUnsatStillReported() {
    // No objective present: the common case must not regress.
    SMTResult<?> result = solve("(x <= 0 | x >= 5) & (x + y = 5/2) & (y = 1)");
    assertFalse(result.isSatisfiable());
    assertFalse(result.isUnknown());
  }
}
