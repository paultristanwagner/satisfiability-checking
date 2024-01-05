import me.paultristanwagner.satchecking.parse.LinearConstraintParser;
import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.SimplexResult;
import me.paultristanwagner.satchecking.theory.solver.SimplexFeasibilitySolver;

import me.paultristanwagner.satchecking.theory.arithmetic.Number;
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
    LinearConstraint c1 = parser.parse("x+y>=1");
    LinearConstraint c2 = parser.parse("0.5x+2y<=4");
    LinearConstraint c3 = parser.parse("2x+0.5y<=4");

    SimplexFeasibilitySolver simplex = new SimplexFeasibilitySolver();

    simplex.addConstraint(c1);
    simplex.addConstraint(c2);
    simplex.addConstraint(c3);

    SimplexResult result = simplex.solve();
    assertTrue(result.isFeasible());

    VariableAssignment<Number> solution = (VariableAssignment<Number>) result.getSolution();

    assertTrue(c1.evaluate(solution));
    assertTrue(c2.evaluate(solution));
    assertTrue(c3.evaluate(solution));
  }
}
