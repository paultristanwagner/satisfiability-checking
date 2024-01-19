package me.paultristanwagner.satchecking.theory.nonlinear;

import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import static me.paultristanwagner.satchecking.theory.arithmetic.Number.number;
import static me.paultristanwagner.satchecking.theory.nonlinear.Polynomial.polynomial;

public class RealAlgebraicNumber {

  public static void main(String[] args) {
    // x^2 + 1
    Polynomial p = polynomial(number(1), number(0), number(1));
    System.out.println(p);

    // x^2 - 1
    Polynomial q = polynomial(number(-1), number(0), number(1));
    System.out.println(q);

    Polynomial pq = p.multiply(q);
    System.out.println(pq);

    System.out.println(pq.squareFreeFactorization());
  }

  private final Number value;
  private final Polynomial polynomial;
  private final Number lowerBound;
  private final Number upperBound;

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

  @Override
  public String toString() {
    if (value != null) {
      return value.toString();
    } else {
      return "(" + polynomial.toString() + ", " + lowerBound + ", " + upperBound + ")";
    }
  }
}
