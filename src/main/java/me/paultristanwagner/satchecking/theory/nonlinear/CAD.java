package me.paultristanwagner.satchecking.theory.nonlinear;

import static me.paultristanwagner.satchecking.theory.arithmetic.Number.number;
import static me.paultristanwagner.satchecking.theory.nonlinear.Cell.emptyCell;
import static me.paultristanwagner.satchecking.theory.nonlinear.Interval.*;
import static me.paultristanwagner.satchecking.theory.nonlinear.Interval.IntervalBoundType.OPEN;

import java.util.*;

import me.paultristanwagner.satchecking.parse.PolynomialParser;
import me.paultristanwagner.satchecking.smt.VariableAssignment;

public class CAD {

  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);
    PolynomialParser parser = new PolynomialParser();

    Set<MultivariatePolynomial> polynomials = new HashSet<>();
    String line;
    while ((line = scanner.nextLine()) != null) {
      if(line.equalsIgnoreCase("solve")) {
        break;
      }

      MultivariatePolynomial polynomial = parser.parse(line);
      polynomials.add(polynomial);
    }

    System.out.println("polynomials = " + polynomials);
    System.out.println();

    CAD cad = new CAD();
    cad.compute(polynomials);
  }

  private List<String> variables;
  private Set<MultivariatePolynomial> polynomials;

  public Set<VariableAssignment<RealAlgebraicNumber>> compute(
      Set<MultivariatePolynomial> polynomials) {
    System.out.println("computing CAD...");

    Set<String> variablesSet = new HashSet<>();
    for (MultivariatePolynomial polynomial : polynomials) {
      variablesSet.addAll(polynomial.variables);
    }
    this.variables = new ArrayList<>(variablesSet);
    System.out.println("variables = " + this.variables);

    // phase 1: projection
    Map<Integer, Set<MultivariatePolynomial>> p = new HashMap<>();
    p.put(variables.size(), polynomials);

    for (int r = variables.size() - 1; r >= 1; r--) {
      String variable = variables.get(r);
      System.out.println("eliminating " + variable);
      Set<MultivariatePolynomial> proj = mcCallumProjection(p.get(r + 1), variable);
      p.put(r, proj);

      String previousVariable = variables.get(r);
      p.get(r + 1).stream().filter(poly -> !poly.highestVariable().equals(previousVariable));
    }
    System.out.println("p's = " + p);

    // phase 2: lifting
    List<List<Cell>> D = new ArrayList<>();
    D.add(List.of(emptyCell()));

    for (int i = 1; i <= variables.size(); i++) {
      List<Cell> D_i = new ArrayList<>();
      String variable = variables.get(i - 1);

      for (Cell R : D.get(i - 1)) {
        System.out.println("lifting " + R);
        Map<String, RealAlgebraicNumber> s = R.chooseSamplePoint();
        System.out.println("s = " + s);

        Set<RealAlgebraicNumber> roots = new HashSet<>();
        System.out.println("p_" + i + " = " + p.get(i));
        for (MultivariatePolynomial polynomial : p.get(i)) {
          MultivariatePolynomial substituted = polynomial.substitute(s);
          System.out.println(substituted);
          Polynomial univariate = substituted.toUnivariatePolynomial();
          roots.addAll(univariate.isolateRoots());
        }

        System.out.println("roots = " + roots);
        // sort roots
        List<RealAlgebraicNumber> sortedRoots = new ArrayList<>(roots);
        sortedRoots.sort((a, b) -> a.equals(b) ? 0 : a.lessThan(b) ? -1 : 1); // todo: make comparable
        System.out.println("roots (sorted) = " + sortedRoots);
        // remove duplicates
        Iterator<RealAlgebraicNumber> iterator = sortedRoots.iterator();
        RealAlgebraicNumber previous = null;
        while (iterator.hasNext()) {
          RealAlgebraicNumber current = iterator.next();
          if (previous != null && previous.equals(current)) {
            System.out.println("previous = " + previous);
            System.out.println("current = " + current);
            System.out.println("removing");
            iterator.remove();
          }
          previous = current;
        }

        System.out.println("roots (sorted, without duplicates) = " + sortedRoots);

        if (sortedRoots.isEmpty()) {
          D_i.add(R.extend(variable, unboundedInterval()));
        } else {
          D_i.add(R.extend(variable, intervalLowerUnbounded(sortedRoots.get(0), OPEN)));
          D_i.add(R.extend(variable, pointInterval(sortedRoots.get(sortedRoots.size() - 1))));
          D_i.add(R.extend(variable, intervalUpperUnbounded(sortedRoots.get(sortedRoots.size() - 1), OPEN)));
        }

        for (int j = 0; j < sortedRoots.size() - 1; j++) {
          RealAlgebraicNumber a = sortedRoots.get(j);
          RealAlgebraicNumber b = sortedRoots.get(j + 1);

          D_i.add(R.extend(variable, pointInterval(a)));
          D_i.add(R.extend(variable, interval(a, b, OPEN, OPEN)));
        }
      }

      D.add(D_i);
    }

    List<Cell> result = D.get(variables.size());
    Set<Map<String, RealAlgebraicNumber>> samplePoints = new HashSet<>();
    for (Cell cell : result) {
      Map<String, RealAlgebraicNumber> sample = cell.chooseSamplePoint();
      samplePoints.add(sample);
      System.out.println(sample + " from " + cell);
    }
    List<List<RealAlgebraicNumber>> samplePointsList = new ArrayList<>();
    for (Map<String, RealAlgebraicNumber> samplePoint : samplePoints) {
      List<RealAlgebraicNumber> samplePointList = new ArrayList<>();
      for (String variable : variables) {
        samplePointList.add(samplePoint.get(variable));
      }
      samplePointsList.add(samplePointList);
    }

    System.out.println("polynomials = " + polynomials);

    System.out.println("result = " + samplePointsList);
    System.out.println("result.size() = " + samplePointsList.size());
    System.out.println();

    for (Map<String, RealAlgebraicNumber> samplePoint : samplePoints) {
      System.out.println("sample point = " + samplePoint);
      boolean isCommonRoot = true;
      for (MultivariatePolynomial polynomial : polynomials) {
        int sign = polynomial.evaluateSign(samplePoint);
        if(sign != 0) {
          isCommonRoot = false;
          break;
        }
      }

      if(isCommonRoot) {
        System.out.println("common root = " + samplePoint);
        samplePoint.forEach((variable, realAlgebraicNumber) -> System.out.println(variable + " = " + realAlgebraicNumber.approximate(number(2).pow(-54)).approximateAsDouble()));
      }
    }

    return null;
  }

  public Set<MultivariatePolynomial> mcCallumProjection(
      Set<MultivariatePolynomial> polynomials, String variable) {
    Set<MultivariatePolynomial> result = new HashSet<>();

    for (MultivariatePolynomial p : polynomials) {
      for (MultivariatePolynomial q : polynomials) {
        if (p == q) {
          continue;
        }

        result.add(p.resultant(q, variable));
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

    return result;
  }
}
