package me.paultristanwagner.satchecking.theory.nonlinear;

import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.paultristanwagner.satchecking.theory.arithmetic.Number.*;
import static me.paultristanwagner.satchecking.theory.nonlinear.RealAlgebraicNumber.realAlgebraicNumber;

public class Polynomial {

  public static void main(String[] args) {
    // x^(4)-2 x^(2)+((1)/(2))

    Polynomial p = polynomial(parse("1/2"), ZERO(), parse("-2"), ZERO(), ONE());
    System.out.println("p = " + p);

    System.out.println("Sturm Sequence:");
    System.out.println(p.sturmSequence());

    System.out.println("Cauchy Bound:");
    System.out.println(p.cauchyBound());

    System.out.println("Number of real roots:");
    System.out.println(p.numberOfRealRoots());

    System.out.println("Real roots:");
    System.out.println(p.isolateRoots());

    System.out.println("Real roots as doubles:");
    System.out.println(p.isolateRootsAsDoubles());
  }

  private final int degree;
  private final Number[] coefficients; // smallest degree first

  private Polynomial(Number[] coefficients) {
    int degree = 0;
    for (int i = 0; i < coefficients.length; i++) {
      if (!coefficients[i].isZero()) {
        degree = i;
      }
    }

    this.degree = degree;
    this.coefficients = new Number[degree + 1];
    System.arraycopy(coefficients, 0, this.coefficients, 0, degree + 1);
  }

  public static Polynomial polynomial(Number... coefficients) {
    return new Polynomial(coefficients);
  }

  public static Polynomial constant(Number constant) {
    return polynomial(constant);
  }

  public static Polynomial xToThePowerOf(int exponent) {
    Number[] coefficients = new Number[exponent + 1];
    coefficients[exponent] = ONE();
    Arrays.fill(coefficients, 0, exponent, ZERO());
    return new Polynomial(coefficients);
  }

  public int getDegree() {
    return degree;
  }

  public Number[] getCoefficients() {
    return coefficients;
  }

  public Number evaluate(Number x) {
    Number result = ZERO();
    for (int i = 0; i < coefficients.length; i++) {
      result = result.add(coefficients[i].multiply(x.pow(i)));
    }
    return result;
  }

  public Polynomial add(Polynomial other) {
    int newDegree = Math.max(degree, other.degree);
    Number[] newCoefficients = new Number[newDegree + 1];
    for (int i = 0; i <= newDegree; i++) {
      Number coefficient = ZERO();
      if (i <= degree) {
        coefficient = coefficient.add(coefficients[i]);
      }
      if (i <= other.degree) {
        coefficient = coefficient.add(other.coefficients[i]);
      }
      newCoefficients[i] = coefficient;
    }
    return new Polynomial(newCoefficients);
  }

  public Polynomial negate() {
    Number[] newCoefficients = new Number[coefficients.length];
    for (int i = 0; i < coefficients.length; i++) {
      newCoefficients[i] = coefficients[i].negate();
    }
    return new Polynomial(newCoefficients);
  }

  public Polynomial subtract(Polynomial other) {
    return add(other.negate());
  }

  public Polynomial multiply(Polynomial other) {
    int newDegree = degree + other.degree;
    Number[] newCoefficients = new Number[newDegree + 1];
    for (int i = 0; i <= newDegree; i++) {
      Number coefficient = ZERO();
      for (int j = 0; j <= i; j++) {
        if (j <= degree && i - j <= other.degree) {
          coefficient = coefficient.add(coefficients[j].multiply(other.coefficients[i - j]));
        }
      }
      newCoefficients[i] = coefficient;
    }
    return new Polynomial(newCoefficients);
  }

  public Polynomial pow(int exponent) {
    if (exponent < 0) {
      throw new IllegalArgumentException("Exponent must be non-negative");
    }

    if (exponent == 0) {
      return polynomial(ONE());
    }

    // TODO: use binary exponentiation
    Polynomial result = this;
    for (int i = 1; i < exponent; i++) {
      result = result.multiply(this);
    }
    return result;
  }

  public Number getLeadingCoefficient() {
    return coefficients[degree];
  }

  public boolean isZero() {
    for (Number coefficient : coefficients) {
      if (!coefficient.isZero()) {
        return false;
      }
    }
    return true;
  }

  public boolean isOne() {
    return degree == 0 && coefficients[0].isOne();
  }

  public List<Polynomial> divide(Polynomial other) {
    if (other.isZero()) {
      throw new ArithmeticException("Cannot divide by zero");
    }

    Polynomial q = polynomial(ZERO());
    Polynomial r = this;
    int d = other.degree;

    while (!r.isZero() && r.degree >= d) {
      Number c = r.getLeadingCoefficient().divide(other.getLeadingCoefficient());
      Polynomial s = xToThePowerOf(r.degree - d).multiply(polynomial(c));
      q = q.add(s);
      r = r.subtract(s.multiply(other));
    }

    return List.of(q, r);
  }

  public Polynomial mod(Polynomial other) {
    return divide(other).get(1);
  }

  public Polynomial gcd(Polynomial other) {
    Polynomial a = this;
    Polynomial b = other;
    while (!b.isZero()) {
      Polynomial r = a.mod(b);
      a = b;
      b = r;
    }

    Polynomial normalized = a.divide(
        constant(a.getLeadingCoefficient())
    ).get(0);

    return normalized;
  }

