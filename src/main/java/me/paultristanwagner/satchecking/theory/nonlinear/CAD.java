package me.paultristanwagner.satchecking.theory.nonlinear;

import java.util.*;

import static me.paultristanwagner.satchecking.theory.nonlinear.Cell.emptyCell;
import static me.paultristanwagner.satchecking.theory.nonlinear.Interval.IntervalBoundType.OPEN;
import static me.paultristanwagner.satchecking.theory.nonlinear.Interval.*;

public class CAD {

  public Set<Cell> compute(
      Set<MultivariatePolynomialConstraint> constraints
  ) {
    return compute(constraints, null);
  }

  public Set<Cell> compute(
      Set<MultivariatePolynomialConstraint> constraints,
      List<String> variableOrdering
  ) {
    Set<MultivariatePolynomial> polynomials = new HashSet<>();
    for (MultivariatePolynomialConstraint constraint : constraints) {
      polynomials.add(constraint.getPolynomial());
    }

    if(variableOrdering == null) {
      Set<String> variablesSet = new HashSet<>();
      for (MultivariatePolynomial polynomial : polynomials) {
        variablesSet.addAll(polynomial.variables);
      }
      variableOrdering = new ArrayList<>(variablesSet);
    }

    // phase 1: projection
    Map<Integer, Set<MultivariatePolynomial>> p = new HashMap<>();
    p.put(variableOrdering.size(), polynomials);

    for (int r = variableOrdering.size() - 1; r >= 1; r--) {
      String variable = variableOrdering.get(r);

      Set<MultivariatePolynomial> proj = mcCallumProjection(p.get(r + 1), variable);
      p.put(r, proj);

      String previousVariable = variableOrdering.get(r);

      // todo: highestVariable could cause errors
      p.get(r + 1).removeIf(poly -> !poly.highestVariable().equals(previousVariable));
    }

    // phase 2: lifting
    List<List<Cell>> D = new ArrayList<>();
    D.add(List.of(emptyCell()));

    for (int i = 1; i <= variableOrdering.size(); i++) {
      List<Cell> D_i = new ArrayList<>();
      String variable = variableOrdering.get(i - 1);

      for (Cell R : D.get(i - 1)) {
        Map<String, RealAlgebraicNumber> s = R.chooseSamplePoint();

        Set<RealAlgebraicNumber> roots = new HashSet<>();
        for (MultivariatePolynomial polynomial : p.get(i)) {
          MultivariatePolynomial substituted = polynomial.substitute(s);
          Polynomial univariate = substituted.toUnivariatePolynomial();
          roots.addAll(univariate.isolateRoots());
        }

        // sort roots
        List<RealAlgebraicNumber> sortedRoots = new ArrayList<>(roots);
        sortedRoots.sort((a, b) -> a.equals(b) ? 0 : a.lessThan(b) ? -1 : 1); // todo: make comparable

        // remove duplicates
        Iterator<RealAlgebraicNumber> iterator = sortedRoots.iterator();
        RealAlgebraicNumber previous = null;
        while (iterator.hasNext()) {
          RealAlgebraicNumber current = iterator.next();
          if (previous != null && previous.equals(current)) {
            iterator.remove();
          }
          previous = current;
        }

        if (sortedRoots.isEmpty()) {
          D_i.add(R.extend(variable, unboundedInterval()));
        } else {
          D_i.add(R.extend(variable, intervalLowerUnbounded(sortedRoots.get(0).copy(), OPEN)));
          D_i.add(R.extend(variable, intervalUpperUnbounded(sortedRoots.get(sortedRoots.size() - 1).copy(), OPEN)));
          D_i.add(R.extend(variable, pointInterval(sortedRoots.get(sortedRoots.size() - 1).copy())));
        }

        for (int j = 0; j < sortedRoots.size() - 1; j++) {
          RealAlgebraicNumber a = sortedRoots.get(j);
          RealAlgebraicNumber b = sortedRoots.get(j + 1);

          D_i.add(R.extend(variable, pointInterval(a.copy())));
          D_i.add(R.extend(variable, interval(a.copy(), b.copy(), OPEN, OPEN)));
        }
      }

      D.add(D_i);
    }

    return new HashSet<>(D.get(variableOrdering.size()));
  }

  public Set<MultivariatePolynomial> mcCallumProjection(
      Set<MultivariatePolynomial> polynomials, String variable) {
    List<MultivariatePolynomial> result = new ArrayList<>();

    for (MultivariatePolynomial p : polynomials) {
      for (MultivariatePolynomial q : polynomials) {
        if (p.equals(q)) {
          continue;
        }

        MultivariatePolynomial resultant = p.resultant(q, variable);

        if (resultant.isConstant()) {
          continue;
        }

        result.add(resultant);
      }
    }

    for (MultivariatePolynomial polynomial : polynomials) {
      MultivariatePolynomial disc = polynomial.discriminant(variable);
      if (disc.isConstant()) {
        continue;
      }

      result.add(disc);
    }

    for (MultivariatePolynomial polynomial : polynomials) {
      List<MultivariatePolynomial> coefficients = polynomial.getCoefficients(variable);

      for (MultivariatePolynomial coefficient : coefficients) {
        if (coefficient.isConstant()) {
          continue;
        }

        result.add(coefficient);
      }
    }

    Set<MultivariatePolynomial> unique = new HashSet<>();

    for (MultivariatePolynomial multivariatePolynomial : result) {
      boolean contains = false;
      for (MultivariatePolynomial added : unique) {
        if (multivariatePolynomial.equals(added)) {
          contains = true;
          break;
        }
      }

      if (!contains) {
        unique.add(multivariatePolynomial);
      }
    }

    return unique;
  }
}
