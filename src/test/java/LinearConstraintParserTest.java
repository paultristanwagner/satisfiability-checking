import me.paultristanwagner.satchecking.parse.LinearConstraintParser;
import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;
import org.junit.jupiter.api.Test;

import static me.paultristanwagner.satchecking.theory.LinearConstraint.Bound.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LinearConstraintParserTest {

  private final LinearConstraintParser parser = new LinearConstraintParser();

  @Test
  public void testConstraints() {
    LinearConstraint lc0 = parser.parse("-31.17x+-+-101.0y=-+-27.156");
    assertEquals(Number.parse("-31.17"), lc0.getDifference().getCoefficients().get("x"));
    assertEquals(Number.parse("101"), lc0.getDifference().getCoefficients().get("y"));
    assertEquals(Number.parse("-27.156"), lc0.getDifference().getConstant());
    assertEquals(EQUAL, lc0.getBound());

    LinearConstraint lc1 = parser.parse("a-b>=-1");
    assertEquals(Number.parse("1"), lc1.getDifference().getCoefficients().get("a"));
    assertEquals(Number.parse("-1"), lc1.getDifference().getCoefficients().get("b"));
    assertEquals(GREATER_EQUALS, lc1.getBound());

    LinearConstraint lc2 = parser.parse("3x-2<=-2y+1");
    assertEquals(Number.parse("3"), lc2.getDifference().getCoefficients().get("x"));
    assertEquals(Number.parse("2"), lc2.getDifference().getCoefficients().get("y"));
    assertEquals(Number.parse("-3"), lc2.getDifference().getConstant());
    assertEquals(LESS_EQUALS, lc2.getBound());
  }
}
