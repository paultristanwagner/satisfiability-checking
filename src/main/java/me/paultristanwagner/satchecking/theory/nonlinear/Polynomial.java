package me.paultristanwagner.satchecking.theory.nonlinear;

import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.paultristanwagner.satchecking.theory.arithmetic.Number.*;

public class Polynomial {

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
