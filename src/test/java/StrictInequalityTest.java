import me.paultristanwagner.satchecking.parse.LinearConstraintParser;
import me.paultristanwagner.satchecking.parse.ParseResult;
import me.paultristanwagner.satchecking.parse.TheoryCNFParser;
import me.paultristanwagner.satchecking.sat.solver.DPLLCDCLSolver;
import me.paultristanwagner.satchecking.smt.SMTResult;
import me.paultristanwagner.satchecking.smt.TheoryCNF;
import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.smt.solver.SMTSolver;
import me.paultristanwagner.satchecking.theory.SimplexResult;
import me.paultristanwagner.satchecking.theory.Theory;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;
import me.paultristanwagner.satchecking.theory.solver.SimplexFeasibilitySolver;
import me.paultristanwagner.satchecking.theory.solver.TheorySolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for strict inequality support ({@code <}, {@code >}) in linear real / integer arithmetic,
 * implemented via the delta-rational method (Dutertre / de Moura).
 */
public class StrictInequalityTest {

  private final LinearConstraintParser parser = new LinearConstraintParser();

  @SuppressWarnings("unchecked")
  private static SMTResult<?> solve(String theoryName, String formula) {
    Theory theory = Theory.get(theoryName);
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

  // --- Direct SimplexFeasibilitySolver strict case --------------------------------------------

  @Test
  public void directStrictInfeasible() {
    SimplexFeasibilitySolver simplex = new SimplexFeasibilitySolver();
    simplex.addConstraint(parser.parse("x<5"));
    simplex.addConstraint(parser.parse("x>5"));

    SimplexResult result = simplex.solve();
    assertFalse(result.isFeasible());
  }

  @Test
  public void directStrictFeasibleWitnessSatisfiesStrictBounds() {
    SimplexFeasibilitySolver simplex = new SimplexFeasibilitySolver();
    simplex.addConstraint(parser.parse("x<5"));
    simplex.addConstraint(parser.parse("x>3"));

    SimplexResult result = simplex.solve();
    assertTrue(result.isFeasible());

    VariableAssignment<Number> solution = (VariableAssignment<Number>) result.getSolution();
    Number x = solution.getAssignment("x");
    assertTrue(x.greaterThan(Number.number(3)), "x must be strictly > 3, was " + x);
    assertTrue(x.lessThan(Number.number(5)), "x must be strictly < 5, was " + x);
  }

  @Test
  public void clearMakesSolverReusable() {
    SimplexFeasibilitySolver simplex = new SimplexFeasibilitySolver();
    simplex.addConstraint(parser.parse("x>5"));
    simplex.addConstraint(parser.parse("x<5"));
    assertFalse(simplex.solve().isFeasible());

    simplex.clear();
    simplex.addConstraint(parser.parse("x>3"));
    simplex.addConstraint(parser.parse("x<5"));
    assertTrue(simplex.solve().isFeasible());
  }

  // --- QF_LRA strict via the SMT pipeline -----------------------------------------------------

  @Test
  public void strictConflictUnsat() {
    assertFalse(solve("QF_LRA", "(x<5)&(x>5)").isSatisfiable());
  }

  @Test
  public void strictRangeSat() {
    assertTrue(solve("QF_LRA", "(x<5)&(x>3)").isSatisfiable());
  }

  @Test
  public void strictAgainstClosedBoundaryUnsat() {
    assertFalse(solve("QF_LRA", "(x<5)&(x>=5)").isSatisfiable());
  }

  @Test
  public void closedBoundaryEqualSat() {
    assertTrue(solve("QF_LRA", "(x<=5)&(x>=5)").isSatisfiable());
  }

  @Test
  public void scaledStrictRangeSat() {
    assertTrue(solve("QF_LRA", "(2x<3)&(2x>2)").isSatisfiable());
  }

  // --- QF_LRA optimization still works (strict relaxed to closure in objective path) ----------

  @Test
  public void minimizationWithDisjunctionAndCoupling() {
    SMTResult<?> result = solve("QF_LRA", "(x<=-3|x>=3)&(y=5)&(x+y>=12)&(min(x))");
    assertTrue(result.isSatisfiable());
    assertNumberEquals(7, valueOf(result, "x"));
  }

  @Test
  public void maximizationOverDisjunction() {
    SMTResult<?> result = solve("QF_LRA", "(x<=5|x<=100)&(max(x))");
    assertTrue(result.isSatisfiable());
    assertNumberEquals(100, valueOf(result, "x"));
  }

  // --- QF_LIA strict --------------------------------------------------------------------------

  @Test
  public void integerStrictRangeUnsat() {
    assertFalse(solve("QF_LIA", "(x<1)&(x>0)").isSatisfiable());
  }

  @Test
  public void integerStrictRangeSat() {
    SMTResult<?> result = solve("QF_LIA", "(x>0)&(x<5)&(2x=4)");
    assertTrue(result.isSatisfiable());
    assertNumberEquals(2, valueOf(result, "x"));
  }

  @Test
  public void integerClosedPointSat() {
    SMTResult<?> result = solve("QF_LIA", "(x>=3)&(x<=3)");
    assertTrue(result.isSatisfiable());
    assertNumberEquals(3, valueOf(result, "x"));
  }
}
