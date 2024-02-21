package me.paultristanwagner.satchecking.theory.nonlinear;

import me.paultristanwagner.satchecking.parse.PolynomialParser;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import static me.paultristanwagner.satchecking.theory.arithmetic.Number.ZERO;
import static me.paultristanwagner.satchecking.theory.arithmetic.Number.number;
import static me.paultristanwagner.satchecking.theory.nonlinear.Interval.IntervalBoundType.*;
import static me.paultristanwagner.satchecking.theory.nonlinear.RealAlgebraicNumber.realAlgebraicNumber;

public class Interval {

  public static void main(String[] args) {
    PolynomialParser parser = new PolynomialParser();
    Polynomial p = parser.parse("x^2").toUnivariatePolynomial();
    Interval interval = interval(number(-1), number(5), CLOSED, CLOSED);
    System.out.println(p.evaluate(interval));
  }

  public enum IntervalBoundType {
    UNBOUNDED,
    OPEN,
    CLOSED
  }

  private final RealAlgebraicNumber lowerBound;
  private final RealAlgebraicNumber upperBound;
  private final IntervalBoundType lowerBoundType;
  private final IntervalBoundType upperBoundType;

  private Interval(
      RealAlgebraicNumber lowerBound,
      RealAlgebraicNumber upperBound,
      IntervalBoundType lowerBoundType,
      IntervalBoundType upperBoundType) {
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
    this.lowerBoundType = lowerBoundType;
    this.upperBoundType = upperBoundType;
  }

  public static Interval unboundedInterval() {
    return new Interval(null, null, UNBOUNDED, UNBOUNDED);
  }

  public static Interval intervalLowerUnbounded(
      RealAlgebraicNumber upperBound, IntervalBoundType upperBoundType) {
    return new Interval(null, upperBound, UNBOUNDED, upperBoundType);
  }

  public static Interval intervalUpperUnbounded(
      RealAlgebraicNumber lowerBound, IntervalBoundType lowerBoundType) {
    return new Interval(lowerBound, null, lowerBoundType, UNBOUNDED);
  }

  public static Interval intervalLowerUnbounded(
      Number upperBound, IntervalBoundType upperBoundType) {
    return intervalLowerUnbounded(realAlgebraicNumber(upperBound), upperBoundType);
  }

  public static Interval intervalUpperUnbounded(
      Number lowerBound, IntervalBoundType lowerBoundType) {
    return intervalUpperUnbounded(realAlgebraicNumber(lowerBound), lowerBoundType);
  }

  public static Interval interval(
      RealAlgebraicNumber lowerBound,
      RealAlgebraicNumber upperBound,
      IntervalBoundType lowerBoundType,
      IntervalBoundType upperBoundType) {
    return new Interval(lowerBound, upperBound, lowerBoundType, upperBoundType);
  }

  public static Interval interval(
      Number lowerBound,
      Number upperBound,
      IntervalBoundType lowerBoundType,
      IntervalBoundType upperBoundType) {
    return interval(
        realAlgebraicNumber(lowerBound),
        realAlgebraicNumber(upperBound),
        lowerBoundType,
        upperBoundType);
  }

  public static Interval pointInterval(RealAlgebraicNumber point) {
    return new Interval(point, point, CLOSED, CLOSED);
  }

  public static Interval pointInterval(Number number) {
    return pointInterval(realAlgebraicNumber(number));
  }

  // todo: improve return values when rational numbers are possible
  public RealAlgebraicNumber chooseSample() {
    if (lowerBoundType == UNBOUNDED && upperBoundType == UNBOUNDED) {
      return realAlgebraicNumber(ZERO());
    } else if (lowerBoundType == CLOSED) {
      return lowerBound; // todo: here we might be able to return a rational number
    } else if (upperBoundType == CLOSED) {
      return upperBound; // todo: here we might be able to return a rational number
    }

    if (lowerBoundType == UNBOUNDED && upperBoundType == OPEN) {
      if (upperBound.isNumeric()) {
        return realAlgebraicNumber(upperBound.numericValue().subtract(number(1)));
      } else {
        return realAlgebraicNumber(upperBound.getLowerBound());
      }
    }

    if (lowerBoundType == OPEN && upperBoundType == UNBOUNDED) {
      if (lowerBound.isNumeric()) {
        return realAlgebraicNumber(lowerBound.numericValue().add(number(1)));
      } else {
        return realAlgebraicNumber(lowerBound.getUpperBound());
      }
    }

    if (lowerBound.isNumeric() && upperBound.isNumeric()) {
      Number rationalMidpoint =
          lowerBound.numericValue().add(upperBound.numericValue()).divide(number(2));
      return realAlgebraicNumber(rationalMidpoint);
    }

    if (!upperBound.isNumeric() && lowerBound.isNumeric()) {
      if (upperBound.getLowerBound().equals(lowerBound.numericValue())) {
        upperBound.refine();
      }

      return realAlgebraicNumber(upperBound.getLowerBound());
    } else if (!lowerBound.isNumeric() && upperBound.isNumeric()) {
      if (lowerBound.getUpperBound().equals(upperBound.numericValue())) {
        lowerBound.refine();
      }

      return realAlgebraicNumber(lowerBound.getUpperBound());
    }

    return realAlgebraicNumber(lowerBound.getUpperBound());
  }

  public Interval add(Interval other) {
    if (lowerBoundType != CLOSED || other.lowerBoundType != CLOSED || upperBoundType != CLOSED || other.upperBoundType != CLOSED) {
      throw new IllegalArgumentException("Can only add closed intervals");
    }

    if (!lowerBound.isNumeric() || !upperBound.isNumeric() || !other.lowerBound.isNumeric() || !other.upperBound.isNumeric()) {
      throw new IllegalArgumentException("Can only add numeric intervals");
    }

    return interval(
        lowerBound.numericValue().add(other.lowerBound.numericValue()),
        upperBound.numericValue().add(other.upperBound.numericValue()),
        CLOSED,
        CLOSED);
  }

