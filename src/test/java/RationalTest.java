import me.paultristanwagner.satchecking.theory.arithmetic.Rational;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RationalTest {

  private static final Rational ZERO = Rational.ZERO();
  private static final Rational ONE = Rational.ONE();
  private static final Rational TWO = new Rational(2);
  private static final Rational HALF = new Rational(1, 2);
  private static final Rational THIRD = new Rational(1, 3);
  private static final Rational TWO_THIRDS = new Rational(2, 3);
  private static final Rational MINUS_ONE = ONE.negate();

  @Test
  public void testParsing() {
    // Test basic parsing
    assertEquals(Rational.ZERO(), Rational.parse("0"));
    assertEquals(Rational.ZERO(), Rational.parse("+0"));
    assertEquals(Rational.ZERO(), Rational.parse("-0"));
    assertEquals(Rational.ONE(), Rational.parse("1"));
    assertEquals(Rational.ONE(), Rational.parse("+1"));

    // Test rational parsing
    assertEquals(Rational.ONE(), Rational.parse("1/1"));
    assertEquals(Rational.ONE(), Rational.parse("+1/1"));
    assertEquals(Rational.ONE(), Rational.parse("-1/-1"));

    assertEquals(new Rational(1, 2), Rational.parse("1/2"));
    assertEquals(new Rational(1, 2), Rational.parse("+1/2"));
    assertEquals(new Rational(1, 2), Rational.parse("-1/-2"));

    assertEquals(new Rational(1, 3), Rational.parse("1/3"));
    assertEquals(new Rational(1, 3), Rational.parse("+1/3"));
    assertEquals(new Rational(1, 3), Rational.parse("-1/-3"));

    assertEquals(new Rational(-1, 2), Rational.parse("-1/2"));
    assertEquals(new Rational(-1, 2), Rational.parse("+1/-2"));

    // Test decimal parsing
    assertEquals(new Rational(1, 2), Rational.parse("0.5"));
    assertEquals(new Rational(1, 2), Rational.parse("+0.5"));
    assertEquals(new Rational(-1, 2), Rational.parse("-0.5"));
    assertEquals(new Rational(1, 2), Rational.parse("0.50"));
    assertEquals(new Rational(1, 2), Rational.parse("+0.50"));

    assertEquals(new Rational(3, 2), Rational.parse("1.5"));

    // Test invalid parsing
    assertThrows(ArithmeticException.class, () -> Rational.parse("1/0"));
    assertThrows(NumberFormatException.class, () -> Rational.parse("1.-0"));
  }

  @Test
  public void testComparison() {
    assertTrue(ZERO.lessThan(ONE)); // 0 < 1
    assertTrue(ZERO.lessThan(HALF)); // 0 < 1/2
    assertTrue(ZERO.lessThan(THIRD)); // 0 < 1/3
    assertTrue(ZERO.lessThan(TWO_THIRDS)); // 0 < 2/3
    assertTrue(MINUS_ONE.lessThan(ZERO)); // -1 < 0

    assertTrue(ONE.greaterThan(ZERO)); // 1 > 0

    assertTrue(HALF.lessThan(ONE)); // 1/2 < 1
    assertTrue(HALF.lessThan(TWO_THIRDS)); // 1/2 < 2/3
  }

  @Test
  public void testArithmetic() {
    // Test addition
    assertEquals(Rational.ONE(), ZERO.add(ONE));
    assertEquals(Rational.ONE(), ONE.add(ZERO));
    assertEquals(Rational.ONE(), HALF.add(HALF));
    assertEquals(Rational.ONE(), THIRD.add(TWO_THIRDS));
    assertEquals(Rational.ONE(), TWO_THIRDS.add(THIRD));
    assertEquals(Rational.ZERO(), ONE.add(MINUS_ONE));
    assertEquals(Rational.ZERO(), MINUS_ONE.add(ONE));

    // Test subtraction
    assertEquals(Rational.ZERO(), ONE.subtract(ONE));
    assertEquals(Rational.ZERO(), HALF.subtract(HALF));
    assertEquals(Rational.ZERO(), THIRD.subtract(THIRD));
    assertEquals(Rational.ZERO(), TWO_THIRDS.subtract(TWO_THIRDS));
    assertEquals(THIRD, ONE.subtract(TWO_THIRDS));

    // Test multiplication
    assertEquals(Rational.ONE(), ONE.multiply(ONE));
    assertEquals(Rational.ONE(), HALF.multiply(TWO));
    assertEquals(Rational.ONE(), ONE.multiply(MINUS_ONE).multiply(MINUS_ONE));
    assertEquals(Rational.ONE(), MINUS_ONE.multiply(ONE).multiply(MINUS_ONE));

    // Test division
    assertEquals(Rational.ONE(), ONE.divide(ONE));
    assertEquals(Rational.ONE(), HALF.divide(HALF));
    assertEquals(Rational.ONE(), THIRD.divide(THIRD));
    assertEquals(Rational.ONE(), TWO_THIRDS.divide(TWO_THIRDS));
    assertEquals(Rational.ONE(), ONE.divide(MINUS_ONE).divide(MINUS_ONE));
    assertEquals(Rational.ONE(), MINUS_ONE.divide(ONE).divide(MINUS_ONE));

    // Test negation
    assertEquals(Rational.ONE(), ONE.negate().negate());
    assertEquals(Rational.ONE(), MINUS_ONE.negate());
  }
}
