import me.paultristanwagner.satchecking.parse.LinearConstraintParser;
import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.SimplexResult;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;
import me.paultristanwagner.satchecking.theory.solver.SimplexOptimizationSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SimplexOptimizationSolverTest {

  private final LinearConstraintParser parser = new LinearConstraintParser();

  @Test
  public void testInfeasible() {
    SimplexOptimizationSolver simplex = new SimplexOptimizationSolver();
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

    SimplexOptimizationSolver simplex = new SimplexOptimizationSolver();
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

  @Test
  public void testMaximal() {
    SimplexOptimizationSolver simplex = new SimplexOptimizationSolver();
    simplex.addConstraint(parser.parse("x+y>=1"));
    simplex.addConstraint(parser.parse("3x+0.5y<=4"));
    simplex.addConstraint(parser.parse("y+x<=3"));
    simplex.maximize(parser.parse("max(2x+y)"));

    SimplexResult result = simplex.solve();
    assertTrue(result.isFeasible());
    assertTrue(result.isOptimal());

    Number maximum = result.getOptimum();
    assertEquals(Number.parse("4"), maximum);
  }

  @Test
  public void testMinimal() {
    SimplexOptimizationSolver simplex = new SimplexOptimizationSolver();
    simplex.addConstraint(parser.parse("x>=-3"));
    simplex.addConstraint(parser.parse("-y-x<=6"));
    simplex.minimize(parser.parse("min(3x+y)"));

    SimplexResult result = simplex.solve();
    assertTrue(result.isFeasible());
    assertTrue(result.isOptimal());

    Number minimum = result.getOptimum();
    assertEquals(Number.parse("-12"), minimum);
  }

  @Test
  public void testUnbounded() {
    SimplexOptimizationSolver simplex = new SimplexOptimizationSolver();
    simplex.addConstraint(parser.parse("y-3x>=2"));
    simplex.addConstraint(parser.parse("4y-12x<=28"));
    simplex.addConstraint(parser.parse("y-0.5x>=-1"));
    simplex.maximize(parser.parse("max(2x+y)"));

    SimplexResult result = simplex.solve();
    assertTrue(result.isFeasible());
    assertFalse(result.isOptimal());
    assertTrue(result.isUnbounded());
  }

  /*
     Calculate the maximum flow on the given graph
         1                   2                      7
          ┌────────►  a  ─────────►  b  ───────────┐
          │                                        │
          │           ▲              ▲             │
          │           │              │             │
          │           │              │             │
          │           │              │             ▼
                      │              │
          s         1 │            5 │             t
                      │              │
          │           │              │             ▲
          │           │              │             │
          │           │              │             │
          │           │              │             │
          │                                        │
          └────────►  c ──────────►  d  ───────────┘
         8                   7                      2
  */
  @Test
  public void testFlow() {
    SimplexOptimizationSolver simplex = new SimplexOptimizationSolver();
    simplex.addConstraint(parser.parse("x_sa>=0"));
    simplex.addConstraint(parser.parse("x_sc>=0"));
    simplex.addConstraint(parser.parse("x_ab>=0"));
    simplex.addConstraint(parser.parse("x_ca>=0"));
    simplex.addConstraint(parser.parse("x_cd>=0"));
    simplex.addConstraint(parser.parse("x_bt>=0"));
    simplex.addConstraint(parser.parse("x_db>=0"));
    simplex.addConstraint(parser.parse("x_dt>=0"));

    simplex.addConstraint(parser.parse("x_sa<=1"));
    simplex.addConstraint(parser.parse("x_sc<=8"));
    simplex.addConstraint(parser.parse("x_ab<=2"));
    simplex.addConstraint(parser.parse("x_ca<=1"));
    simplex.addConstraint(parser.parse("x_cd<=7"));
    simplex.addConstraint(parser.parse("x_bt<=7"));
    simplex.addConstraint(parser.parse("x_db<=5"));
    simplex.addConstraint(parser.parse("x_dt<=2"));

    simplex.addConstraint(parser.parse("x_ab-x_sa-x_ca=0"));
    simplex.addConstraint(parser.parse("x_ca+x_cd-x_sc=0"));
    simplex.addConstraint(parser.parse("x_bt-x_ab-x_db=0"));
    simplex.addConstraint(parser.parse("x_db+x_dt-x_cd=0"));

    simplex.maximize(parser.parse("max(x_sa+x_sc)"));

    SimplexResult result = simplex.solve();
    assertTrue(result.isFeasible());
    assertFalse(result.isUnbounded());
    assertTrue(result.isOptimal());

    Number maximum = result.getOptimum();
    assertEquals(Number.parse("9"), maximum);
  }
}
