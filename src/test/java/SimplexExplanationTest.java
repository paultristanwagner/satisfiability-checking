import me.paultristanwagner.satchecking.parse.LinearConstraintParser;
import me.paultristanwagner.satchecking.theory.SimplexResult;
import me.paultristanwagner.satchecking.theory.solver.SimplexFeasibilitySolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class SimplexExplanationTest {

  private static SimplexResult solve(String... constraints) {
    SimplexFeasibilitySolver solver = new SimplexFeasibilitySolver();
    LinearConstraintParser parser = new LinearConstraintParser();
    for (String c : constraints) {
      solver.addConstraint(parser.parse(c));
    }
    return solver.solve();
  }

  /**
   * Hardening for #24: infeasibility-explanation generation previously parsed slack indices out of
   * variable names (`split("s")`), which is unsafe for variables containing or lacking 's'. These
   * infeasible instances must report infeasible without throwing, regardless of variable names.
   */
  @Test
  public void infeasibleWithVariableNamedS() {
    assertFalse(solve("s<=-1", "s>=1").isFeasible());
  }

  @Test
  public void infeasibleWithVariableContainingS() {
    assertFalse(solve("cost<=-10", "cost>=10").isFeasible());
  }

  @Test
  public void infeasibleWithPlainVariables() {
    assertFalse(solve("x+y<=-10", "x+y>=10").isFeasible());
  }
}
