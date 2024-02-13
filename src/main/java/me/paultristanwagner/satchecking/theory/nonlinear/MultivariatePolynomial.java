package me.paultristanwagner.satchecking.theory.nonlinear;

import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import java.util.*;
import java.util.stream.Collectors;

import static me.paultristanwagner.satchecking.theory.nonlinear.Matrix.matrix;

public class MultivariatePolynomial {

  public static void main(String[] args) {
    List<String> pVariables = List.of("x", "y");
    Map<List<Integer>, Number> pCoefficients = new HashMap<>(Map.of(
        List.of(0, 0), Number.ONE(),
        List.of(1, 0), Number.parse("2"),
        List.of(0, 1), Number.parse("3"),
        List.of(1, 1), Number.parse("4")
    ));

    MultivariatePolynomial p = multivariatePolynomial(pCoefficients, pVariables);
    System.out.println("p = " + p);

    List<String> qVariables = List.of("x", "y");
    Map<List<Integer>, Number> qCoefficients = new HashMap<>(Map.of(
        List.of(0, 2), Number.parse("2"),
        List.of(1, 0), Number.parse("3")
    ));

    MultivariatePolynomial q = multivariatePolynomial(qCoefficients, qVariables);

    System.out.println("q = " + q);

    System.out.println("Res[p, q, y]");
    System.out.println(p.resultant(q, "y"));

    System.out.println("Disc[q, y]");
    System.out.println(q.discriminant("y"));

    CAD cad = new CAD();
    cad.compute(Set.of(p, q));
  }

  public Map<List<Integer>, Number> coefficients;
  public List<String> variables;

  private MultivariatePolynomial(Map<List<Integer>, Number> coefficients, List<String> variables) {
    this.coefficients = coefficients;
    this.variables = variables;
  }

  public static MultivariatePolynomial multivariatePolynomial(Map<List<Integer>, Number> coefficients, List<String> variables) {
    return new MultivariatePolynomial(coefficients, variables);
  }

  public static MultivariatePolynomial ZERO() {
    return multivariatePolynomial(new HashMap<>(), new ArrayList<>());
  }

  public String highestVariable() {
    int highestVariableIndex = 0;
    for (List<Integer> exponents : coefficients.keySet()) {
      for (int i = 0; i < exponents.size(); i++) {
        if(exponents.get(i) != 0 && i > highestVariableIndex) {
          highestVariableIndex = i;
        }
      }
    }

    return variables.get(highestVariableIndex);
  }

  public boolean isZero() {
    for (List<Integer> exponents : coefficients.keySet()) {
      if (!coefficients.get(exponents).isZero()) {
        return false;
      }
    }

    return true;
  }

  public boolean isConstant() {
    for (List<Integer> exponents : coefficients.keySet()) {
      if (!coefficients.get(exponents).isZero() && !isConstantExponent(exponents)) {
        return false;
      }
    }

    return true;
  }

  private List<Integer> mapExponents(List<Integer> exponents, List<String> newVariables) {
    int[] newExponentsArray = new int[newVariables.size()];
    for (int i = 0; i < exponents.size(); i++) {
      int exponent = exponents.get(i);
      String variable = this.variables.get(i);
      int variableIndex = newVariables.indexOf(variable); // todo: inefficient

      newExponentsArray[variableIndex] = exponent;
    }

    List<Integer> newExponents = new ArrayList<>();
    for (int newExponent : newExponentsArray) {
      newExponents.add(newExponent);
    }

    return newExponents;
  }

  public MultivariatePolynomial add(MultivariatePolynomial other) {
    Set<String> variablesSet = new HashSet<>(this.variables);
    variablesSet.addAll(other.variables);

    List<String> variables = new ArrayList<>(variablesSet);

    Map<List<Integer>, Number> coefficients = new HashMap<>();

    for (List<Integer> exponents : this.coefficients.keySet()) {
      List<Integer> thisNewExponents = this.mapExponents(exponents, variables);

      Number c = coefficients.getOrDefault(thisNewExponents, Number.ZERO());
      c = c.add(this.coefficients.getOrDefault(exponents, Number.ZERO()));
      coefficients.put(thisNewExponents, c);
    }

    for (List<Integer> exponents : other.coefficients.keySet()) {
      List<Integer> otherNewExponents = other.mapExponents(exponents, variables);
      Number c = coefficients.getOrDefault(otherNewExponents, Number.ZERO());
      c = c.add(other.coefficients.getOrDefault(exponents, Number.ZERO()));
      coefficients.put(otherNewExponents, c);
    }

    return multivariatePolynomial(coefficients, variables);
  }

  public MultivariatePolynomial negate() {
    List<String> variables = new ArrayList<>(this.variables);
    Map<List<Integer>, Number> coefficients = new HashMap<>();
    for (List<Integer> exponents : this.coefficients.keySet()) {
      coefficients.put(new ArrayList<>(exponents), this.coefficients.get(exponents).negate());
    }

    return multivariatePolynomial(coefficients, variables);
  }

  public MultivariatePolynomial subtract(MultivariatePolynomial other) {
    return this.add(other.negate());
  }

