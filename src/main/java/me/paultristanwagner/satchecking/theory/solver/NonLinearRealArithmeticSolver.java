package me.paultristanwagner.satchecking.theory.solver;

import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.TheoryResult;
import me.paultristanwagner.satchecking.theory.nonlinear.*;
import me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomialConstraint.MultivariateMaximizationConstraint;
import me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomialConstraint.MultivariateMinimizationConstraint;
import me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomialConstraint.MultivariateOptimizationConstraint;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import static me.paultristanwagner.satchecking.theory.TheoryResult.satisfiable;
import static me.paultristanwagner.satchecking.theory.TheoryResult.unsatisfiable;
import static me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomial.variable;

public class NonLinearRealArithmeticSolver implements TheorySolver<MultivariatePolynomialConstraint> {

  private static final String TARGET_VARIABLE_NAME_BASE = "target_";

  private final Set<MultivariatePolynomialConstraint> constraints = new HashSet<>();
  private MultivariateOptimizationConstraint objective;

  @Override
  public void clear() {
    constraints.clear();
  }

  @Override
  public void load(Set<MultivariatePolynomialConstraint> constraints) {
    clear();

    constraints.forEach(this::addConstraint);
  }

  @Override
  public void addConstraint(MultivariatePolynomialConstraint constraint) {
    if (constraint instanceof MultivariateOptimizationConstraint optimizationConstraint) {
      if (objective != null) {
        throw new IllegalArgumentException("More than one optimization objective provided");
      }

      objective = optimizationConstraint;
      return;
    }

    constraints.add(constraint);
  }

  @Override
  public TheoryResult<MultivariatePolynomialConstraint> solve() {
    // collect all variables
    Set<String> variablesSet = new HashSet<>();
    for (MultivariatePolynomialConstraint constraint : constraints) {
      MultivariatePolynomial polynomial = constraint.getPolynomial();
      variablesSet.addAll(polynomial.variables);
    }

    // if we have an optimization objective, we need to add a fresh variable and fix the variable ordering
    List<String> variableOrdering = null;
    String freshVariableName = null;
    if (objective != null) {
      freshVariableName = freshVariableName(variablesSet);
      MultivariatePolynomial freshVariable = variable(freshVariableName);
      MultivariatePolynomialConstraint helper = MultivariatePolynomialConstraint.equals(freshVariable, objective.getObjective());

      constraints.add(helper);

      variableOrdering = new ArrayList<>();
      variableOrdering.add(freshVariableName);
      variableOrdering.addAll(variablesSet);
    }

    CAD cad = new CAD();
    Set<Cell> result = cad.compute(constraints, variableOrdering);

    List<Pair<Cell, VariableAssignment<RealAlgebraicNumber>>> pairs = new ArrayList<>();
    for (Cell cell : result) {
      Map<String, RealAlgebraicNumber> samplePoint = cell.chooseSamplePoint();
      VariableAssignment<RealAlgebraicNumber> variableAssignment = new VariableAssignment<>(samplePoint);

      pairs.add(Pair.of(cell, variableAssignment));
    }

    if (objective != null) {
      Comparator<Interval> comparator;

      if (objective instanceof MultivariateMaximizationConstraint) {
        comparator = new Interval.LowerBoundIntervalComparator().reversed();
      } else {
        comparator = new Interval.LowerBoundIntervalComparator();
      }

      pairs.sort((a, b) -> comparator.compare(a.getLeft().getIntervals().get(0), b.getLeft().getIntervals().get(0)));
    }

    for (Pair<Cell, VariableAssignment<RealAlgebraicNumber>> pair : pairs) {
      Cell cell = pair.getLeft();
      VariableAssignment<RealAlgebraicNumber> variableAssignment = pair.getRight();

      boolean satisfied = true;
      for (MultivariatePolynomialConstraint constraint : constraints) {
        int sign = constraint.getPolynomial().evaluateSign(variableAssignment);

        if (!constraint.getComparison().evaluateSign(sign)) {
          satisfied = false;
          break;
        }
      }

      if (satisfied) {
        if (objective != null) {
          variableAssignment.remove(freshVariableName);

          Interval targetInterval = cell.getIntervals().get(0);

          boolean unboundedInDirection =
              (objective instanceof MultivariateMaximizationConstraint && targetInterval.getUpperBoundType() != Interval.IntervalBoundType.CLOSED) ||
                  (objective instanceof MultivariateMinimizationConstraint && targetInterval.getLowerBoundType() != Interval.IntervalBoundType.CLOSED);

          if (unboundedInDirection) {
            return unsatisfiable(constraints);
          } else {
            return satisfiable(new VariableAssignment(variableAssignment));
          }
        }

        return satisfiable(new VariableAssignment(variableAssignment));
      }
    }

    return unsatisfiable(constraints);
  }

  private String freshVariableName(Set<String> variables) {
    int i = 0;
    String freshVariableName = TARGET_VARIABLE_NAME_BASE + i;
    while (variables.contains(freshVariableName)) {
      freshVariableName = TARGET_VARIABLE_NAME_BASE + i;
      i++;
    }
    return freshVariableName;
  }
}
