package me.paultristanwagner.satchecking.smt;

import me.paultristanwagner.satchecking.theory.Constraint;

public class SMTResult<C extends Constraint> {

  private final boolean unknown;
  private final boolean satisfiable;
  private final VariableAssignment solution;

  private SMTResult(boolean unknown, boolean satisfiable, VariableAssignment solution) {
    this.unknown = unknown;
    this.satisfiable = satisfiable;
    this.solution = solution;
  }

  public static <C extends Constraint> SMTResult<C> unknown() {
    return new SMTResult<>(true, false, null);
  }

  public static <C extends Constraint> SMTResult<C> satisfiable(
      VariableAssignment variableAssignment) {
    return new SMTResult<>(false, true, variableAssignment);
  }

  public static <C extends Constraint> SMTResult<C> unsatisfiable() {
    return new SMTResult<>(false, false, null);
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
}
