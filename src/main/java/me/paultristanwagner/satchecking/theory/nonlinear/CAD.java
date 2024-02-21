package me.paultristanwagner.satchecking.theory.nonlinear;

import me.paultristanwagner.satchecking.parse.Parser;
import me.paultristanwagner.satchecking.parse.PolynomialParser;
import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.arithmetic.Rational;

import java.util.*;

import static me.paultristanwagner.satchecking.theory.nonlinear.Cell.emptyCell;
import static me.paultristanwagner.satchecking.theory.nonlinear.Interval.IntervalBoundType.OPEN;
import static me.paultristanwagner.satchecking.theory.nonlinear.Interval.*;
import static me.paultristanwagner.satchecking.theory.nonlinear.RealAlgebraicNumber.realAlgebraicNumber;

public class CAD {

  public static void main(String[] args) {
    Parser<MultivariatePolynomial> parser = new PolynomialParser();
    MultivariatePolynomial p = parser.parse("x^2 + y^2 - 1");
    MultivariatePolynomial q = parser.parse("x^2 + y^3 - 1/2");

    RealAlgebraicNumber x = realAlgebraicNumber(parser.parse("x^6-2x^4+2x^2-3/4").toUnivariatePolynomial(), Rational.parse("105/128"), Rational.parse("27/32"));
    RealAlgebraicNumber y = realAlgebraicNumber(parser.parse("x^6-1x^4+x^2-1/4").toUnivariatePolynomial(), Rational.parse("1/2"), Rational.parse("3/4"));

    System.out.println(p);
    System.out.println(q);
    System.out.println(x);
    System.out.println(y);



  }

  private List<String> variables;
  private Set<MultivariatePolynomial> polynomials;

  public CAD(Set<MultivariatePolynomial> polynomials) {
    this.polynomials = polynomials;

    Set<String> variablesSet = new HashSet<>();
    for (MultivariatePolynomial polynomial : polynomials) {
      variablesSet.addAll(polynomial.variables);
    }
    this.variables = new ArrayList<>(variablesSet);


  }

  public Set<VariableAssignment<RealAlgebraicNumber>> compute(
      Set<MultivariatePolynomialConstraint> constraints
  ) {
    return compute(constraints, false);
  }

  public Set<VariableAssignment<RealAlgebraicNumber>> compute(
      Set<MultivariatePolynomialConstraint> constraints,
      boolean onlyEqualities
  ) {
    this.polynomials = new HashSet<>();
    for (MultivariatePolynomialConstraint constraint : constraints) {
      this.polynomials.add(constraint.getPolynomial());
    }

    Set<String> variablesSet = new HashSet<>();
    for (MultivariatePolynomial polynomial : polynomials) {
      variablesSet.addAll(polynomial.variables);
    }
    this.variables = new ArrayList<>(variablesSet);

    // phase 1: projection
    Map<Integer, Set<MultivariatePolynomial>> p = new HashMap<>();
    p.put(variables.size(), polynomials);

    for (int r = variables.size() - 1; r >= 1; r--) {
      String variable = variables.get(r);
      Set<MultivariatePolynomial> proj = mcCallumProjection(p.get(r + 1), variable);
      p.put(r, proj);

      String previousVariable = variables.get(r);
      p.get(r + 1).stream().filter(poly -> !poly.highestVariable().equals(previousVariable));
    }

    // phase 2: lifting
    List<List<Cell>> D = new ArrayList<>();
    D.add(List.of(emptyCell()));

    for (int i = 1; i <= variables.size(); i++) {
      List<Cell> D_i = new ArrayList<>();
      String variable = variables.get(i - 1);

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
          if (!onlyEqualities) {
            D_i.add(R.extend(variable, unboundedInterval()));
          }
        } else {
          if (!onlyEqualities) {
            D_i.add(R.extend(variable, intervalLowerUnbounded(sortedRoots.get(0), OPEN)));
            D_i.add(R.extend(variable, intervalUpperUnbounded(sortedRoots.get(sortedRoots.size() - 1), OPEN)));

          }
          D_i.add(R.extend(variable, pointInterval(sortedRoots.get(sortedRoots.size() - 1))));
        }

        for (int j = 0; j < sortedRoots.size() - 1; j++) {
          RealAlgebraicNumber a = sortedRoots.get(j);
          RealAlgebraicNumber b = sortedRoots.get(j + 1);

          D_i.add(R.extend(variable, pointInterval(a)));
          if (!onlyEqualities) {
            D_i.add(R.extend(variable, interval(a, b, OPEN, OPEN)));
          }
        }
      }

      D.add(D_i);
    }

    List<Cell> result = D.get(variables.size());
    Set<VariableAssignment<RealAlgebraicNumber>> assignments = new HashSet<>();
    for (Cell cell : result) {
      VariableAssignment<RealAlgebraicNumber> assignment = new VariableAssignment<>(cell.chooseSamplePoint());
      assignments.add(assignment);
    }

    return assignments;
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