  public Polynomial getDerivative() {
    if (degree == 0) {
      return polynomial(ZERO());
    }

    Number[] newCoefficients = new Number[coefficients.length - 1];
    for (int i = 0; i < newCoefficients.length; i++) {
      newCoefficients[i] = coefficients[i + 1].multiply(number(i + 1));
    }
    return new Polynomial(newCoefficients);
  }

  public List<Polynomial> squareFreeFactorization() {
    List<Polynomial> result = new ArrayList<>();

    Polynomial P = this;
    Polynomial G = P.gcd(P.getDerivative());
    Polynomial C = P.divide(G).get(0);
    Polynomial D = P.getDerivative().divide(G).get(0).subtract(C.getDerivative());

    for(int i = 1; !C.isOne(); i++) {
      P = C.gcd(D);
      C = C.divide(P).get(0);
      D = D.divide(P).get(0).subtract(C.getDerivative());

      result.add(P);
    }

    return result;
  }

  public List<Polynomial> sturmSequence() {
    List<Polynomial> sturmSequence = new ArrayList<>();
    sturmSequence.add(this);
    sturmSequence.add(this.getDerivative());
    while(true) {
      Polynomial p = sturmSequence.get(sturmSequence.size() - 2);
      Polynomial q = sturmSequence.get(sturmSequence.size() - 1);
      Polynomial negRem = p.divide(q).get(1).negate();

      if(negRem.isZero()) {
        break;
      }

      sturmSequence.add(negRem);
    }

    return sturmSequence;
  }

  private int sturmSequenceEvaluation(Number xi, List<Polynomial> sturmSequence) {
    int signChanges = 0;
    int sign = 0;

    for (Polynomial p : sturmSequence) {
      Number eval = p.evaluate(xi);
      if(eval.isZero()) {
        continue;
      }

      if(eval.isPositive() && sign == -1
        || eval.isNegative() && sign == 1) {
        signChanges++;
      }

      if(eval.isPositive()) {
        sign = 1;
      } else if(eval.isNegative()) {
        sign = -1;
      }
    }

    return signChanges;
  }

  public int numberOfRealRoots() {
    Number cauchyBound = cauchyBound();

    return numberOfRealRoots(cauchyBound.negate(), cauchyBound);
  }

  public int numberOfRealRoots(Number a, Number b) {
    return numberOfRealRoots(a, b, sturmSequence());
  }

  public int numberOfRealRoots(Number a, Number b, List<Polynomial> sturmSequence) {
    return sturmSequenceEvaluation(a, sturmSequence) - sturmSequenceEvaluation(b, sturmSequence);
  }

  public Number cauchyBound() {
    Number highestRatio = Number.ZERO();

    Number lcoeff = this.getLeadingCoefficient();
    for (int i = 0; i < degree; i++) {
      Number c = coefficients[i];
      Number absRatio = c.divide(lcoeff).abs();

      if(absRatio.greaterThan(highestRatio)) {
        highestRatio = absRatio;
      }
    }

    return ONE().add(highestRatio);
  }

  public List<RealAlgebraicNumber> isolateRoots() {
    Number cauchyBound = cauchyBound();
    return isolateRoots(cauchyBound.negate(), cauchyBound);
  }

  private List<RealAlgebraicNumber> isolateRoots(Number lowerBound, Number upperBound) {
    int numberOfRealRoots = numberOfRealRoots(lowerBound, upperBound);

    if(numberOfRealRoots == 0) {
      return new ArrayList<>();
    }

    if(numberOfRealRoots == 1) {
      Number eval = evaluate(upperBound);
      if(eval.isZero()) {
        return new ArrayList<>(List.of(
            realAlgebraicNumber(upperBound)
        ));
      }

      return new ArrayList<>(List.of(
          realAlgebraicNumber(this, lowerBound, upperBound)
      ));
    }

    Number mid = lowerBound.add(upperBound).divide(number(2));

    List<RealAlgebraicNumber> leftRoots = isolateRoots(lowerBound, mid);
    List<RealAlgebraicNumber> rightRoots = isolateRoots(mid, upperBound);
    leftRoots.addAll(rightRoots);

    return leftRoots;
  }

  public List<Double> isolateRootsAsDoubles() {
    Number epsilon = number(2).pow(-54);

    List<RealAlgebraicNumber> roots = isolateRoots();
    List<Double> result = new ArrayList<>();
    for (RealAlgebraicNumber root : roots) {
      result.add(root.approximate(epsilon).approximateAsDouble());
    }
    return result;
  }

  @Override
  public String toString() {
    if (isZero()) {
      return "0";
    }

    StringBuilder builder = new StringBuilder();
    for (int i = degree; i >= 0; i--) {
      Number coefficient = coefficients[i];
      if (coefficient.isZero()) {
        continue;
      }

      if (i != degree && coefficient.isNonNegative()) {
        builder.append("+");
      }

      if (coefficient.isOne()) {
        if (i == 0) {
          builder.append("1");
        } else if (i == 1) {
          builder.append("x");
        } else {
          builder.append("x^").append(i);
        }
      } else {
        if (i == 0) {
          builder.append(coefficient);
        } else if (i == 1) {
          builder.append(coefficient).append("x");
        } else {
          builder.append(coefficient).append("x^").append(i);
        }
      }
    }
    return builder.toString();
  }
}
