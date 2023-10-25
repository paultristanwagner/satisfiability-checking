import me.paultristanwagner.satchecking.parse.Parser;
import me.paultristanwagner.satchecking.parse.PropositionalLogicParser;
import me.paultristanwagner.satchecking.parse.PropositionalLogicParser.PropositionalLogicExpression;
import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.sat.solver.DPLLCDCLSolver;
import me.paultristanwagner.satchecking.sat.solver.SATSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TseitinTransformationTest {

  @Test
  public void testImplication() {
    String formula = "a -> b -> c";

    Parser<PropositionalLogicExpression> parser = new PropositionalLogicParser();
    PropositionalLogicExpression expression = parser.parse(formula);
    CNF cnf = PropositionalLogicParser.tseitin(expression);

    SATSolver solver = new DPLLCDCLSolver();
    solver.load(cnf);

    int models = 0;
    while(solver.nextModel() != null) {
      models++;
    }

    assertEquals(7, models);
  }
}
