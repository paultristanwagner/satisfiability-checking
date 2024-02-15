package me.paultristanwagner.satchecking.theory.nonlinear;

import static me.paultristanwagner.satchecking.theory.arithmetic.Number.ZERO;
import static me.paultristanwagner.satchecking.theory.arithmetic.Number.number;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import me.paultristanwagner.satchecking.theory.arithmetic.Number;

public class RealAlgebraicNumber {

  private Number value;
  private Polynomial polynomial;
  private Number lowerBound;
  private Number upperBound;

  private RealAlgebraicNumber(
      Number value, Polynomial polynomial, Number lowerBound, Number upperBound) {
    System.out.println("defining ran: " + value + " " + polynomial + " " + lowerBound + " " + upperBound);
    this.value = value;
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;

    if (polynomial == null) {
      return;
    }

    // finding minimal polynomial
    List<Polynomial> squareFreeFactors = polynomial.squareFreeFactorization();
    for (Polynomial squareFreeFactor : squareFreeFactors) {
      if (squareFreeFactor.isConstant()) {
        continue;
      }

      System.out.println("factor = " + squareFreeFactor);
      int numberRoots = squareFreeFactor.numberOfRealRoots(lowerBound, upperBound);
      System.out.println("number of roots = " + numberRoots);
      System.out.println("interval: " + lowerBound + " " + upperBound);
      if (numberRoots > 0) {
        this.polynomial = squareFreeFactor;
        break;
      }
    }

    if (this.polynomial == null) {
      throw new IllegalArgumentException("No real roots in interval");
    }

    if (this.polynomial.getDegree() == 1) {
      this.value =
          this.polynomial
              .getCoefficients()[0]
              .negate()
              .divide(this.polynomial.getCoefficients()[1]);
      this.polynomial = null;
      this.lowerBound = null;
      this.upperBound = null;
    }
  }

  public static RealAlgebraicNumber realAlgebraicNumber(Number value) {
    return new RealAlgebraicNumber(value, null, value, value);
  }

  public static RealAlgebraicNumber realAlgebraicNumber(
      Polynomial polynomial, Number lowerBound, Number upperBound) {
    return new RealAlgebraicNumber(null, polynomial, lowerBound, upperBound);
  }

  public boolean isNumeric() {
    return value != null || polynomial.getDegree() == 1;
  }

  public Number numericValue() {
    if (!isNumeric()) {
      throw new IllegalStateException("Not a numeric value");
    }

    if (value != null) {
      return value;
    }

    return polynomial.getCoefficients()[0].negate().divide(polynomial.getCoefficients()[1]);
  }

  public Number getLength() {
    if (value != null) {
      return ZERO();
    }

    return upperBound.subtract(lowerBound);
  }

  public void refine() {
    if(isNumeric()) {
      return;
    }

    Number mid = lowerBound.midpoint(upperBound);

    if(this.polynomial.hasRealRootAt(mid)) {
      Number quarter = lowerBound.midpoint(mid);
      Number threeQuarters = mid.midpoint(upperBound);
      this.lowerBound = quarter;
      this.upperBound = threeQuarters;
      return;
    }

    int numberRootsLeft = polynomial.numberOfRealRoots(lowerBound, mid); // todo: check right bound

    if (numberRootsLeft == 1) {
      upperBound = mid;
    } else {
      lowerBound = mid;
    }
  }

  public void refine(Number epsilon) {
    if(isNumeric()) {
      return;
    }

    while(getLength().greaterThanOrEqual(epsilon)) {
      refine();
    }
  }

  public Number approximate(Number epsilon) {
    if (isNumeric()) {
      return numericValue();
    }

    refine(epsilon);
    return lowerBound.add(upperBound).divide(number(2));
  }

  public double approximateAsDouble() {
    return approximate(number(2).pow(-54)).approximateAsDouble();
  }

  public float approximateAsFloat() {
    return approximate(number(2).pow(-24)).approximateAsFloat();
  }

  public boolean lessThan(RealAlgebraicNumber other) {
    if (this.equals(other)) {
      return false;
    }

    System.out.println("comparing " + this + " and " + other + " ...");

    if (this.isNumeric() && other.isNumeric()) {
      return this.numericValue().lessThan(other.numericValue());
    }

    while(true) {
      if(this.isNumeric() && !other.isNumeric()) {
        if(this.numericValue().lessThanOrEqual(other.lowerBound)) {
          return true;
        } else if(this.numericValue().greaterThanOrEqual(other.upperBound)) {
          return false;
        }

        other.refine();
      } else if (!this.isNumeric() && other.isNumeric()) {
        if(this.upperBound.lessThanOrEqual(other.numericValue())) {
          return true;
        } else if(this.lowerBound.greaterThanOrEqual(other.numericValue())) {
          return false;
        }

        this.refine();
      } else {
        if(this.upperBound.lessThanOrEqual(other.lowerBound)) {
          return true;
        } else if(this.lowerBound.greaterThanOrEqual(other.upperBound)) {
          return false;
        }

        this.refine();
        other.refine();
      }
    }
  }

