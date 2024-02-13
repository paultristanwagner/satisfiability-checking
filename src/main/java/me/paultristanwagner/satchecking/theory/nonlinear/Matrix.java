package me.paultristanwagner.satchecking.theory.nonlinear;

import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomial.ZERO;
import static me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomial.multivariatePolynomial;

public class Matrix {

  public static void main(String[] args) {
    List<String> variables = new ArrayList<>();
    int n = 3;
    Map<List<Integer>, MultivariatePolynomial> entries = new HashMap<>(Map.of(
        List.of(0, 1), multivariatePolynomial(Map.of(List.of(), Number.number(1)), variables),
        List.of(0, 2), multivariatePolynomial(Map.of(List.of(), Number.number(2)), variables),
        List.of(1, 0), multivariatePolynomial(Map.of(List.of(), Number.number(3)), variables),
        List.of(1, 1), multivariatePolynomial(Map.of(List.of(), Number.number(2)), variables),
        List.of(1, 2), multivariatePolynomial(Map.of(List.of(), Number.number(1)), variables),
        List.of(2, 0), multivariatePolynomial(Map.of(List.of(), Number.number(1)), variables),
        List.of(2, 1), multivariatePolynomial(Map.of(List.of(), Number.number(1)), variables)
    ));
    Matrix matrix = matrix(variables, n, n, entries);
    System.out.println(matrix.determinant());
  }

  private final List<String> variables;
  private final int m;
  private final int n;
  private final Map<List<Integer>, MultivariatePolynomial> entries;

  private Matrix(List<String> variables, int m, int n, Map<List<Integer>, MultivariatePolynomial> entries) {
    this.variables = variables;
    this.m = m;
    this.n = n;
    this.entries = entries;
  }

  public static Matrix matrix(List<String> variables, int m, int n, Map<List<Integer>, MultivariatePolynomial> entries) {
    return new Matrix(variables, m, n, entries);
  }

  public boolean isSquare() {
    return m == n;
  }

  public MultivariatePolynomial minor(int i, int j) {
    if (!isSquare()) {
      throw new IllegalStateException("Cannot compute minor of non-square matrix");
    }

    List<String> variables = new ArrayList<>(this.variables);
    int n = this.n - 1;
    Map<List<Integer>, MultivariatePolynomial> entries = new HashMap<>();

    for (List<Integer> index : this.entries.keySet()) {
      if(index.get(0) == i || index.get(1) == j) {
        continue;
      }

      List<Integer> newIndex = new ArrayList<>(List.of(
          index.get(0) < i ? index.get(0) : index.get(0) - 1,
          index.get(1) < j ? index.get(1) : index.get(1) - 1
      ));

      entries.put(newIndex, this.entries.get(index));
    }

    Matrix subMatrix = matrix(variables, n, n, entries);
    return subMatrix.determinant();
  }

  public MultivariatePolynomial determinant() {
    if (!isSquare()) {
      throw new IllegalStateException("Cannot compute determinant of non-square matrix");
    }

    if (n == 1) {
      return entries.values().stream().findAny().orElseGet(MultivariatePolynomial::ZERO);
    }

    if (n == 2) {
      MultivariatePolynomial a = entries.getOrDefault(List.of(0, 0), ZERO());
      MultivariatePolynomial b = entries.getOrDefault(List.of(0, 1), ZERO());
      MultivariatePolynomial c = entries.getOrDefault(List.of(1, 0), ZERO());
      MultivariatePolynomial d = entries.getOrDefault(List.of(1, 1), ZERO());

      MultivariatePolynomial ad = a.multiply(d);
      MultivariatePolynomial bc = b.multiply(c);
      return ad.subtract(bc);
    }

    // Laplace's expansion along the 0-th column
    int j = 0;
    MultivariatePolynomial result = ZERO();
    for (int i = 0; i < n; i++) {
      MultivariatePolynomial b = entries.getOrDefault(List.of(i, j), ZERO());
      if (b.isZero()) {
        continue;
      }

      MultivariatePolynomial minor = minor(i, j);
      boolean positive = (i + j) % 2 == 0;

      MultivariatePolynomial bm = b.multiply(minor);

      if (positive) {
        result = result.add(bm);
      } else {
        result = result.subtract(bm);
      }
    }

    return result;
  }

  // todo: add toString method
}
