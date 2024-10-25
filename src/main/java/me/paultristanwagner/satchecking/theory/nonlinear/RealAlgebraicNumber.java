package me.paultristanwagner.satchecking.theory.nonlinear;

import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import java.util.List;
import java.util.Objects;

import static me.paultristanwagner.satchecking.theory.arithmetic.Number.ZERO;
import static me.paultristanwagner.satchecking.theory.arithmetic.Number.number;

public class RealAlgebraicNumber {

  private Number value;
  private Polynomial polynomial;
  private Number lowerBound;
  private Number upperBound;

  private RealAlgebraicNumber(Number value, Polynomial polynomial, Number lowerBound, Number upperBound, boolean minimal) {
    this.value = value;
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;

    if (polynomial == null) {
      return;
    }

    if(minimal) {
      this.polynomial = polynomial;
      return;
    }

    // finding minimal polynomial
    // todo: improve efficiency
    List<Polynomial> squareFreeFactors = polynomial.squareFreeFactorization();
    for (Polynomial squareFreeFactor : squareFreeFactors) {
      if (squareFreeFactor.isConstant()) {
        continue;
      }

      int numberRoots = squareFreeFactor.numberOfRealRoots(lowerBound, upperBound);
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
    return new RealAlgebraicNumber(value, null, value, value, true);
  }

  public static RealAlgebraicNumber realAlgebraicNumber(
      Polynomial polynomial, Number lowerBound, Number upperBound) {
    return new RealAlgebraicNumber(null, polynomial, lowerBound, upperBound, false);
  }

  public RealAlgebraicNumber copy() {
    return new RealAlgebraicNumber(value, polynomial, lowerBound, upperBound, true);
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
    if (isNumeric()) {
      return;
    }

    Number mid = lowerBound.midpoint(upperBound);

    if (this.polynomial.hasRealRootAt(mid)) {
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
    if (isNumeric()) {
      return;
    }

    while (getLength().greaterThanOrEqual(epsilon)) {
      refine();
    }
  }

  public Number approximate(Number epsilon) {
    if (isNumeric()) {
      return numericValue();
    }

    RealAlgebraicNumber copy = copy();

    copy.refine(epsilon);
    return copy.lowerBound.add(copy.upperBound).divide(number(2));
  }

  public boolean isZero() {
    if (isNumeric()) {
      return numericValue().isZero();
    }

    return ZERO().greaterThan(lowerBound) && ZERO().lessThan(upperBound) && this.polynomial.hasRealRootAt(ZERO());
  }

  public boolean isPositive() {
    if(isZero()) {
      return false;
    } else if (isNumeric()) {
      return numericValue().isPositive();
    }

    while(true) {
      if (this.lowerBound.isPositive()) {
        return true;
      } else if (this.upperBound.isNegative()) {
        return false;
      }

      this.refine();
    }
  }

  public boolean isNegative() {
    return !isZero() && !isPositive();
  }

  public int sign() {
    if (isZero()) {
      return 0;
    } else if (isPositive()) {
      return 1;
    } else {
      return -1;
    }
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

    if (this.isNumeric() && other.isNumeric()) {
      return this.numericValue().lessThan(other.numericValue());
    }

    while (true) {
      if (this.isNumeric() && !other.isNumeric()) {
        if (this.numericValue().lessThanOrEqual(other.lowerBound)) {
          return true;
        } else if (this.numericValue().greaterThanOrEqual(other.upperBound)) {
          return false;
        }

        other.refine();
      } else if (!this.isNumeric() && other.isNumeric()) {
        if (this.upperBound.lessThanOrEqual(other.numericValue())) {
          return true;
        } else if (this.lowerBound.greaterThanOrEqual(other.numericValue())) {
          return false;
        }

        this.refine();
      } else {
        if (this.upperBound.lessThanOrEqual(other.lowerBound)) {
          return true;
        } else if (this.lowerBound.greaterThanOrEqual(other.upperBound)) {
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
      return "(" + polynomial.toString() + ", " + lowerBound + ", " + upperBound + ") ≈ " + approximateAsDouble();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RealAlgebraicNumber other = (RealAlgebraicNumber) o;

    if (Objects.equals(value, other.value) && Objects.equals(polynomial, other.polynomial) && Objects.equals(lowerBound, other.lowerBound) && Objects.equals(upperBound, other.upperBound)) {
      return true;
    }

    if (this.isNumeric() && other.isNumeric()) {
      return this.numericValue().equals(other.numericValue());
    } else if (this.isNumeric() && !other.isNumeric()) {
      Number numericValue = this.numericValue();
      return numericValue.greaterThan(other.lowerBound)
          && numericValue.lessThan(other.upperBound)
          && other.polynomial.hasRealRootAt(numericValue);
    } else if (!this.isNumeric() && other.isNumeric()) {
      Number numericValue = other.numericValue();
      return numericValue.greaterThan(this.lowerBound)
          && numericValue.lessThan(this.upperBound)
          && this.polynomial.hasRealRootAt(numericValue);
    }

    if (this.lowerBound.greaterThanOrEqual(other.upperBound) || this.upperBound.lessThanOrEqual(other.lowerBound)) {
      return false;
    }

    Number innerLowerBound = this.lowerBound.greaterThan(other.lowerBound) ? this.lowerBound : other.lowerBound;
    Number innerUpperBound = this.upperBound.lessThan(other.upperBound) ? this.upperBound : other.upperBound;

    int thisInnerRoots = this.polynomial.numberOfRealRoots(innerLowerBound, innerUpperBound);
    int otherInnerRoots = other.polynomial.numberOfRealRoots(innerLowerBound, innerUpperBound);

    if (thisInnerRoots != otherInnerRoots) {
      return false;
    }

    if (this.polynomial.equals(other.polynomial)) {
      return true;
    }

    Polynomial gcd = this.polynomial.gcd(other.polynomial);
    // todo: we can potentially also evaluate the gcd at the interval bounds
    return gcd.numberOfRealRoots(innerLowerBound, innerUpperBound) > 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, polynomial, lowerBound, upperBound);
  }
}
