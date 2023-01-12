package me.paultristanwagner.satchecking.theory;

import me.paultristanwagner.satchecking.smt.VariableAssignment;

import java.util.Set;

public class SimplexResult extends TheoryResult<LinearConstraint> {

  boolean feasible;
  boolean unbounded;
  boolean optimal;
  double optimum;

  private SimplexResult(
      boolean satisfiable,
      boolean feasible,
      boolean unbounded,
      boolean optimal,
      double optimum,
      VariableAssignment solution,
      Set<LinearConstraint> explanation) {
    super(satisfiable, solution, explanation);
    this.feasible = feasible;
    this.unbounded = unbounded;
    this.optimal = optimal;
    this.optimum = optimum;
  }

  public static SimplexResult infeasible(Set<LinearConstraint> explanation) {
    return new SimplexResult(false, false, false, false, 0, null, explanation);
  }

  public static SimplexResult feasible(VariableAssignment solution) {
    return new SimplexResult(true, true, false, false, 0, solution, null);
  }

  public static SimplexResult optimal(VariableAssignment solution, double optimum) {
    return new SimplexResult(true, true, false, true, optimum, solution, null);
  }

  public static SimplexResult unbounded(
      VariableAssignment solution, Set<LinearConstraint> explanation) {
    return new SimplexResult(false, true, true, false, 0, solution, explanation);
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

  public double getOptimum() {
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
