import me.paultristanwagner.satchecking.sat.solver.DPLLCDCLSolver;
import me.paultristanwagner.satchecking.smt.SMTResult;
import me.paultristanwagner.satchecking.smt.TheoryCNF;
import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.smt.solver.SMTSolver;
import me.paultristanwagner.satchecking.theory.Theory;
import me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorConstraint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for issue #14: bit-vector division/remainder by zero must
 * not force a spurious UNSAT. The bit width defaults to 8 bits in this solver,
 * and {@code /} and {@code %} are signed operators (bvsdiv / bvsrem). For the
 * non-negative dividends used here the divide-by-zero result coincides with the
 * unsigned {@code bvudiv} / {@code bvurem} values, i.e. all-ones / the dividend.
 */
public class BitVectorDivisionByZeroTest {

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static SMTResult solve(String formula) {
    Theory theory = Theory.get("QF_BV");
    var parser = theory.getCNFParser();
    SMTSolver solver = theory.getSMTSolver();
    solver.setSATSolver(new DPLLCDCLSolver());
    solver.setTheorySolver(theory.getTheorySolver());

    TheoryCNF<BitVectorConstraint> cnf =
        (TheoryCNF<BitVectorConstraint>) parser.parseWithRemaining(formula).result();
    solver.load(cnf);

    return solver.solve();
  }

  // The BV theory solver stores each variable's value as a string of the form
  // "0b<bits> (<decimal>)"; we recover the unsigned numeric value from the bits.
  private static long unsignedValueOf(SMTResult result, String variable) {
    VariableAssignment solution = result.getSolution();
    String rendered = String.valueOf(solution.getAssignment(variable));

    int start = rendered.indexOf("0b") + 2;
    int end = rendered.indexOf(' ', start);
    if (end < 0) {
      end = rendered.length();
    }
    String bits = rendered.substring(start, end);

    return Long.parseLong(bits, 2);
  }

  @Test
  public void divisionByZeroIsSatAndAllOnes() {
    SMTResult result = solve("(y = 0) & (x = 1) & (x / y = z)");

    assertTrue(result.isSatisfiable(), "x / 0 must be SAT, not UNSAT");
    // bvudiv(x, 0) = bvsdiv(x >= 0, 0) = all ones = 255 for 8-bit.
    assertEquals(255L, unsignedValueOf(result, "z"));
  }

  @Test
  public void remainderByZeroIsSatAndEqualsDividend() {
    SMTResult result = solve("(y = 0) & (x = 5) & (x % y = z)");

    assertTrue(result.isSatisfiable(), "x % 0 must be SAT, not UNSAT");
    // bvurem(x, 0) = bvsrem(x, 0) = x = 5.
    assertEquals(5L, unsignedValueOf(result, "z"));
  }

  @Test
  public void normalDivisionStillWorks() {
    SMTResult result = solve("(x = 6) & (y = 2) & (x / y = z)");

    assertTrue(result.isSatisfiable());
    assertEquals(3L, unsignedValueOf(result, "z"));
  }

  @Test
  public void normalRemainderStillWorks() {
    SMTResult result = solve("(x = 7) & (y = 2) & (x % y = z)");

    assertTrue(result.isSatisfiable());
    assertEquals(1L, unsignedValueOf(result, "z"));
  }

  @Test
  public void divisionByZeroResultIsPinned() {
    // The divide-by-zero result is fixed (255), so demanding a different value
    // must be UNSAT. This guards against "anything goes" under-constraining.
    SMTResult result = solve("(y = 0) & (x = 1) & (x / y = z) & (z = 5)");

    assertTrue(!result.isSatisfiable(), "x / 0 result must be pinned to all-ones");
  }
}
