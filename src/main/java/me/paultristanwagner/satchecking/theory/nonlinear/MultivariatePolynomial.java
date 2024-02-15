package me.paultristanwagner.satchecking.theory.nonlinear;

import static me.paultristanwagner.satchecking.theory.arithmetic.Number.ONE;
import static me.paultristanwagner.satchecking.theory.nonlinear.Exponent.exponent;
import static me.paultristanwagner.satchecking.theory.nonlinear.Matrix.matrix;
import static me.paultristanwagner.satchecking.theory.nonlinear.Polynomial.polynomial;

import java.util.*;
import java.util.stream.Collectors;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;

public class MultivariatePolynomial {

  public Map<Exponent, Number> coefficients;
  public List<String> variables;

  private MultivariatePolynomial(Map<Exponent, Number> coefficients, List<String> variables) {
    this.coefficients = coefficients;
    this.variables = variables;
  }

  public static MultivariatePolynomial multivariatePolynomial(
      Map<Exponent, Number> coefficients, List<String> variables) {
    return new MultivariatePolynomial(coefficients, variables);
  }

  public static MultivariatePolynomial constant(Number number) {
    return multivariatePolynomial(Map.of(exponent(), number), new ArrayList<>());
  }

  public static MultivariatePolynomial variable(String variable) {
    return multivariatePolynomial(Map.of(exponent(1), ONE()), List.of(variable));
  }

  public static MultivariatePolynomial ZERO() {
    return multivariatePolynomial(new HashMap<>(), new ArrayList<>());
  }

  public String highestVariable() {
    int highestVariableIndex = 0;
    for (Exponent exponent : coefficients.keySet()) {
      highestVariableIndex = exponent.highestNonZeroIndex();
    }

    return variables.get(highestVariableIndex);
  }

  public int highestExponent(String variable) {
    int variableIndex = this.variables.indexOf(variable);
    int highestExponent = 0;

    if (variableIndex == -1) {
      return 0;
    }

    for (Exponent exponent : this.coefficients.keySet()) {
      int value = exponent.get(variableIndex);
      highestExponent = Math.max(highestExponent, value);
    }

    return highestExponent;
  }

  public boolean isZero() {
    for (Exponent exponent : coefficients.keySet()) {
      if (!coefficients.get(exponent).isZero()) {
        return false;
      }
    }

    return true;
  }

  public boolean isConstant() {
    for (Exponent exponent : coefficients.keySet()) {
      if (!coefficients.get(exponent).isZero() && !exponent.isConstantExponent()) {
        return false;
      }
    }

    return true;
  }

  public MultivariatePolynomial add(MultivariatePolynomial other) {
    Set<String> variablesSet = new HashSet<>(this.variables);
    variablesSet.addAll(other.variables);
    List<String> newVariables = new ArrayList<>(variablesSet);
    Map<Exponent, Number> newCoefficients = new HashMap<>();

    for (Exponent exponent : this.coefficients.keySet()) {
      Exponent thisNewExponent = Exponent.project(exponent, this.variables, newVariables);

      Number c = newCoefficients.getOrDefault(thisNewExponent, Number.ZERO());
      c = c.add(this.coefficients.getOrDefault(exponent, Number.ZERO()));
      newCoefficients.put(thisNewExponent, c);
    }

    for (Exponent exponent : other.coefficients.keySet()) {
      Exponent otherNewExponent = Exponent.project(exponent, other.variables, newVariables);
      Number c = newCoefficients.getOrDefault(otherNewExponent, Number.ZERO());
      c = c.add(other.coefficients.getOrDefault(exponent, Number.ZERO()));
      newCoefficients.put(otherNewExponent, c);
    }

    return multivariatePolynomial(newCoefficients, newVariables);
  }

  public MultivariatePolynomial negate() {
    List<String> variables = new ArrayList<>(this.variables);
    Map<Exponent, Number> coefficients = new HashMap<>();
    for (Exponent exponent : this.coefficients.keySet()) {
      coefficients.put(exponent, this.coefficients.get(exponent).negate());
    }

    return multivariatePolynomial(coefficients, variables);
  }

  public MultivariatePolynomial subtract(MultivariatePolynomial other) {
    return this.add(other.negate());
  }