  public MultivariatePolynomial multiply(MultivariatePolynomial other) {
    Set<String> variablesSet = new HashSet<>(this.variables);
    variablesSet.addAll(other.variables);

    List<String> variables = new ArrayList<>(variablesSet);

    Map<List<Integer>, Number> coefficients = new HashMap<>();

    for (List<Integer> exponents : this.coefficients.keySet()) {
      for (List<Integer> otherExponents : other.coefficients.keySet()) {
        int[] newExponentsArray = new int[variables.size()];

        for (int i = 0; i < exponents.size(); i++) {
          Integer exponent = exponents.get(i);
          int variableIndex = variables.indexOf(this.variables.get(i)); // todo: inefficient
          newExponentsArray[variableIndex] += exponent;
        }

        for (int i = 0; i < otherExponents.size(); i++) {
          Integer exponent = otherExponents.get(i);
          int variableIndex = variables.indexOf(other.variables.get(i)); // todo: inefficient
          newExponentsArray[variableIndex] += exponent;
        }

        List<Integer> newExponents = new ArrayList<>();
        for (int newExponent : newExponentsArray) {
          newExponents.add(newExponent);
        }

        Number c = coefficients.getOrDefault(newExponents, Number.ZERO());
        c = c.add(
            this.coefficients.get(exponents).multiply(other.coefficients.get(otherExponents))
        );

        coefficients.put(newExponents, c);
      }
    }

    return multivariatePolynomial(coefficients, variables);
  }

  public List<MultivariatePolynomial> getCoefficients(String variable) {
    int variableIndex = this.variables.indexOf(variable);
    int highestExponent = 0;

    List<String> newVariables = new ArrayList<>(variables);
    newVariables.remove(variable);

    for (List<Integer> exponents : this.coefficients.keySet()) {
      int exponent = exponents.get(variableIndex);
      highestExponent = Math.max(highestExponent, exponent);
    }

    MultivariatePolynomial[] coefficientsArray = new MultivariatePolynomial[highestExponent + 1];
    Arrays.fill(coefficientsArray, ZERO());
    for (List<Integer> exponents : this.coefficients.keySet()) {
      int variableExponent = exponents.get(variableIndex);

      Number c = this.coefficients.get(exponents);
      List<Integer> newExponents = new ArrayList<>();
      for (int i = 0; i < exponents.size(); i++) {
        if (i == variableIndex) {
          continue;
        }

        Integer exponent = exponents.get(i);
        newExponents.add(exponent);
      }

      Map<List<Integer>, Number> monomialCoefficients = new HashMap<>(Map.of(
          newExponents, c
      ));
      MultivariatePolynomial monomial = multivariatePolynomial(monomialCoefficients, newVariables);

      coefficientsArray[variableExponent] = coefficientsArray[variableExponent].add(monomial);
    }

    return Arrays.stream(coefficientsArray).collect(Collectors.toList());
  }

  public MultivariatePolynomial derivative(String variable) {
    int variableIndex = this.variables.indexOf(variable);

    List<String> newVariables = new ArrayList<>(this.variables);
    Map<List<Integer>, Number> coefficients = new HashMap<>();
    for (List<Integer> exponents : this.coefficients.keySet()) {
      if(exponents.get(variableIndex) == 0) {
        continue;
      }

      List<Integer> newExponents = new ArrayList<>(exponents);
      newExponents.set(variableIndex, exponents.get(variableIndex) - 1);

      coefficients.put(newExponents, this.coefficients.get(exponents));
    }

    return multivariatePolynomial(coefficients, newVariables);
  }

  public MultivariatePolynomial resultant(MultivariatePolynomial other, String variable) {
    List<String> newVariables = new ArrayList<>(variables);
    newVariables.remove(variable);

    List<MultivariatePolynomial> thisCoefficients = this.getCoefficients(variable);
    List<MultivariatePolynomial> otherCoefficients = other.getCoefficients(variable);

    Map<List<Integer>, MultivariatePolynomial> entries = new HashMap<>();

    for (int i = 0; i < otherCoefficients.size() - 1; i++) {
      for (int j = 0; j < thisCoefficients.size(); j++) {
        entries.put(List.of(i, i + j), thisCoefficients.get(j));
      }
    }

    for (int i = 0; i < thisCoefficients.size() - 1; i++) {
      for (int j = 0; j < otherCoefficients.size(); j++) {
        entries.put(List.of(otherCoefficients.size() + i - 1, i + j), otherCoefficients.get(j));
      }
    }

    int n = thisCoefficients.size() + otherCoefficients.size() - 2;
    Matrix sylvesterMatrix = matrix(newVariables, n, n, entries);

    return sylvesterMatrix.determinant();
  }

  public MultivariatePolynomial discriminant(String variable) {
    MultivariatePolynomial derivative = this.derivative(variable);

    return this.resultant(derivative, variable);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Iterator<List<Integer>> iterator = coefficients.keySet().iterator(); iterator.hasNext(); ) {
      List<Integer> exponents = iterator.next();
      Number coefficient = coefficients.get(exponents);

      if (coefficient.isZero()) {
        continue;
      }

      sb.append(coefficient);

      if (!isConstantExponent(exponents)) {
        sb.append(" * ");
      }

      for (int i = 0; i < exponents.size(); i++) {
        if (exponents.get(i) == 0) {
          continue;
        }

        sb.append(variables.get(i));
        if (exponents.get(i) > 1) {
          sb.append("^").append(exponents.get(i));
        }

        if (i < exponents.size() - 1 && exponents.get(i + 1) != 0) {
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

  private boolean isConstantExponent(List<Integer> exponent) {
    for (int e : exponent) {
      if (e != 0) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MultivariatePolynomial that = (MultivariatePolynomial) o;
    return Objects.equals(coefficients, that.coefficients) && Objects.equals(variables, that.variables);
  }

  @Override
  public int hashCode() {
    return Objects.hash(coefficients, variables);
  }
}
