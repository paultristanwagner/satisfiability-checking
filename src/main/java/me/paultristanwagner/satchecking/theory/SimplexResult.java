package me.paultristanwagner.satchecking.theory;

import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import java.util.Set;

import static me.paultristanwagner.satchecking.theory.arithmetic.Number.ZERO;

public class SimplexResult extends TheoryResult<LinearConstraint> {

  final boolean feasible;
  final boolean unbounded;
  final boolean optimal;
  final Number optimum;

  private SimplexResult(
      boolean unknown,
      boolean satisfiable,
      boolean feasible,
      boolean unbounded,
      boolean optimal,
      Number optimum,
      VariableAssignment solution,
      Set<LinearConstraint> explanation) {
    super(unknown, satisfiable, solution, explanation);
    this.feasible = feasible;
    this.unbounded = unbounded;
    this.optimal = optimal;
    this.optimum = optimum;
  }

  public static SimplexResult infeasible(Set<LinearConstraint> explanation) {
    return new SimplexResult(false, false, false, false, false, ZERO(), null, explanation);
  }

  public static SimplexResult feasible(VariableAssignment solution) {
    return new SimplexResult(false, true, true, false, false, ZERO(), solution, null);
  }

  public static SimplexResult optimal(VariableAssignment solution, Number optimum) {
    return new SimplexResult(false, true, true, false, true, optimum, solution, null);
  }

  public static SimplexResult unbounded(
      VariableAssignment solution, Set<LinearConstraint> explanation) {
    return new SimplexResult(false, false, true, true, false, ZERO(), solution, explanation);
  }

  public boolean isFeasible() {
    return feasible;
  }

  public boolean isUnbounded() {
    return unbounded;
  }

  public boolean isOptimal() {
    return optimal;
  }

  public Number getOptimum() {
    return optimum;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (feasible) {
      builder.append(solution);
    } else {
      for (LinearConstraint linearConstraint : this.explanation) {
        builder.append(linearConstraint).append("; ");
      }
    }

    return builder.toString();
  }
}
