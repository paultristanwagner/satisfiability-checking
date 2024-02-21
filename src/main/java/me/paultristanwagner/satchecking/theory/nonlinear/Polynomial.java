package me.paultristanwagner.satchecking.theory.nonlinear;

import me.paultristanwagner.satchecking.parse.Parser;
import me.paultristanwagner.satchecking.parse.PolynomialParser;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;
import me.paultristanwagner.satchecking.theory.arithmetic.Rational;

import java.math.BigInteger;
import java.util.*;

import static me.paultristanwagner.satchecking.theory.arithmetic.Number.*;
import static me.paultristanwagner.satchecking.theory.nonlinear.Exponent.exponent;
import static me.paultristanwagner.satchecking.theory.nonlinear.Interval.pointInterval;
import static me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomial.multivariatePolynomial;
import static me.paultristanwagner.satchecking.theory.nonlinear.RealAlgebraicNumber.realAlgebraicNumber;

public class Polynomial {

  public static void main(String[] args) {
    Parser<MultivariatePolynomial> parser = new PolynomialParser();
    Polynomial p = parser.parse("x^5+x^4+x^2+x+2").toUnivariatePolynomial();
    Polynomial q = parser.parse("3x^5-7x^3+3x^2").toUnivariatePolynomial();

    System.out.println(p);
    System.out.println(q);
    System.out.println(p.pow(100).squareFreeFactorization());
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

  public Number evaluate(RealAlgebraicNumber realAlgebraicNumber) {
    if (realAlgebraicNumber.isNumeric()) {
      return evaluate(realAlgebraicNumber.numericValue());
    }

    MultivariatePolynomial multivariatePolynomial = toMultivariatePolynomial("x");
    MultivariatePolynomial substituted = multivariatePolynomial.substitute(Map.of("x", realAlgebraicNumber));
    Polynomial univariate = substituted.toUnivariatePolynomial();

    if (univariate.getDegree() != 0) {
      return ZERO();
    }

    return univariate.getCoefficients()[0];
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

  public boolean isConstant() {
    return degree == 0;
  }

  public Polynomial toIntegerPolynomial() {
    BigInteger lcm = coefficients[0].getDenominator();
    for (int i = 1; i < coefficients.length; i++) {
      BigInteger gcd = lcm.gcd(coefficients[i].getDenominator());
      lcm = lcm.multiply(coefficients[i].getDenominator()).divide(gcd);
    }

    Number[] newCoefficients = new Number[coefficients.length];
    for (int i = 0; i < coefficients.length; i++) {
      newCoefficients[i] = coefficients[i].multiply(new Rational(lcm));
    }
    return new Polynomial(newCoefficients);
  }

  public Number content() {
    Number content = null;

    for (Number coefficient : coefficients) {
      if (coefficient.isZero()) {
        continue;
      }

      if (!coefficient.isInteger()) {
        throw new IllegalArgumentException("Cannot calculate content of non-integer coefficients");
      }

      if (content == null) {
        content = coefficient;
      } else {
        content = content.gcd(coefficient);
      }
    }

    return content;
  }

  public List<Polynomial> pseudoDivision(Polynomial other) {
    int a = degree;
    int b = other.degree;

    Number bLC = other.getLeadingCoefficient();
    Number pow = bLC.pow(a - b + 1);

    return polynomial(pow).multiply(this).divide(other);
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
      Polynomial v = s.multiply(other);
      r = r.subtract(v);
    }

    return List.of(q, r);
  }

  public Polynomial mod(Polynomial other) {
    return divide(other).get(1);
  }

  // todo: this can be done more efficiently
  public Polynomial gcd(Polynomial other) {
    if(this.isZero()) {
      return other;
    } else if(other.isZero()) {
      return this;
    }

    Polynomial thisInteger = toIntegerPolynomial();
    Polynomial otherInteger = other.toIntegerPolynomial();
    Polynomial nonNormalizedGcd = thisInteger.nonNormalizedGcd(otherInteger);

    return nonNormalizedGcd.divide(constant(nonNormalizedGcd.getLeadingCoefficient())).get(0);
  }

  public Polynomial nonNormalizedGcdOld(Polynomial other) {
    Polynomial a = this;
    Polynomial b = other;
    while (!b.isZero()) {
      Polynomial r = a.mod(b);
      a = b;
      b = r;
    }

    return a;
  }

  public Polynomial nonNormalizedGcd(Polynomial other) {
    Polynomial a = this.toIntegerPolynomial();
    Polynomial b = other.toIntegerPolynomial();
    while (!b.isZero()) {
      Polynomial r = a.pseudoDivision(b).get(1).toIntegerPolynomial();
      if (r.isZero()) {
        return b;
      }

      Number content = r.content();
      r = r.divide(constant(content)).get(0);
      a = b;
      b = r;
    }

    return a;
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

    for (int i = 1; !C.isOne(); i++) {
      P = C.gcd(D);

      C = C.divide(P).get(0);
      D = D.divide(P).get(0).subtract(C.getDerivative());

      result.add(P);
    }

    return result;
  }

  public boolean isSquareFree() {
    List<Polynomial> squareFreeFactors = squareFreeFactorization();
    for (int i = 0; i < squareFreeFactors.size(); i++) {
      if ((i + 1) % 2 == 0 && !squareFreeFactors.get(i).isConstant()) {
        return false;
      }
    }

    return true;
  }

  public List<Polynomial> sturmSequence() {
    if (isConstant()) {
      return List.of(this);
    }

    List<Polynomial> sturmSequence = new ArrayList<>();
    sturmSequence.add(this);
    sturmSequence.add(this.getDerivative());
    while (true) {
      Polynomial p = sturmSequence.get(sturmSequence.size() - 2);
      Polynomial q = sturmSequence.get(sturmSequence.size() - 1);
      Polynomial negRem = p.divide(q).get(1).negate();

      if (negRem.isZero()) {
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
      if (eval.isZero()) {
        continue;
      }

      if (eval.isPositive() && sign == -1 || eval.isNegative() && sign == 1) {
        signChanges++;
      }

      if (eval.isPositive()) {
        sign = 1;
      } else if (eval.isNegative()) {
        sign = -1;
      }
    }

    return signChanges;
  }

  public int numberOfRealRoots() {
    Number cauchyBound = cauchyBound();

    return numberOfRealRoots(cauchyBound.negate(), cauchyBound);
  }

  public boolean hasRealRootAt(Number x) {
    return evaluate(x).isZero();
  }

  public int numberOfRealRoots(Number a, Number b) {
    return numberOfRealRoots(a, b, sturmSequence());
  }

  public int numberOfRealRoots(Number a, Number b, List<Polynomial> sturmSequence) {
    int roots =
        sturmSequenceEvaluation(a, sturmSequence) - sturmSequenceEvaluation(b, sturmSequence);
    if (hasRealRootAt(b)) {
      roots--;
    }

    return roots;
  }

  public Number cauchyBound() {
    Number highestRatio = Number.ZERO();

    Number lcoeff = this.getLeadingCoefficient();
    for (int i = 0; i < degree; i++) {
      Number c = coefficients[i];
      Number absRatio = c.divide(lcoeff).abs();

      if (absRatio.greaterThan(highestRatio)) {
        highestRatio = absRatio;
      }
    }

    return ONE().add(highestRatio);
  }

  public Set<RealAlgebraicNumber> isolateRoots() {
    Number cauchyBound = cauchyBound();
    return isolateRoots(cauchyBound.negate(), cauchyBound);
  }

  public Set<RealAlgebraicNumber> isolateRoots(Number lowerBound, Number upperBound) {
    if (isConstant()) {
      return Set.of();
    }

    List<Polynomial> squareFreeFactors = squareFreeFactorization();

    if (squareFreeFactors.size() > 1) {
      Set<RealAlgebraicNumber> roots = new HashSet<>();
      for (Polynomial squareFreeFactor : squareFreeFactors) {
        if (squareFreeFactor.isConstant()) {
          continue;
        }

        Set<RealAlgebraicNumber> factorRoots = squareFreeFactor.isolateRoots(lowerBound, upperBound);
        roots.addAll(factorRoots);
      }

      return roots;
    }

    int numberOfRealRoots = numberOfRealRoots(lowerBound, upperBound);

    if (numberOfRealRoots == 0) {
      return Set.of();
    }

    if (numberOfRealRoots == 1) {
      return Set.of(realAlgebraicNumber(this, lowerBound, upperBound));
    }

    Number split;
    if (lowerBound.isNegative() && upperBound.isPositive() || lowerBound.isPositive() && upperBound.isNegative()) {
      split = ZERO();
    } else {
      split = lowerBound.midpoint(upperBound);
    }

    Set<RealAlgebraicNumber> leftRoots = new HashSet<>(isolateRoots(lowerBound, split));
    Set<RealAlgebraicNumber> rightRoots = isolateRoots(split, upperBound);
    leftRoots.addAll(rightRoots);

    if (hasRealRootAt(split)) {
      leftRoots.add(realAlgebraicNumber(split));
    }

    return leftRoots;
  }

  public MultivariatePolynomial toMultivariatePolynomial(String variable) {
    List<String> variables = List.of(variable);
    Map<Exponent, Number> coefficients = new HashMap<>();
    for (int i = 0; i <= degree; i++) {
      coefficients.put(exponent(i), this.coefficients[i]);
    }

    return multivariatePolynomial(coefficients, variables);
  }

  public Set<Double> isolateRootsAsDoubles() {
    Number epsilon = number(2).pow(-54);

    Set<RealAlgebraicNumber> roots = isolateRoots();
    Set<Double> result = new HashSet<>();
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

  public Interval evaluate(Interval interval) {
    Interval current = null;
    for (int i = 0; i < coefficients.length; i++) {
      Number coefficient = coefficients[i];

      Interval term;
      if (i == 0) {
        term = pointInterval(coefficient);
      } else {
        term = interval.pow(i).multiply(coefficient);
      }

      if (current == null) {
        current = term;
      } else {
        current = current.add(term);
      }
    }

    return current;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Polynomial that = (Polynomial) o;
    return degree == that.degree && Arrays.equals(coefficients, that.coefficients);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(degree);
    result = 31 * result + Arrays.hashCode(coefficients);
    return result;
  }
}
