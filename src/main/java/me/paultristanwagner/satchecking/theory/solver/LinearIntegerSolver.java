package me.paultristanwagner.satchecking.theory.solver;

import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.TheoryResult;

import java.util.HashSet;
import java.util.Set;

public class LinearIntegerSolver implements TheorySolver<LinearConstraint> {

  private static final long MAXIMUM_BRANCH_DEPTH = (1 << 10);

  private final long depth;
  private final Set<LinearConstraint> constraints;

  public LinearIntegerSolver() {
    this(0);
  }

  public LinearIntegerSolver(long depth) {
    this.depth = depth;
    this.constraints = new HashSet<>();
  }

  @Override
  public void clear() {
    this.constraints.clear();
  }

  @Override
  public void addConstraint(LinearConstraint constraint) {
    this.constraints.add(constraint);
  }

  @Override
  public TheoryResult<LinearConstraint> solve() {
    if (depth > MAXIMUM_BRANCH_DEPTH) {
      return TheoryResult.unknown();
    }

    boolean unknownInvolved = false;

    SimplexOptimizationSolver simplexSolver = new SimplexOptimizationSolver();
    simplexSolver.load(constraints);
    TheoryResult<LinearConstraint> result = simplexSolver.solve();

    if (!result.isSatisfiable()) {
      return result;
    }

    VariableAssignment assignment = result.getSolution();
    boolean integral = true;
    String firstNonIntegralVariable = null;
    double nonIntegralValue = 0;

    for (String variable : assignment.getVariables()) {
      double value = assignment.getAssignment(variable);
      if (value != Math.floor(value)) { // todo: maybe add epsilon here
        integral = false;
        firstNonIntegralVariable = variable;
        nonIntegralValue = value;
        break;
      }
    }

    if (integral) {
      return result;
    }

    LinearConstraint upperBound = new LinearConstraint();
    upperBound.setCoefficient(firstNonIntegralVariable, 1);
    upperBound.setValue(Math.floor(nonIntegralValue));
    upperBound.setBound(LinearConstraint.Bound.UPPER);

    LinearIntegerSolver solverA = new LinearIntegerSolver(depth + 1);
    solverA.load(constraints);
    solverA.addConstraint(upperBound);

    boolean isOptimizationProblem = simplexSolver.hasObjectiveFunction();
    boolean isMaximizationProblem = simplexSolver.isMaximization();
    boolean isMinimizationProblem = simplexSolver.isMinimization();

    double localOptimum =
        isMaximizationProblem ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    LinearConstraint objective = simplexSolver.getOriginalObjective();

    TheoryResult<LinearConstraint> aResult = solverA.solve();
    if (aResult.isUnknown()) {
      unknownInvolved = true;
    }

    if (aResult.isSatisfiable()) {
      if (!isOptimizationProblem) {
        return aResult;
      } else {
        double value = objective.evaluate(aResult.getSolution());
        if (isMaximizationProblem && value > localOptimum) {
          localOptimum = value;
        } else if (isMinimizationProblem && value < localOptimum) {
          localOptimum = value;
        }
      }
    }

    LinearConstraint lowerBound = new LinearConstraint();
    lowerBound.setCoefficient(firstNonIntegralVariable, 1);
    lowerBound.setValue(Math.ceil(nonIntegralValue));
    lowerBound.setBound(LinearConstraint.Bound.LOWER);

    LinearIntegerSolver solverB = new LinearIntegerSolver(depth + 1);
    solverB.load(constraints);
    solverB.addConstraint(lowerBound);

    TheoryResult<LinearConstraint> bResult = solverB.solve();
    if (bResult.isUnknown()) {
      unknownInvolved = true;
    }

    if (bResult.isSatisfiable()) {
      if (!isOptimizationProblem) {
        return bResult;
      } else {
        double value = objective.evaluate(bResult.getSolution());
        if (isMaximizationProblem) {
          if (value > localOptimum) {
            return bResult;
          } else {
            return aResult;
          }
        } else if (isMinimizationProblem) {
          if (value < localOptimum) {
            return bResult;
          } else {
            return aResult;
          }
        }
      }
    } else if (aResult.isSatisfiable()) {
      return aResult;
    }

    if (unknownInvolved) {
      return TheoryResult.unknown();
    }

    return TheoryResult.unsatisfiable(constraints);
  }
}
