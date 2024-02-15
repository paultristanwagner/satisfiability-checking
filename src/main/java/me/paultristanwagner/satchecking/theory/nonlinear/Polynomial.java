package me.paultristanwagner.satchecking.theory.nonlinear;

import static me.paultristanwagner.satchecking.theory.arithmetic.Number.*;
import static me.paultristanwagner.satchecking.theory.nonlinear.Exponent.exponent;
import static me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomial.multivariatePolynomial;
import static me.paultristanwagner.satchecking.theory.nonlinear.RealAlgebraicNumber.realAlgebraicNumber;

import java.util.*;

import me.paultristanwagner.satchecking.parse.Parser;
import me.paultristanwagner.satchecking.parse.PolynomialParser;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;

public class Polynomial {

  public static void main(String[] args) {
    Parser<MultivariatePolynomial> parser = new PolynomialParser();
    System.out.println(parser.parse("x^8+x^6-3*x^4-3*x^3+8*x^2+2*x-5"));
    Polynomial p = parser.parse("x^8+x^6-3*x^4-3*x^3+8*x^2+2*x-5").toUnivariatePolynomial();
    Polynomial q = parser.parse("3*x^6+5*x^4-4*x^2-9*x+21").toUnivariatePolynomial();

    System.out.println(p);
    System.out.println(q);
    System.out.println(p.add(q));
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

    if(univariate.getDegree() != 0) {
      throw new IllegalArgumentException("Univariate polynomial has degree != 0");
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
    Polynomial nonNormalizedGcd = nonNormalizedGcd(other);

    return nonNormalizedGcd.divide(constant(nonNormalizedGcd.getLeadingCoefficient())).get(0);
  }

  public Polynomial nonNormalizedGcd(Polynomial other) {
    Polynomial a = this;
    Polynomial b = other;
    while (!b.isZero()) {
      Polynomial r = a.mod(b);
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

    for (int i = 1; !C.isConstant(); i++) {
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
    if(isConstant()) {
      return Set.of();
    }

    List<Polynomial> squareFreeFactors = squareFreeFactorization();

    if(squareFreeFactors.size() > 1) {
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

    System.out.println("isolating roots of " + this + " in interval " + lowerBound + " " + upperBound);
    System.out.println("square free = " + isSquareFree());
    int numberOfRealRoots = numberOfRealRoots(lowerBound, upperBound);
    System.out.println("no of roots = " + numberOfRealRoots);

    if (numberOfRealRoots == 0) {
      return Set.of();
    }

    if (numberOfRealRoots == 1) {
      System.out.println("supposedly one root in interval " + lowerBound + " " + upperBound);
      return Set.of(realAlgebraicNumber(this, lowerBound, upperBound));
    }

    Number mid = lowerBound.midpoint(upperBound);

    Set<RealAlgebraicNumber> leftRoots = new HashSet<>(isolateRoots(lowerBound, mid));
    Set<RealAlgebraicNumber> rightRoots = isolateRoots(mid, upperBound);
    leftRoots.addAll(rightRoots);

    if (hasRealRootAt(mid)) {
      leftRoots.add(realAlgebraicNumber(mid));
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

  public Polynomial subresultant(Polynomial other) {
    Polynomial r_0 = this;
    Polynomial r_1 = other;
    Number gamma = null;
    Polynomial beta = null;
    Polynomial phi = null;
    int d = 0;

    for (int i = 1; !r_1.isZero(); i++) {
      Number[] coefficients_0 = r_0.getCoefficients();
      Number[]  coefficients_1 = r_1.getCoefficients();
      int degree_0 = coefficients_0.length - 1;
      int degree_1 = coefficients_1.length - 1;

      if (i != 1) {
        System.out.println("remainder: " + constant(gamma).negate().pow(d).divide(phi.pow(d - 1)).get(1));
        phi = constant(gamma).negate().pow(d).divide(phi.pow(d - 1)).get(0);
        beta = constant(gamma).multiply(phi.pow(degree_0 - degree_1)).negate();
      }

      d = degree_0 - degree_1;

      if(i == 1) {
        if(d % 2 == 0) {
          beta = constant(ONE().negate());
        } else {
          beta = constant(ONE());
        }
        phi = constant(ONE().negate());
      }

      gamma = coefficients_1[degree_1];
      Polynomial temp = r_1;
      System.out.println("remainder: " +
          constant(gamma.pow(d + 1)).multiply(r_0).divide(r_1).get(1).divide(beta).get(1));
      r_1 = constant(gamma.pow(d + 1)).multiply(r_0).divide(r_1).get(1).divide(beta).get(0);
      r_0 = temp;

      System.out.println(r_1);
    }

    return r_0;
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
