package me.paultristanwagner.satchecking.theory.nonlinear;

import me.paultristanwagner.satchecking.theory.Constraint;

public class MultivariatePolynomialConstraint implements Constraint {

  public enum Comparison {
    EQUALS,
    NOT_EQUALS,
    LESS_THAN,
    GREATER_THAN,
    LESS_THAN_OR_EQUALS,
    GREATER_THAN_OR_EQUALS;

    public boolean evaluateSign(int sign) {
      return switch (this) {
        case EQUALS -> sign == 0;
        case NOT_EQUALS -> sign != 0;
        case LESS_THAN -> sign < 0;
        case GREATER_THAN -> sign > 0;
        case LESS_THAN_OR_EQUALS -> sign <= 0;
        case GREATER_THAN_OR_EQUALS -> sign >= 0;
      };
    }

    public String toString() {
      return switch (this) {
        case EQUALS -> "=";
        case NOT_EQUALS -> "!=";
        case LESS_THAN -> "<";
        case GREATER_THAN -> ">";
        case LESS_THAN_OR_EQUALS -> "<=";
        case GREATER_THAN_OR_EQUALS -> ">=";
      };
    }
  }

  private final MultivariatePolynomial polynomial;
  private final Comparison comparison;

  private MultivariatePolynomialConstraint(MultivariatePolynomial polynomial, Comparison comparison) {
    this.polynomial = polynomial;
    this.comparison = comparison;
  }

  public static MultivariatePolynomialConstraint multivariatePolynomialConstraint(MultivariatePolynomial polynomial, Comparison comparison) {
    return new MultivariatePolynomialConstraint(polynomial, comparison);
  }

  public static MultivariatePolynomialConstraint lessThanZero(MultivariatePolynomial polynomial) {
    return new MultivariatePolynomialConstraint(polynomial, Comparison.LESS_THAN);
  }

  public static MultivariatePolynomialConstraint greaterThanZero(MultivariatePolynomial polynomial) {
    return new MultivariatePolynomialConstraint(polynomial, Comparison.GREATER_THAN);
  }

  public static MultivariatePolynomialConstraint lessThanOrEqualsZero(MultivariatePolynomial polynomial) {
    return new MultivariatePolynomialConstraint(polynomial, Comparison.LESS_THAN_OR_EQUALS);
  }

  public static MultivariatePolynomialConstraint greaterThanOrEqualsZero(MultivariatePolynomial polynomial) {
    return new MultivariatePolynomialConstraint(polynomial, Comparison.GREATER_THAN_OR_EQUALS);
  }

  public static MultivariatePolynomialConstraint equalsZero(MultivariatePolynomial polynomial) {
    return new MultivariatePolynomialConstraint(polynomial, Comparison.EQUALS);
  }

  public static MultivariatePolynomialConstraint notEqualsZero(MultivariatePolynomial polynomial) {
    return new MultivariatePolynomialConstraint(polynomial, Comparison.NOT_EQUALS);
  }

  public static MultivariatePolynomialConstraint equals(MultivariatePolynomial polynomial, MultivariatePolynomial other) {
    return equalsZero(polynomial.subtract(other));
  }

  public static MultivariatePolynomialConstraint notEquals(MultivariatePolynomial polynomial, MultivariatePolynomial other) {
    return notEqualsZero(polynomial.subtract(other));
  }

  public static MultivariatePolynomialConstraint lessThan(MultivariatePolynomial polynomial, MultivariatePolynomial other) {
    return lessThanZero(polynomial.subtract(other));
  }

  public static MultivariatePolynomialConstraint greaterThan(MultivariatePolynomial polynomial, MultivariatePolynomial other) {
    return greaterThanZero(polynomial.subtract(other));
  }

  public static MultivariatePolynomialConstraint lessThanOrEquals(MultivariatePolynomial polynomial, MultivariatePolynomial other) {
    return lessThanOrEqualsZero(polynomial.subtract(other));
  }

  public static MultivariatePolynomialConstraint greaterThanOrEquals(MultivariatePolynomial polynomial, MultivariatePolynomial other) {
    return greaterThanOrEqualsZero(polynomial.subtract(other));
  }

  public MultivariatePolynomial getPolynomial() {
    return polynomial;
  }

  public Comparison getComparison() {
    return comparison;
  }
}