  public MultivariatePolynomial multiply(MultivariatePolynomial other) {
    Set<String> variablesSet = new HashSet<>(this.variables);
    variablesSet.addAll(other.variables);
    List<String> newVariables = new ArrayList<>(variablesSet);
    Map<Exponent, Number> newCoefficients = new HashMap<>();

    for (Exponent exponent : this.coefficients.keySet()) {
      Exponent projectedExponent = Exponent.project(exponent, this.variables, newVariables);
      for (Exponent otherExponent : other.coefficients.keySet()) {
        Exponent projectedOtherExponent = Exponent.project(otherExponent, other.variables, newVariables);
        Exponent newExponent = projectedExponent.add(projectedOtherExponent);

        Number c = newCoefficients.getOrDefault(newExponent, Number.ZERO());
        c = c.add(this.coefficients.get(exponent).multiply(other.coefficients.get(otherExponent)));

        newCoefficients.put(newExponent, c);
      }
    }

    return multivariatePolynomial(newCoefficients, newVariables);
  }

  // todo: use fast exponentiation
  public MultivariatePolynomial pow(int exponent) {
    if (exponent < 0) {
      throw new IllegalArgumentException("Exponent must be non-negative");
    }

    if (exponent == 0) {
      return constant(ONE());
    }

    MultivariatePolynomial result = this;
    for (int i = 1; i < exponent; i++) {
      result = result.multiply(this);
    }

    return result;
  }

  public Exponent getLeadMonomial() {
    Exponent leadMonomial = null;

    for (Exponent exponent : coefficients.keySet()) {
      if (leadMonomial == null || leadMonomial.compareTo(exponent) < 0) {
        leadMonomial = exponent;
      }
    }

    return leadMonomial;
  }

  public List<MultivariatePolynomial> pseudoDivision(MultivariatePolynomial divisor) {
    String variable = divisor.highestVariable();

    List<MultivariatePolynomial> divisorCoefficients = divisor.getCoefficients(variable);
    int divisorDegree = divisorCoefficients.size() - 1;

    MultivariatePolynomial remainder = this;
    MultivariatePolynomial result = ZERO();

    while (true) {
      // get lead monomial in divisor
      // rem := rem - (lc(remainder) * lm(remainder) / (lc(divisor) * lm(divisor))
    }
  }

  public List<MultivariatePolynomial> getCoefficients(String variable) {
    List<String> newVariables = new ArrayList<>(variables);
    newVariables.remove(variable);

    int highestExponent = highestExponent(variable);

    MultivariatePolynomial[] coefficientsArray = new MultivariatePolynomial[highestExponent + 1];
    Arrays.fill(coefficientsArray, ZERO());

    int variableIndex = variables.indexOf(variable);
    for (Exponent exponent : this.coefficients.keySet()) {
      int variableExponent = variableIndex != -1 ? exponent.get(variableIndex) : 0;

      Number c = this.coefficients.get(exponent);
      Exponent newExponent = Exponent.project(exponent, this.variables, newVariables);

      Map<Exponent, Number> monomialCoefficients = new HashMap<>(Map.of(newExponent, c));
      MultivariatePolynomial monomial = multivariatePolynomial(monomialCoefficients, newVariables);

      coefficientsArray[variableExponent] = coefficientsArray[variableExponent].add(monomial);
    }

    return Arrays.stream(coefficientsArray).collect(Collectors.toList());
  }

  public MultivariatePolynomial derivative(String variable) {
    int variableIndex = this.variables.indexOf(variable);

    if (variableIndex == -1) {
      return ZERO();
    }

    List<String> newVariables = new ArrayList<>(this.variables);
    Map<Exponent, Number> coefficients = new HashMap<>();
    for (Exponent exponent : this.coefficients.keySet()) {
      if (exponent.get(variableIndex) == 0) {
        continue;
      }

      List<Integer> exponentValues = new ArrayList<>(exponent.getValues());
      exponentValues.set(variableIndex, exponent.get(variableIndex) - 1);
      Exponent newExponent = exponent(exponentValues);

      coefficients.put(newExponent, this.coefficients.get(exponent));
    }

    return multivariatePolynomial(coefficients, newVariables);
  }

