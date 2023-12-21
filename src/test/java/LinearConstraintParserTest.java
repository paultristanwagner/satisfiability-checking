import me.paultristanwagner.satchecking.parse.LinearConstraintParser;
import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LinearConstraintParserTest {

  private final LinearConstraintParser parser = new LinearConstraintParser();

  @Test
  public void testConstraints() {
    LinearConstraint lc0 = parser.parse("-31.17x+-+-101.0y=-+-27.156");
    assertEquals(Number.parse("-31.17"), lc0.getCoefficients().get("x"));
    assertEquals(Number.parse("101"), lc0.getCoefficients().get("y"));
    assertEquals(Number.parse("27.156"), lc0.getValue());
    assertEquals(LinearConstraint.Bound.EQUAL, lc0.getBound());

    LinearConstraint lc1 = parser.parse("a-b>=-1");
    assertEquals(Number.parse("1"), lc1.getCoefficients().get("a"));
    assertEquals(Number.parse("-1"), lc1.getCoefficients().get("b"));
    assertEquals(LinearConstraint.Bound.LOWER, lc1.getBound());
  }
}
