package me.paultristanwagner.satchecking.theory.nonlinear;

import static me.paultristanwagner.satchecking.theory.arithmetic.Number.ZERO;
import static me.paultristanwagner.satchecking.theory.arithmetic.Number.number;
import static me.paultristanwagner.satchecking.theory.nonlinear.Interval.IntervalBoundType.*;
import static me.paultristanwagner.satchecking.theory.nonlinear.RealAlgebraicNumber.realAlgebraicNumber;

import me.paultristanwagner.satchecking.theory.arithmetic.Number;

public class Interval {

  public enum IntervalBoundType {
    UNBOUNDED,
    OPEN,
    CLOSED
  }

  public static void main(String[] args) {
    Interval i1 = pointInterval(number(1));
    System.out.println(i1);
  }

  // todo: add cell datatype

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

    if (!upperBound.isNumeric()) {
      return realAlgebraicNumber(upperBound.getLowerBound());
    } else {
      return realAlgebraicNumber(lowerBound.getUpperBound());
    }
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
