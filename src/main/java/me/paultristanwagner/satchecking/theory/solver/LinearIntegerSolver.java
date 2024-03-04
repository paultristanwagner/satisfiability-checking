package me.paultristanwagner.satchecking.theory.solver;

import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.LinearTerm;
import me.paultristanwagner.satchecking.theory.TheoryResult;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import java.util.HashSet;
import java.util.Set;

import static me.paultristanwagner.satchecking.theory.LinearConstraint.greaterThanOrEqual;
import static me.paultristanwagner.satchecking.theory.LinearConstraint.lessThanOrEqual;
import static me.paultristanwagner.satchecking.theory.arithmetic.Number.ONE;

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

    // todo: make checked
    VariableAssignment<Number> assignment = result.getSolution();
    boolean integral = true;
    String firstNonIntegralVariable = null;
    Number nonIntegralValue = Number.ZERO();

    for (String variable : assignment.getVariables()) {
      Number value = assignment.getAssignment(variable);
      if (!value.isInteger()) {
        integral = false;
        firstNonIntegralVariable = variable;
        nonIntegralValue = value;
        break;
      }
    }

    if (integral) {
      return result;
    }

    LinearTerm term = new LinearTerm();
    term.setCoefficient(firstNonIntegralVariable, ONE());
    term.setConstant(nonIntegralValue.floor().negate());
    LinearConstraint upperBound = lessThanOrEqual(term, new LinearTerm());

    LinearIntegerSolver solverA = new LinearIntegerSolver(depth + 1);
    solverA.load(constraints);
    solverA.addConstraint(upperBound);

    boolean isOptimizationProblem = simplexSolver.hasObjectiveFunction();
    boolean isMaximizationProblem = simplexSolver.isMaximization();
    boolean isMinimizationProblem = simplexSolver.isMinimization();

    Number localOptimum = null;
    LinearTerm objective = simplexSolver.getOriginalObjective().getLeftHandSide();

    TheoryResult<LinearConstraint> aResult = solverA.solve();
    if (aResult.isUnknown()) {
      unknownInvolved = true;
    }

    if (aResult.isSatisfiable()) {
      if (!isOptimizationProblem) {
        return aResult;
      }

      localOptimum = objective.evaluate(aResult.getSolution());
    }

    LinearTerm termB = new LinearTerm();
    termB.setCoefficient(firstNonIntegralVariable, ONE());
    termB.setConstant(nonIntegralValue.ceil().negate());
    LinearConstraint lowerBound = greaterThanOrEqual(termB, new LinearTerm());

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
      }

      Number value = objective.evaluate(bResult.getSolution());
      if (isMaximizationProblem) {
        if (localOptimum == null || value.greaterThan(localOptimum)) {
          return bResult;
        } else {
          return aResult;
        }
      } else if (isMinimizationProblem) {
        if (localOptimum == null || value.lessThan(localOptimum)) {
          return bResult;
        } else {
          return aResult;
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
