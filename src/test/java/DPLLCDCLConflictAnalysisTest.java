import me.paultristanwagner.satchecking.sat.Assignment;
import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.sat.Result;
import me.paultristanwagner.satchecking.sat.solver.DPLLCDCLSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for issue #36: NullPointerException in CDCL conflict analysis.
 *
 * <p>Conflict analysis could select a <em>decision</em> literal (antecedent == null) as the literal
 * to resolve on, causing {@code resolution} to dereference a null antecedent. The reproducer below
 * triggered that crash; after the fix it must return SAT with a model satisfying the CNF.
 */
public class DPLLCDCLConflictAnalysisTest {

  @Test
  public void testConflictAnalysisDoesNotCrashAndReturnsSat() {
    CNF cnf = CNF.parse("(e) & (a | d) & (c | c) & (d | a) & (e)");

    Result result = DPLLCDCLSolver.check(cnf);

    assertTrue(result.isSatisfiable(), "Reproducer for issue #36 must be satisfiable");

    Assignment assignment = result.getAssignment();
    assertTrue(assignment.evaluate(cnf), "Returned model must satisfy the CNF");
  }
}
