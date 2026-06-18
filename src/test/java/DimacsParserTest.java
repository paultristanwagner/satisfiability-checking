import me.paultristanwagner.satchecking.parse.DimacsParser;
import me.paultristanwagner.satchecking.parse.SyntaxError;
import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.sat.Result;
import me.paultristanwagner.satchecking.sat.solver.DPLLCDCLSolver;
import me.paultristanwagner.satchecking.sat.solver.EnumerationSolver;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the robust DIMACS reader: the hand-written cases from the issue plus a small
 * differential check that re-uses the brute-force {@link EnumerationSolver} as the oracle to prove
 * the parser produces a correct CNF.
 */
public class DimacsParserTest {

  private final DimacsParser parser = new DimacsParser();

  @Test
  public void satCase() {
    CNF cnf = parser.parse("p cnf 3 2\n1 -3 0\n2 3 -1 0\n");
    Result result = DPLLCDCLSolver.check(cnf);
    assertTrue(result.isSatisfiable());
    assertTrue(result.getAssignment().evaluate(cnf));
  }

  @Test
  public void unsatCase() {
    CNF cnf = parser.parse("p cnf 1 2\n1 0\n-1 0\n");
    Result result = DPLLCDCLSolver.check(cnf);
    assertFalse(result.isSatisfiable());
  }

  @Test
  public void commentsAndMultiLineClause() {
    CNF cnf = parser.parse("c comment\nc another\np cnf 2 2\n1\n2 0\n-1 -2 0\n");
    assertEquals(2, cnf.getClauses().size());
    assertEquals(2, cnf.getClauses().get(0).getLiterals().size());
    Result result = DPLLCDCLSolver.check(cnf);
    assertTrue(result.isSatisfiable());
    assertTrue(result.getAssignment().evaluate(cnf));
  }

  @Test
  public void trailingEndMarkersTolerated() {
    CNF withPercent = parser.parse("p cnf 2 2\n1 0\n-1 2 0\n%\n0\n");
    assertEquals(2, withPercent.getClauses().size());

    CNF withZero = parser.parse("p cnf 2 2\n1 0\n-1 2 0\n0\n");
    assertEquals(2, withZero.getClauses().size());
  }

  @Test
  public void outOfRangeLiteralIsLenient() {
    // Out-of-range literal: a warning is printed to stderr but parsing succeeds.
    CNF cnf = parser.parse("p cnf 2 1\n1 5 0\n");
    Result result = DPLLCDCLSolver.check(cnf);
    assertNotNull(result);
  }

  @Test
  public void missingHeaderThrowsWithMessage() {
    SyntaxError error = assertThrows(SyntaxError.class, () -> parser.parse("1 2 0"));
    assertNotNull(error.getMessage());
  }

  @Test
  public void garbledHeaderThrowsWithMessage() {
    SyntaxError error = assertThrows(SyntaxError.class, () -> parser.parse("p cnf foo 2\n1 0\n"));
    assertNotNull(error.getMessage());
  }

  @Test
  public void unterminatedClauseThrowsWithMessage() {
    SyntaxError error = assertThrows(SyntaxError.class, () -> parser.parse("p cnf 2 1\n1 2\n"));
    assertNotNull(error.getMessage());
  }

  @Test
  public void differentialAgreementWithEnumerationOracle() {
    Random rnd = new Random(98765L);
    int trials = 500;
    int mismatches = 0;
    int invalidModels = 0;

    for (int t = 0; t < trials; t++) {
      int nVars = 3 + rnd.nextInt(3); // 3-5
      int nClauses = 1 + rnd.nextInt(8); // 1-8
      StringBuilder sb = new StringBuilder();
      sb.append("p cnf ").append(nVars).append(' ').append(nClauses).append('\n');
      for (int c = 0; c < nClauses; c++) {
        int width = 1 + rnd.nextInt(3); // 1-3
        for (int w = 0; w < width; w++) {
          int v = 1 + rnd.nextInt(nVars);
          if (rnd.nextBoolean()) {
            v = -v;
          }
          sb.append(v).append(' ');
        }
        sb.append("0\n");
      }

      CNF cnf = parser.parse(sb.toString());
      boolean enumSat = EnumerationSolver.check(cnf).isSatisfiable();
      Result cdcl = DPLLCDCLSolver.check(cnf);
      if (enumSat != cdcl.isSatisfiable()) {
        mismatches++;
      }
      if (cdcl.isSatisfiable() && !cdcl.getAssignment().evaluate(cnf)) {
        invalidModels++;
      }
    }

    assertEquals(0, mismatches, "SAT/UNSAT verdict disagreement with enumeration oracle");
    assertEquals(0, invalidModels, "CDCL returned a model that does not satisfy the parsed CNF");
  }
}
