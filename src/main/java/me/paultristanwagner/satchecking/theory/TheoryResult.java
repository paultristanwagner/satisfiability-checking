package me.paultristanwagner.satchecking.theory;

import me.paultristanwagner.satchecking.smt.VariableAssignment;

import java.util.Set;

public class TheoryResult<C extends Constraint> {

  protected final boolean unknown;
  protected final boolean satisfiable;
  protected final VariableAssignment solution;
  protected final Set<C> explanation;

  protected TheoryResult(
      boolean unknown, boolean satisfiable, VariableAssignment solution, Set<C> explanation) {
    this.unknown = unknown;
    this.satisfiable = satisfiable;
    this.solution = solution;
    this.explanation = explanation;
  }

  public static <C extends Constraint> TheoryResult<C> satisfiable(VariableAssignment solution) {
    return new TheoryResult<>(false, true, solution, null);
  }

  public static <C extends Constraint> TheoryResult<C> unsatisfiable(Set<C> explanation) {
    return new TheoryResult<>(false, false, null, explanation);
  }

  public static <C extends Constraint> TheoryResult<C> unknown() {
    return new TheoryResult<>(true, false, null, null);
  }

  public boolean isUnknown() {
    return unknown;
  }

  public boolean isSatisfiable() {
    return satisfiable;
  }

  public VariableAssignment getSolution() {
    return solution;
  }

  public Set<C> getExplanation() {
    return explanation;
  }
}