  public boolean greaterThan(RealAlgebraicNumber other) {
    return other.lessThan(this);
  }

  public boolean lessThanOrEqual(RealAlgebraicNumber other) {
    return this.equals(other) || this.lessThan(other);
  }

  public boolean greaterThanOrEqual(RealAlgebraicNumber other) {
    return this.equals(other) || this.greaterThan(other);
  }

  public Number getLowerBound() {
    if (lowerBound == null) {
      throw new IllegalStateException("No lower bound");
    }

    return lowerBound;
  }

  public Number getUpperBound() {
    if (upperBound == null) {
      throw new IllegalStateException("No upper bound");
    }

    return upperBound;
  }

  public Polynomial getPolynomial() {
    if (polynomial == null) {
      throw new IllegalStateException("No polynomial");
    }

    return polynomial;
  }

  @Override
  public String toString() {
    if (value != null) {
      return value.toString();
    } else {
      return "(" + polynomial.toString() + ", " + lowerBound + ", " + upperBound + ")";
    }
  }

  @Override
  public boolean equals(Object o) {
    System.out.println();
    System.out.println("checking " + this + " and " + o + " for equality...");

    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RealAlgebraicNumber other = (RealAlgebraicNumber) o;

    if(Objects.equals(value, other.value) && Objects.equals(polynomial, other.polynomial) && Objects.equals(lowerBound, other.lowerBound) && Objects.equals(upperBound, other.upperBound)){
      System.out.println("  identical");
      return true;
    }

    if (this.isNumeric() && other.isNumeric()) {
      System.out.println("  both numeric: result = " + this.numericValue().equals(other.numericValue()));
      return this.numericValue().equals(other.numericValue());
    } else if (this.isNumeric() && !other.isNumeric()) {
      System.out.println("  this numeric");
      Number numericValue = this.numericValue();
      boolean result = numericValue.greaterThan(other.lowerBound)
          && numericValue.lessThan(other.upperBound)
          && other.polynomial.hasRealRootAt(numericValue);
      System.out.println("  result = " + result);
      return result;
    } else if (!this.isNumeric() && other.isNumeric()) {
      System.out.println("  other numeric");
      Number numericValue = other.numericValue();
      boolean result = numericValue.greaterThan(this.lowerBound)
          && numericValue.lessThan(this.upperBound)
          && this.polynomial.hasRealRootAt(numericValue);
      System.out.println("  result = " + result);
      return result;
    }

    if(this.lowerBound.greaterThanOrEqual(other.upperBound) || this.upperBound.lessThanOrEqual(other.lowerBound)) {
      System.out.println("  non overlapping bounds");
      return false;
    }

    System.out.println("  continue checking");
    Number innerLowerBound = this.lowerBound.greaterThan(other.lowerBound) ? this.lowerBound : other.lowerBound;
    Number innerUpperBound = this.upperBound.lessThan(other.upperBound) ? this.upperBound : other.upperBound;
    System.out.println("  overlap = " + innerLowerBound + " " + innerUpperBound);

    int thisInnerRoots = this.polynomial.numberOfRealRoots(innerLowerBound, innerUpperBound);
    int otherInnerRoots = other.polynomial.numberOfRealRoots(innerLowerBound, innerUpperBound);

    if(thisInnerRoots != otherInnerRoots) {
      System.out.println("  different number of roots in overlap");
      return false;
    }

    if(this.polynomial.equals(other.polynomial)) {
      System.out.println(  "  same number of roots in overlap and same polynomial");
      return true;
    }

    // if the difference has a root in the interval, the polynomials have a common root // todo: test this
    Polynomial difference = this.polynomial.subtract(other.polynomial);

    Set<RealAlgebraicNumber> differenceRoots = difference.isolateRoots(innerLowerBound, innerUpperBound);
    for (RealAlgebraicNumber differenceRoot : differenceRoots) {
      System.out.println("  difference root = " + differenceRoot);
      Number evaluation = difference.evaluate(differenceRoot);
      if(evaluation.isZero()) {
        System.out.println("  difference root is a common root");
        return true;
      }
    }

    System.out.println("  no common root");
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, polynomial, lowerBound, upperBound);
  }
}
