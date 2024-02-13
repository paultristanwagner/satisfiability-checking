package me.paultristanwagner.satchecking.theory.nonlinear;

import me.paultristanwagner.satchecking.smt.VariableAssignment;
import org.checkerframework.checker.units.qual.A;

import java.util.*;

public class CAD {

  private List<String> variables;
  private Set<MultivariatePolynomial> polynomials;

  public Set<VariableAssignment<RealAlgebraicNumber>> compute(Set<MultivariatePolynomial> polynomials) {
    Set<String> variablesSet = new HashSet<>();
    for (MultivariatePolynomial polynomial : polynomials) {
      variablesSet.addAll(polynomial.variables);
    }
    this.variables = new ArrayList<>(variablesSet);
    System.out.println(this.variables);

    // phase 1: projection
    Map<Integer, Set<MultivariatePolynomial>> p = new HashMap<>();
    p.put(variables.size(), polynomials);

    for (int r = variables.size() - 1; r >= 1; r--) {
      String variable = variables.get(r - 1);
      System.out.println(p.get(r + 1));
      Set<MultivariatePolynomial> proj = mcCallumProjection(p.get(r + 1), variable);
      p.put(r, proj);

      String previousVariable = variables.get(r);
      p.get(r + 1).stream().filter(poly -> !poly.highestVariable().equals(previousVariable));
    }
    System.out.println(p);

    // phase 2: lifting
    // todo: use proper intervals
    List<List<List<List<RealAlgebraicNumber>>>> D = new ArrayList<>();
    D.add(new ArrayList<>());

    for (int i = 1; i < variables.size(); i++) {
      List<List<RealAlgebraicNumber>> D_i = new ArrayList<>();

      for (List<List<RealAlgebraicNumber>> R : D.get(i - 1)) {
        // todo: choose s in setOf(R)
      }
    }

    return null;
  }

  public Set<MultivariatePolynomial> mcCallumProjection(Set<MultivariatePolynomial> polynomials, String variable) {
    Set<MultivariatePolynomial> result = new HashSet<>();

    for (MultivariatePolynomial p : polynomials) {
      for (MultivariatePolynomial q : polynomials) {
        if(p == q) {
          continue;
        }

        result.add(p.resultant(q, variable));
      }
    }

    for (MultivariatePolynomial polynomial : polynomials) {
      MultivariatePolynomial disc = polynomial.discriminant(variable);
      if(disc.isConstant()) {
        continue;
      }

      result.add(disc);
    }

    for (MultivariatePolynomial polynomial : polynomials) {
      List<MultivariatePolynomial> coefficients = polynomial.getCoefficients(variable);

      for (MultivariatePolynomial coefficient : coefficients) {
        if(coefficient.isConstant()) {
          continue;
        }

        result.add(coefficient);
      }
    }

    return result;
  }
}
