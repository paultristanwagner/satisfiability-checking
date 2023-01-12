import me.paultristanwagner.satchecking.parse.LinearConstraintParser;
import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.SimplexResult;
import me.paultristanwagner.satchecking.theory.solver.SimplexFeasibilitySolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimplexFeasibilitySolverTest {

  private final LinearConstraintParser parser = new LinearConstraintParser();

  @Test
  public void testInfeasible() {
    SimplexFeasibilitySolver simplex = new SimplexFeasibilitySolver();
    simplex.addConstraint(parser.parse("2x+3y-0.5z<=-10"));
    simplex.addConstraint(parser.parse("2x+3y-0.5z>=10"));

    SimplexResult result = simplex.solve();
    assertFalse(result.isFeasible());
  }

  @Test
  public void testFeasible() {
    SimplexFeasibilitySolver simplex = new SimplexFeasibilitySolver();
    simplex.addConstraint(parser.parse("x+y>=1"));
    simplex.addConstraint(parser.parse("0.5x+2y<=4"));
    simplex.addConstraint(parser.parse("2x+0.5y<=4"));

    SimplexResult result = simplex.solve();
    assertTrue(result.isFeasible());

    VariableAssignment solution = result.getSolution();
    double x = solution.getAssignment("x");
    double y = solution.getAssignment("y");

    assertTrue(x + y >= 1);
    assertTrue(0.5 * x + 2 * y <= 4);
    assertTrue(2 * x + 0.5 * y <= 4);
  }
}