  public MultivariatePolynomial resultant(MultivariatePolynomial other, String variable) {
    System.out.println(
        "computing resultant of " + this + " and " + other + " with respect to " + variable);
    List<String> newVariables = new ArrayList<>(variables);
    newVariables.remove(variable);

    List<MultivariatePolynomial> thisCoefficients = this.getCoefficients(variable);
    List<MultivariatePolynomial> otherCoefficients = other.getCoefficients(variable);

    Map<List<Integer>, MultivariatePolynomial> entries = new HashMap<>();

    for (int i = 0; i < otherCoefficients.size() - 1; i++) {
      for (int j = 0; j < thisCoefficients.size(); j++) {
        if (thisCoefficients.get(j).isZero()) {
          continue;
        }

        entries.put(List.of(i, i + j), thisCoefficients.get(j));
      }
    }

    for (int i = 0; i < thisCoefficients.size() - 1; i++) {
      for (int j = 0; j < otherCoefficients.size(); j++) {
        if (otherCoefficients.get(j).isZero()) {
          continue;
        }

        entries.put(List.of(otherCoefficients.size() + i - 1, i + j), otherCoefficients.get(j));
      }
    }

    System.out.println(entries);

    int n = thisCoefficients.size() + otherCoefficients.size() - 2;
    Matrix sylvesterMatrix = matrix(newVariables, n, n, entries);

    return sylvesterMatrix.determinant();
  }

  public MultivariatePolynomial discriminant(String variable) {
    MultivariatePolynomial derivative = this.derivative(variable);

    return this.resultant(derivative, variable);
  }

  public MultivariatePolynomial substitute(Map<String, RealAlgebraicNumber> substitution) {
    MultivariatePolynomial current = this;
    for (String variable : substitution.keySet()) {
      RealAlgebraicNumber value = substitution.get(variable);
      int variableIndex = current.variables.indexOf(variable);

      if (variableIndex == -1) {
        continue;
      }

      if (!value.isNumeric()) {
        Polynomial ranPolynomial = value.getPolynomial();
        current = current.resultant(ranPolynomial.toMultivariatePolynomial(variable), variable);
        continue;
      }

      Number rationalValue = value.numericValue();

      List<String> newVariables = new ArrayList<>(current.variables);
      newVariables.remove(variable);

      Map<Exponent, Number> newCoefficients = new HashMap<>();

      for (Exponent exponent : current.coefficients.keySet()) {
        int power = exponent.get(variableIndex);

        Number c = current.coefficients.get(exponent);
        Exponent newExponent = Exponent.project(exponent, current.variables, newVariables);

        if (power != 0) {
          c = c.multiply(rationalValue.pow(power));
        }

        Number prev = newCoefficients.getOrDefault(newExponent, Number.ZERO());
        newCoefficients.put(newExponent, prev.add(c));
      }

      current = multivariatePolynomial(newCoefficients, newVariables);
    }

    return current;
  }

  public Polynomial toUnivariatePolynomial() {
    if (this.variables.size() > 1) {
      throw new IllegalArgumentException("Not a univariate polynomial");
    }

    if (this.variables.isEmpty()) {
      return polynomial(coefficients.getOrDefault(List.of(), Number.ZERO()));
    }

    int degree = 0;
    for (Exponent exponent : coefficients.keySet()) {
      degree = Math.max(degree, exponent.get(0));
    }

    Number[] coefficientsArray = new Number[degree + 1];
    for (int i = 0; i <= degree; i++) {
      Exponent exponent = exponent(i);
      coefficientsArray[i] = coefficients.getOrDefault(exponent, Number.ZERO());
    }

    return polynomial(coefficientsArray);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Iterator<Exponent> iterator = coefficients.keySet().iterator();
        iterator.hasNext(); ) {
      Exponent exponent = iterator.next();
      Number coefficient = coefficients.get(exponent);

      if (coefficient.isZero()) {
        continue;
      }

      sb.append(coefficient);

      if (!exponent.isConstantExponent()) {
        sb.append(" * ");
      }

      for (int i = 0; i < variables.size(); i++) {
        if (exponent.get(i) == 0) {
          continue;
        }

        sb.append(variables.get(i));
        if (exponent.get(i) > 1) {
          sb.append("^").append(exponent.get(i));
        }

        if (i < variables.size() - 1 && exponent.get(i + 1) != 0) {
          sb.append(" * ");
        }
      }

      if (iterator.hasNext()) {
        sb.append(" + ");
      }
    }

    if (sb.isEmpty()) {
      return "0";
    }

    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MultivariatePolynomial that = (MultivariatePolynomial) o;
    return Objects.equals(coefficients, that.coefficients)
        && Objects.equals(variables, that.variables);
  }

  @Override
  public int hashCode() {
    return Objects.hash(coefficients, variables);
  }
}
