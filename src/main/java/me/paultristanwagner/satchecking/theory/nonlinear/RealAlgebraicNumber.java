package me.paultristanwagner.satchecking.theory.nonlinear;

import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import static me.paultristanwagner.satchecking.theory.arithmetic.Number.ZERO;
import static me.paultristanwagner.satchecking.theory.arithmetic.Number.number;
import static me.paultristanwagner.satchecking.theory.nonlinear.Polynomial.polynomial;

public class RealAlgebraicNumber {

  public static void main(String[] args) {
    // x^2 + 1
    Polynomial p = polynomial(number(1), number(0), number(1));
    System.out.println(p);

    // x^2 - 2
    Polynomial q = polynomial(number(-2), number(0), number(1));
    System.out.println(q);

    Polynomial pq = p.multiply(q);
    System.out.println(pq);

    System.out.println("Square-Free factorization:");
    System.out.println(pq.squareFreeFactorization());

    RealAlgebraicNumber sqrt2 = realAlgebraicNumber(q, number(0), number(2));
    System.out.println("sqrt(2) = " + sqrt2);

    Number epsilon = number(2).pow(-54);
    System.out.println(epsilon);

    Number sqrt2RationalApproximation = sqrt2.approximate(epsilon);
    System.out.println(sqrt2RationalApproximation);
    System.out.println(sqrt2RationalApproximation.approximateAsDouble());
    System.out.println(Math.sqrt(2));
  }

  private final Number value;
  private final Polynomial polynomial;
  private Number lowerBound;
  private Number upperBound;

  private RealAlgebraicNumber(Number value, Polynomial polynomial, Number lowerBound, Number upperBound) {
    this.value = value;
    this.polynomial = polynomial;
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
  }

  public static RealAlgebraicNumber realAlgebraicNumber(Number value) {
    return new RealAlgebraicNumber(value, null, null, null);
  }

  public static RealAlgebraicNumber realAlgebraicNumber(Polynomial polynomial, Number lowerBound, Number upperBound) {
    return new RealAlgebraicNumber(null, polynomial, lowerBound, upperBound);
  }

  public boolean isRational() {
    return value != null || polynomial.getDegree() == 1;
  }

  public Number getLength() {
    if(value != null) {
      return ZERO();
    }

    return upperBound.subtract(lowerBound);
  }

  public void refine(Number epsilon) {
    if(getLength().lessThan(epsilon)) {
      return;
    }

    Number mid = lowerBound.add(upperBound).divide(number(2));

    int numberRootsLeft = polynomial.numberOfRealRoots(lowerBound, mid);

    if(numberRootsLeft == 1) {
      upperBound = mid;
    } else {
      lowerBound = mid;
    }

    refine(epsilon);
  }

  public Number approximate(Number epsilon) {
    refine(epsilon);
    return lowerBound.add(upperBound).divide(number(2));
  }

  @Override
  public String toString() {
    if (value != null) {
      return value.toString();
    } else {
      return "(" + polynomial.toString() + ", " + lowerBound + ", " + upperBound + ")";
    }
  }
}
