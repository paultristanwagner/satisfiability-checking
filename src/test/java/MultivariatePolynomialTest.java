import me.paultristanwagner.satchecking.parse.Parser;
import me.paultristanwagner.satchecking.parse.PolynomialParser;
import me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomial;
import org.junit.jupiter.api.Test;

import java.util.List;

import static me.paultristanwagner.satchecking.theory.arithmetic.Number.number;
import static me.paultristanwagner.satchecking.theory.nonlinear.Exponent.exponent;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MultivariatePolynomialTest {

  private Parser<MultivariatePolynomial> parser = new PolynomialParser();

  @Test
  public void testSimple() {
    MultivariatePolynomial p = parser.parse("x^2 + 2*x + 1");

    assertEquals(number(1), p.getCoefficient(exponent(2)));
    assertEquals(number(2), p.getCoefficient(exponent(1)));
    assertEquals(number(1), p.getCoefficient(exponent(0)));

    assertEquals(2, p.degree("x"));
  }

  @Test
  public void testAdd() {
    MultivariatePolynomial p = parser.parse("x^2*y - 2*x*y^2*z + 1");
    MultivariatePolynomial q = parser.parse("x^2 - 10*y^3*z + 17");

    MultivariatePolynomial result = p.add(q);

    assertEquals(number(18), result.getCoefficient(exponent(0, 0, 0)));
    assertEquals(number(1), result.getCoefficient(exponent(2, 0, 0)));
    assertEquals(number(1), result.getCoefficient(exponent(2, 1, 0)));
    assertEquals(number(-2), result.getCoefficient(exponent(1, 2, 1)));
    assertEquals(number(-10), result.getCoefficient(exponent(0, 3, 1)));

    assertEquals(2, result.degree("x"));
    assertEquals(3, result.degree("y"));
    assertEquals(1, result.degree("z"));
  }

  @Test
  public void testMultiply() {
    MultivariatePolynomial p = parser.parse("x^2*y - 2*x*y^2*z + 1");
    MultivariatePolynomial q = parser.parse("x^2 - 10*y^3*z + 17");

    MultivariatePolynomial result = p.multiply(q);

    assertEquals(number(17), result.getCoefficient(exponent(0, 0, 0)));
    assertEquals(number(-10), result.getCoefficient(exponent(0, 3, 1)));
    assertEquals(number(-2), result.getCoefficient(exponent(3, 2, 1)));

    assertEquals(4, result.degree("x"));
    assertEquals(5, result.degree("y"));
    assertEquals(2, result.degree("z"));
  }

  @Test
  public void testPseudoDivision() {
    MultivariatePolynomial p = parser.parse("x^2 + y^2");
    MultivariatePolynomial q = parser.parse("x + y");

    List<MultivariatePolynomial> result = p.pseudoDivision(q, "x");

    MultivariatePolynomial quotient = result.get(0);
    MultivariatePolynomial remainder = result.get(1);

    assertEquals(number(1), quotient.getCoefficient(exponent(1, 0)));
    assertEquals(number(-1), quotient.getCoefficient(exponent(0, 1)));

    assertEquals(number(2), remainder.getCoefficient(exponent(0, 2)));

    q = parser.parse("2*x - 2*y");
    result = p.pseudoDivision(q, "x");

    quotient = result.get(0);
    remainder = result.get(1);

    assertEquals(number(0), quotient.getCoefficient(exponent(0, 0)));
    assertEquals(number(2), quotient.getCoefficient(exponent(1, 0)));
    assertEquals(number(2), quotient.getCoefficient(exponent(0, 1)));
    assertEquals(number(0), quotient.getCoefficient(exponent(1, 1)));

    assertEquals(number(0), remainder.getCoefficient(exponent(0, 0)));
    assertEquals(number(0), remainder.getCoefficient(exponent(0, 1)));
    assertEquals(number(0), remainder.getCoefficient(exponent(2, 0)));
    assertEquals(number(8), remainder.getCoefficient(exponent(0, 2)));
  }

  @Test
  public void testDivision() {
    MultivariatePolynomial p = parser.parse("x^2 + y^2");
    MultivariatePolynomial q = parser.parse("2*x - 2*y");

    List<MultivariatePolynomial> result = p.divide(q, "x");

    MultivariatePolynomial quotient = result.get(0);
    MultivariatePolynomial remainder = result.get(1);

    assertEquals(number(0), quotient.getCoefficient(exponent(0, 0)));
    assertEquals(number(0), quotient.getCoefficient(exponent(1, 0)));
    assertEquals(number(0), quotient.getCoefficient(exponent(0, 1)));
    assertEquals(number(0), quotient.getCoefficient(exponent(1, 1)));
    assertEquals(number(0), quotient.getCoefficient(exponent(0, 2)));
    assertEquals(number(0), quotient.getCoefficient(exponent(2, 0)));
    assertEquals(number(0), quotient.getCoefficient(exponent(2, 2)));

    assertEquals(number(1), remainder.getCoefficient(exponent(0, 2)));
    assertEquals(number(1), remainder.getCoefficient(exponent(2, 0)));
  }

  @Test
  public void testResultant() {
    MultivariatePolynomial p = parser.parse("x^2 + x + 1");
    MultivariatePolynomial q = parser.parse("z - x^7 + 1");

    MultivariatePolynomial result = p.resultant(q, "x");
    System.out.println(result);

    assertEquals(number(3), result.getCoefficient(exponent(0, 0)));
    assertEquals(number(3), result.getCoefficient(exponent(0, 1)));
    assertEquals(number(1), result.getCoefficient(exponent(0, 2)));
    assertEquals(number(0), result.getCoefficient(exponent(0, 3)));
    assertEquals(number(0), result.getCoefficient(exponent(0, 4)));

    p = parser.parse("x*z^2-y^3");
    q = parser.parse("-x*y^2 + x*z^2 - y^3");

    result = p.resultant(q, "z");
    System.out.println(result);

    assertEquals(number(0), result.getCoefficient(exponent(0, 0, 0)));
    assertEquals(number(0), result.getCoefficient(exponent(0, 1, 0)));
    assertEquals(number(0), result.getCoefficient(exponent(0, 2, 0)));
    assertEquals(number(0), result.getCoefficient(exponent(0, 3, 0)));
    assertEquals(number(0), result.getCoefficient(exponent(0, 4, 0)));
    assertEquals(number(0), result.getCoefficient(exponent(1, 0, 0)));
    assertEquals(number(0), result.getCoefficient(exponent(1, 1, 0)));
    assertEquals(number(0), result.getCoefficient(exponent(1, 2, 0)));
    assertEquals(number(-1), result.getCoefficient(exponent(2, 2, 0))); // todo: investigate why the sign is wrong

  }
}
