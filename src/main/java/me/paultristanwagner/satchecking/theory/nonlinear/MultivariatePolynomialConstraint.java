package me.paultristanwagner.satchecking.theory.nonlinear;

import me.paultristanwagner.satchecking.theory.Constraint;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

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

  /**
   * The negation of a polynomial constraint over the reals is exact: it keeps the SAME polynomial and
   * flips the comparison ({@code p < 0} negates to {@code p >= 0}, etc.). This is used by the lazy SMT
   * loop to obtain the constraint for a literal asserted false, so it MUST be sound.
   */
  @Override
  public boolean isNegatable() {
    return true;
  }

  @Override
  public Constraint negate() {
    Comparison negated =
        switch (comparison) {
          case EQUALS -> Comparison.NOT_EQUALS;
          case NOT_EQUALS -> Comparison.EQUALS;
          case LESS_THAN -> Comparison.GREATER_THAN_OR_EQUALS;
          case GREATER_THAN_OR_EQUALS -> Comparison.LESS_THAN;
          case GREATER_THAN -> Comparison.LESS_THAN_OR_EQUALS;
          case LESS_THAN_OR_EQUALS -> Comparison.GREATER_THAN;
        };
    return new MultivariatePolynomialConstraint(polynomial, negated);
  }

  /**
   * A stable, variable-order-independent canonical form of the polynomial used for {@link #equals}
   * and {@link #hashCode}. We deliberately do NOT rely on {@link MultivariatePolynomial#equals}/{@link
   * MultivariatePolynomial#hashCode}: those call a {@code prune()} that mutates the polynomial's
   * fields, which would make this constraint unsafe as a hash-map / {@code BiMap} key. Instead we
   * derive the canonical form directly from the (immutable view of the) coefficient map, keying each
   * monomial by VARIABLE NAME rather than by positional exponent so that two polynomials built with
   * different internal variable orderings still compare equal.
   *
   * <p>Zero coefficients are dropped, so {@code x - x} and {@code 0} share a canonical form.
   */
  private String canonicalPolynomial() {
    if (polynomial == null) {
      return "null"; // optimization constraints (no comparison); never used as an atom key.
    }

    List<String> variables = polynomial.variables;
    // Map each monomial to a sorted "var^exp*..." key -> accumulated coefficient (Number, not String,
    // so we never round-trip through a possibly-lossy textual form), dropping zero terms.
    TreeMap<String, Number> monomials = new TreeMap<>();
    for (Map.Entry<Exponent, Number> entry : polynomial.coefficients.entrySet()) {
      Number coefficient = entry.getValue();
      if (coefficient == null || coefficient.isZero()) {
        continue;
      }
      Exponent exponent = entry.getKey();
      List<Integer> powers = exponent.getValues();
      TreeMap<String, Integer> byName = new TreeMap<>();
      for (int i = 0; i < variables.size() && i < powers.size(); i++) {
        int power = powers.get(i);
        if (power != 0) {
          byName.merge(variables.get(i), power, Integer::sum);
        }
      }
      StringBuilder key = new StringBuilder();
      for (Map.Entry<String, Integer> ve : byName.entrySet()) {
        if (key.length() > 0) {
          key.append('*');
        }
        key.append(ve.getKey()).append('^').append(ve.getValue());
      }
      // Identical monomials (after name-keying) accumulate their coefficients.
      monomials.merge(key.toString(), coefficient, Number::add);
    }

    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Number> m : monomials.entrySet()) {
      if (m.getValue().isZero()) {
        continue; // monomials that cancel out
      }
      sb.append('[').append(m.getValue()).append(']').append(m.getKey()).append(';');
    }
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MultivariatePolynomialConstraint that = (MultivariatePolynomialConstraint) o;
    return comparison == that.comparison
        && Objects.equals(canonicalPolynomial(), that.canonicalPolynomial());
  }

  @Override
  public int hashCode() {
    return Objects.hash(comparison, canonicalPolynomial());
  }

  @Override
  public String toString() {
    return polynomial + " " + comparison + " 0";
  }

  public static abstract class MultivariateOptimizationConstraint extends MultivariatePolynomialConstraint {

    protected final MultivariatePolynomial objective;

    private MultivariateOptimizationConstraint(MultivariatePolynomial objective) {
      super(objective, null);

      this.objective = objective;
    }

    public MultivariatePolynomial getObjective() {
      return objective;
    }
  }

  public static class MultivariateMinimizationConstraint extends MultivariateOptimizationConstraint {

    private MultivariateMinimizationConstraint(MultivariatePolynomial objective) {
      super(objective);
    }

    public static MultivariateMinimizationConstraint minimize(MultivariatePolynomial objective) {
      return new MultivariateMinimizationConstraint(objective);
    }

    @Override
    public String toString() {
      return "min(" + objective + ")";
    }
  }

  public static class MultivariateMaximizationConstraint extends MultivariateOptimizationConstraint {


    private MultivariateMaximizationConstraint(MultivariatePolynomial objective) {
      super(objective);
    }

    public static MultivariateMaximizationConstraint maximize(MultivariatePolynomial objective) {
      return new MultivariateMaximizationConstraint(objective);
    }

    @Override
    public String toString() {
      return "max(" + objective + ")";
    }
  }
}