  public Interval subtract(Interval other) {
    if (lowerBoundType != CLOSED || other.lowerBoundType != CLOSED || upperBoundType != CLOSED || other.upperBoundType != CLOSED) {
      throw new IllegalArgumentException("Can only subtract closed intervals");
    }

    if (!lowerBound.isNumeric() || !upperBound.isNumeric() || !other.lowerBound.isNumeric() || !other.upperBound.isNumeric()) {
      throw new IllegalArgumentException("Can only subtract numeric intervals");
    }

    return interval(
        lowerBound.numericValue().subtract(other.upperBound.numericValue()),
        upperBound.numericValue().subtract(other.lowerBound.numericValue()),
        CLOSED,
        CLOSED);
  }

  public Interval multiply(Number number) {
    if (lowerBoundType != CLOSED || upperBoundType != CLOSED) {
      throw new IllegalArgumentException("Can only multiply closed intervals");
    }

    if (!lowerBound.isNumeric() || !upperBound.isNumeric()) {
      throw new IllegalArgumentException("Can only multiply numeric intervals");
    }

    Number lower = lowerBound.numericValue().multiply(number);
    Number upper = upperBound.numericValue().multiply(number);
    if (lower.greaterThan(upper)) {
      Number temp = lower;
      lower = upper;
      upper = temp;
    }

    return interval(
        lower,
        upper,
        CLOSED,
        CLOSED);
  }

  public Interval multiply(Interval other) {
    if (lowerBoundType != CLOSED || other.lowerBoundType != CLOSED || upperBoundType != CLOSED || other.upperBoundType != CLOSED) {
      throw new IllegalArgumentException("Can only multiply closed intervals");
    }

    if (!lowerBound.isNumeric() || !upperBound.isNumeric() || !other.lowerBound.isNumeric() || !other.upperBound.isNumeric()) {
      throw new IllegalArgumentException("Can only multiply numeric intervals");
    }

    Number[] products = {
        lowerBound.numericValue().multiply(other.lowerBound.numericValue()),
        lowerBound.numericValue().multiply(other.upperBound.numericValue()),
        upperBound.numericValue().multiply(other.lowerBound.numericValue()),
        upperBound.numericValue().multiply(other.upperBound.numericValue())
    };

    Number lower = products[0];
    Number upper = products[0];

    for (Number product : products) {
      if (product.lessThan(lower)) {
        lower = product;
      }

      if (product.greaterThan(upper)) {
        upper = product;
      }
    }

    return interval(lower, upper, CLOSED, CLOSED);
  }

  public Interval pow(int exponent) {
    if (lowerBoundType != CLOSED || upperBoundType != CLOSED) {
      throw new IllegalArgumentException("Can only raise closed intervals to a power");
    }

    if (!lowerBound.isNumeric() || !upperBound.isNumeric()) {
      throw new IllegalArgumentException("Can only raise numeric intervals to a power");
    }

    if (exponent < 0) {
      throw new IllegalArgumentException("Cannot raise an interval to a negative power");
    }

    if (exponent == 0) {
      return pointInterval(number(1));
    }

    // todo: use binary exponentiation
    Interval result = this;
    for (int i = 1; i < exponent; i++) {
      result = result.multiply(this);
    }

    if (exponent % 2 == 0 && lowerBound.isNegative()) {
      result = interval(number(0), result.getUpperBound().numericValue(), CLOSED, CLOSED);
    }

    return result;
  }

  public boolean contains(RealAlgebraicNumber number) {
    if (lowerBoundType == UNBOUNDED && upperBoundType == UNBOUNDED) {
      return true;
    } else if(lowerBoundType == OPEN && number.lessThanOrEqual(lowerBound)) {
      return false;
    } else if(upperBoundType == OPEN && number.greaterThanOrEqual(upperBound)) {
      return false;
    } else if(lowerBoundType == CLOSED && number.lessThan(lowerBound)) {
      return false;
    } else if(upperBoundType == CLOSED && number.greaterThan(upperBound)) {
      return false;
    }

    return true;
  }

  public boolean contains(Number number) {
    return contains(realAlgebraicNumber(number));
  }

  public boolean containsZero() {
    return contains(ZERO());
  }

  public int sign() {
    if (lowerBound.isZero() && upperBound.isZero()) {
      return 0;
    }

    if (containsZero()) {
      throw new IllegalArgumentException("The interval has no unique sign");
    }

    if (lowerBound.isZero()) {
      return upperBound.sign();
    }

    return lowerBound.sign();
  }

  public IntervalBoundType getLowerBoundType() {
    return lowerBoundType;
  }

  public IntervalBoundType getUpperBoundType() {
    return upperBoundType;
  }

  public RealAlgebraicNumber getLowerBound() {
    return lowerBound;
  }

  public RealAlgebraicNumber getUpperBound() {
    return upperBound;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    switch (lowerBoundType) {
      case UNBOUNDED -> sb.append("(-oo");
      case OPEN -> sb.append("(").append(lowerBound);
      case CLOSED -> sb.append("[").append(lowerBound);
    }

    sb.append(", ");

    switch (upperBoundType) {
      case UNBOUNDED -> sb.append("oo)");
      case OPEN -> sb.append(upperBound).append(")");
      case CLOSED -> sb.append(upperBound).append("]");
    }

    return sb.toString();
  }
}
