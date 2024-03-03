package me.paultristanwagner.satchecking.theory;

import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import java.util.HashSet;
import java.util.Set;

import static me.paultristanwagner.satchecking.theory.LinearConstraint.Bound.*;
import static me.paultristanwagner.satchecking.theory.arithmetic.Number.ZERO;

public class LinearConstraint implements Constraint {

  protected final LinearTerm lhs;
  protected final LinearTerm rhs;
  protected final LinearTerm difference;
  protected final Bound bound;

  private LinearConstraint derivedFrom;

  public LinearConstraint() {
    this.lhs = new LinearTerm();
    this.rhs = new LinearTerm();
    this.difference = new LinearTerm();
    this.bound = EQUAL;
  }

  public LinearConstraint(LinearConstraint constraint) {
    this.lhs = new LinearTerm(constraint.lhs);
    this.rhs = new LinearTerm(constraint.rhs);
    this.difference = new LinearTerm(constraint.difference);
    this.bound = constraint.bound;
    this.derivedFrom = constraint;
  }

  public LinearConstraint(LinearTerm lhs, LinearTerm rhs, Bound bound) {
    this.lhs = lhs;
    this.rhs = rhs;
    this.difference = lhs.subtract(rhs);
    this.bound = bound;
  }

  public static LinearConstraint equal(LinearTerm lhs, LinearTerm rhs) {
    return new LinearConstraint(lhs, rhs, EQUAL);
  }

  public static LinearConstraint lessThanOrEqual(LinearTerm lhs, LinearTerm rhs) {
    return new LinearConstraint(lhs, rhs, GREATER_EQUALS);
  }

  public static LinearConstraint greaterThanOrEqual(LinearTerm lhs, LinearTerm rhs) {
    return new LinearConstraint(lhs, rhs, LESS_EQUALS);
  }

  public Set<String> getVariables() {
    Set<String> variables = new HashSet<>(lhs.getVariables());
    variables.addAll(rhs.getVariables());
    return variables;
  }

  public Bound getBound() {
    return bound;
  }

  public void setDerivedFrom(LinearConstraint derivedFrom) {
    this.derivedFrom = derivedFrom;
  }

  public boolean constrainsVariable(String variable) {
    return difference.getCoefficients().getOrDefault(variable, ZERO()).isNonZero();
  }

  public Number getBoundOn(String variable) {
    Set<String> variables = getVariables();

    if (variables.size() != 1) {
      throw new IllegalStateException("Constraint does not have exactly one variable");
    }

    if (!getVariables().contains(variable)) {
      throw new IllegalArgumentException("Variable is not in constraint");
    }

    Number coefficient = difference.coefficients.get(variable);

    return difference.getConstant().negate().divide(coefficient);
  }

  public LinearConstraint getRoot() {
    if (derivedFrom == null) {
      return this;
    }
    return derivedFrom.getRoot();
  }

  public LinearConstraint offset(String variable, String substitute, Number offset) {
    LinearTerm lhs = this.lhs.offset(variable, substitute, offset);
    LinearTerm rhs = this.rhs.offset(variable, substitute, offset);

    return new LinearConstraint(lhs, rhs, this.bound);
  }

  public LinearConstraint positiveNegativeSubstitute(
      String variable, String positive, String negative) {

    LinearTerm lhs = this.lhs.positiveNegativeSubstitute(variable, positive, negative);
    LinearTerm rhs = this.rhs.positiveNegativeSubstitute(variable, positive, negative);

    return new LinearConstraint(lhs, rhs, this.bound);
  }

  public LinearTerm getLeftHandSide() {
    return lhs;
  }

  public LinearTerm getRightHandSide() {
    return rhs;
  }

  public LinearTerm getDifference() {
    return difference;
  }

  public enum Bound {
    GREATER_EQUALS,
    LESS_EQUALS,
    EQUAL
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(lhs);

    if (bound == EQUAL) {
      sb.append("=");
    } else if (bound == GREATER_EQUALS) {
      sb.append(">=");
    } else {
      sb.append("<=");
    }

    sb.append(rhs);

    return sb.toString();
  }

  public static class MaximizingConstraint extends LinearConstraint {

    public MaximizingConstraint(LinearTerm term) {
      super(term, new LinearTerm(), EQUAL);
    }

    public LinearTerm getTerm() {
      return lhs;
    }

    @Override
    public String toString() {
      return "max(" + lhs + ")";
    }
  }

  public static class MinimizingConstraint extends LinearConstraint {

    public MinimizingConstraint(LinearTerm term) {
      super(term, new LinearTerm(), EQUAL);
    }

    public LinearTerm getTerm() {
      return lhs;
    }

    @Override
    public String toString() {
      return "min(" + lhs + ")";
    }
  }

  public boolean evaluate(VariableAssignment<Number> assignment) {
    Number lhsValue = lhs.evaluate(assignment);
    Number rhsValue = rhs.evaluate(assignment);

    if (bound == EQUAL) {
      return lhsValue.equals(rhsValue);
    } else if (bound == GREATER_EQUALS) {
      return lhsValue.greaterThanOrEqual(rhsValue);
    } else {
      return lhsValue.lessThanOrEqual(rhsValue);
    }
  }
}
