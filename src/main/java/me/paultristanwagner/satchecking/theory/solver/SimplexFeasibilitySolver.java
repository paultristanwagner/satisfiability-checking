package me.paultristanwagner.satchecking.theory.solver;

import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.SimplexResult;
import me.paultristanwagner.satchecking.theory.arithmetic.DeltaRational;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import java.util.*;

import static me.paultristanwagner.satchecking.theory.LinearConstraint.Bound.*;
import static me.paultristanwagner.satchecking.theory.arithmetic.Number.ONE;
import static me.paultristanwagner.satchecking.theory.arithmetic.Number.ZERO;

public class SimplexFeasibilitySolver implements TheorySolver<LinearConstraint> {

  private int rows;
  private int columns;
  private Number[][] tableau;

  private List<String> basicVariables;
  private List<String> nonBasicVariables;
  private List<DeltaRational> values;

  private Map<String, DeltaRational> lowerBounds;
  private Map<String, DeltaRational> upperBounds;

  // Maps each slack variable name to the index of the constraint it represents. Used to build
  // infeasibility explanations structurally rather than by parsing variable names.
  private Map<String, Integer> slackConstraintIndices;

  private final List<LinearConstraint> constraints = new ArrayList<>();

  @Override
  public void clear() {
    constraints.clear();
    tableau = null;
    basicVariables = null;
    nonBasicVariables = null;
    values = null;
    lowerBounds = null;
    upperBounds = null;
    slackConstraintIndices = null;
    rows = 0;
    columns = 0;
  }

  @Override
  public void addConstraint(LinearConstraint constraint) {
    if (constraint instanceof LinearConstraint.MaximizingConstraint
        || constraint instanceof LinearConstraint.MinimizingConstraint) {
      throw new IllegalArgumentException("Use SimplexOptimizer for optimization");
    }

    constraints.add(constraint);
  }

  @Override
  public SimplexResult solve() {
    this.lowerBounds = new HashMap<>();
    this.upperBounds = new HashMap<>();

    // Collect variables
    Set<String> variableSet = new HashSet<>();
    for (LinearConstraint constraint : constraints) {
      variableSet.addAll(constraint.getVariables());
    }
    List<String> variables = new ArrayList<>(variableSet);
    variables.sort(String::compareTo);

    // Initialize non-basic variables
    nonBasicVariables = new ArrayList<>(variables);
    values = new ArrayList<>();
    for (int i = 0; i < variables.size(); i++) {
      values.add(DeltaRational.ZERO);
    }

    // Initialize slack variables, tableau and constraints
    basicVariables = new ArrayList<>();
    slackConstraintIndices = new HashMap<>();
    tableau = new Number[constraints.size()][variableSet.size()];
    for (int i = 0; i < constraints.size(); i++) {
      String slackName = "s" + i;
      LinearConstraint constraint = constraints.get(i);
      basicVariables.add(slackName);
      slackConstraintIndices.put(slackName, i);

      for (int j = 0; j < variableSet.size(); j++) {
        String variable = variables.get(j);
        tableau[i][j] = constraint.getDifference().getCoefficients().getOrDefault(variable, ZERO());
      }

      // c is the value the row-term (sum of coefficient*variable) is bounded against.
      Number c = constraint.getDifference().getConstant().negate();

      switch (constraint.getBound()) {
        case EQUAL -> {
          lowerBounds.put(slackName, DeltaRational.of(c));
          upperBounds.put(slackName, DeltaRational.of(c));
        }
        case LESS_EQUALS -> upperBounds.put(slackName, DeltaRational.of(c));
        case GREATER_EQUALS -> lowerBounds.put(slackName, DeltaRational.of(c));
        // strict: s < c  <=>  s <= c - delta
        case LESS -> upperBounds.put(slackName, DeltaRational.of(c, ONE().negate()));
        // strict: s > c  <=>  s >= c + delta
        case GREATER -> lowerBounds.put(slackName, DeltaRational.of(c, ONE()));
      }
    }

    this.rows = tableau.length;
    this.columns = tableau.length == 0 ? 0 : tableau[0].length;

    while (true) {
      Violation violation = getViolation();
      if (violation == null) {
        break;
      }

      String pivotVariable = getPivotVariable(violation.variable(), violation.increase());
      if (pivotVariable == null) {
        Set<LinearConstraint> explanation = calculateExplanation(violation);
        return SimplexResult.infeasible(explanation);
      }
      pivot(violation.variable(), pivotVariable, violation.increase());
    }

    // The current assignment is feasible over the delta-rationals. Pick a concrete, safe
    // positive delta to materialize a real-valued witness (see chooseDelta).
    Number delta = chooseDelta();

    VariableAssignment variableAssignment = new VariableAssignment();

    for (String variable : variables) {
      DeltaRational value;
      if (basicVariables.contains(variable)) {
        value = getBasicValue(basicVariables.indexOf(variable));
      } else {
        value = values.get(nonBasicVariables.indexOf(variable));
      }
      // materialize c + k*delta
      Number concrete = value.getRational().add(value.getDelta().multiply(delta));
      variableAssignment.assign(variable, concrete);
    }
    return SimplexResult.feasible(variableAssignment);
  }

  /**
   * Chooses a concrete positive delta such that every delta-rational variable value {@code (c, k)}
   * stays within its bounds. The verdict (SAT/UNSAT) does NOT depend on this choice; delta only
   * shapes the concrete witness.
   *
   * <p>For each basic variable with value {@code (c_val, k_val)} we look at its bounds {@code
   * (c_bound, k_bound)}. The materialized value {@code c_val + k_val*delta} must respect {@code
   * c_bound + k_bound*delta}. When the rational parts are equal the constraint is already satisfied
   * for any delta (the delta parts are consistent by construction of the feasible point). When the
   * rational parts differ, the bound is strictly satisfied for all sufficiently small delta; the
   * critical delta where it would be violated is {@code (c_bound - c_val) / (k_val - k_bound)},
   * which we take only when positive. We pick delta to be the minimum of all such positive critical
   * values (halved, to stay strictly inside), or 1 when there is no such bound.
   */
  private Number chooseDelta() {
    Number delta = null; // will hold the minimum positive critical ratio

    for (int i = 0; i < rows; i++) {
      String variable = basicVariables.get(i);
      DeltaRational value = getBasicValue(i);

      DeltaRational upper = upperBounds.get(variable);
      if (upper != null) {
        Number ratio = criticalRatio(value, upper);
        if (ratio != null && (delta == null || ratio.lessThan(delta))) {
          delta = ratio;
        }
      }

      DeltaRational lower = lowerBounds.get(variable);
      if (lower != null) {
        Number ratio = criticalRatio(value, lower);
        if (ratio != null && (delta == null || ratio.lessThan(delta))) {
          delta = ratio;
        }
      }
    }

    if (delta == null) {
      return ONE();
    }

    // Stay strictly below the critical value to keep all strict bounds satisfied.
    return delta.divide(Number.number(2));
  }

  /**
   * Returns the positive critical delta where {@code value} would meet {@code bound}, or null if
   * the bound imposes no positive upper limit on delta (rational parts equal, or the value moves
   * away from the bound as delta grows).
   *
   * <p>value = (c_val, k_val), bound = (c_bound, k_bound). Critical delta = (c_bound - c_val) /
   * (k_val - k_bound), taken only when both numerator-direction and denominator make it positive.
   */
  private Number criticalRatio(DeltaRational value, DeltaRational bound) {
    Number cDiff = bound.getRational().subtract(value.getRational()); // c_bound - c_val
    Number kDiff = value.getDelta().subtract(bound.getDelta()); // k_val - k_bound

    if (cDiff.isZero()) {
      // rational parts equal: feasibility already holds in the delta-part for all delta > 0.
      return null;
    }
    if (kDiff.isZero()) {
      // delta does not move value relative to bound; rational parts already separate them.
      return null;
    }

    Number ratio = cDiff.divide(kDiff);
    if (ratio.isPositive()) {
      return ratio;
    }
    return null;
  }

  private Set<LinearConstraint> calculateExplanation(Violation violation) {
    Set<LinearConstraint> explanation = new HashSet<>();

    // The violating variable is a slack; add the constraint it represents.
    Integer violatingIndex = slackConstraintIndices.get(violation.variable());
    if (violatingIndex != null) {
      explanation.add(constraints.get(violatingIndex));
    }

    // Add the constraints of every slack with a non-zero coefficient in the violating row.
    // Original (non-slack) variables represent no constraint, so they are skipped. Previously
    // this parsed the variable name (split("s")), which crashed on original variables or names
    // containing 's', and after pivots when a non-basic was an original variable.
    int basicIndex = basicVariables.indexOf(violation.variable());
    for (int j = 0; j < columns; j++) {
      Number a = tableau[basicIndex][j];
      if (a.isNonZero()) {
        Integer constraintIndex = slackConstraintIndices.get(nonBasicVariables.get(j));
        if (constraintIndex != null) {
          explanation.add(constraints.get(constraintIndex));
        }
      }
    }

    return explanation;
  }

  private Violation getViolation() {
    Violation result = null;
    for (int i = 0; i < rows; i++) {
      String variable = basicVariables.get(i);
      DeltaRational value = getBasicValue(i);
      if (result != null && result.variable().compareTo(variable) < 0) {
        continue;
      }

      if (upperBounds.containsKey(variable)) {
        DeltaRational u = upperBounds.get(variable);
        if (value.greaterThan(u)) {
          result = new Violation(variable, false);
        }
      }

      if (lowerBounds.containsKey(variable)) {
        DeltaRational l = lowerBounds.get(variable);
        if (value.lessThan(l)) {
          result = new Violation(variable, true);
        }
      }
    }
    return result;
  }

  private String getPivotVariable(String violatingVariable, boolean increase) {
    String result = null;
    boolean found = false;
    for (int i = 0; i < columns; i++) {
      String nonBasic = this.nonBasicVariables.get(i);
      if ((!found || nonBasic.compareTo(result) < 0)
          && canPivot(violatingVariable, nonBasic, increase)) {
        result = nonBasic;
        found = true;
      }
    }
    return result;
  }

  private boolean canPivot(String basic, String nonBasic, boolean increase) {
    int basicIndex = this.basicVariables.indexOf(basic);
    int nonBasicIndex = this.nonBasicVariables.indexOf(nonBasic);
    Number a = tableau[basicIndex][nonBasicIndex];
    if (a.isZero()) {
      return false;
    }

    if (increase == a.isPositive()) {
      return canBeIncreased(nonBasic);
    } else {
      return canBeDecreased(nonBasic);
    }
  }

  private void pivot(String basic, String nonBasic, boolean increase) {
    int basicIndex = this.basicVariables.indexOf(basic);
    int nonBasicIndex = this.nonBasicVariables.indexOf(nonBasic);

    if (increase) {
      DeltaRational l = lowerBounds.get(basic);
      values.set(nonBasicIndex, l);
    } else {
      DeltaRational u = upperBounds.get(basic);
      values.set(nonBasicIndex, u);
    }

    // Swap
    this.basicVariables.set(basicIndex, nonBasic);
    this.nonBasicVariables.set(nonBasicIndex, basic);
    Number coefficient = tableau[basicIndex][nonBasicIndex];
    tableau[basicIndex][nonBasicIndex] = ONE().divide(coefficient);
    for (int j = 0; j < columns; j++) {
      if (j != nonBasicIndex) {
        Number result = tableau[basicIndex][j].divide(coefficient.negate());
        tableau[basicIndex][j] = result;
      }
    }

    // Replace in other rows
    for (int i = 0; i < rows; i++) {
      if (i == basicIndex) {
        continue;
      }

      Number m = tableau[i][nonBasicIndex];
      tableau[i][nonBasicIndex] = m.multiply(tableau[basicIndex][nonBasicIndex]);
      for (int j = 0; j < columns; j++) {
        if (j != nonBasicIndex) {
          Number result = tableau[i][j].add(m.multiply(tableau[basicIndex][j]));
          tableau[i][j] = result;
        }
      }
    }
  }

  private boolean canBeIncreased(String variable) {
    if (upperBounds.containsKey(variable)) {
      DeltaRational u = upperBounds.get(variable);
      DeltaRational value = values.get(nonBasicVariables.indexOf(variable));

      return value.lessThan(u);
    }
    return true;
  }

  private boolean canBeDecreased(String variable) {
    if (lowerBounds.containsKey(variable)) {
      DeltaRational l = lowerBounds.get(variable);
      DeltaRational value = values.get(nonBasicVariables.indexOf(variable));

      return value.greaterThan(l);
    }
    return true;
  }

  public DeltaRational getBasicValue(int row) {
    DeltaRational result = DeltaRational.ZERO;
    for (int i = 0; i < columns; i++) {
      DeltaRational summand = values.get(i).scale(tableau[row][i]);
      result = result.add(summand);
    }
    return result;
  }

  public void printTableau() {
    System.out.println("---------------------");
    System.out.print("       ");
    for (int i = 0; i < nonBasicVariables.size(); i++) {
      String variable = nonBasicVariables.get(i);
      System.out.printf(" %s[%s]", variable, values.get(i));
    }
    System.out.println();

    for (int i = 0; i < rows; i++) {
      System.out.printf("%s[%s] ", basicVariables.get(i), getBasicValue(i));
      for (int j = 0; j < columns; j++) {
        System.out.printf("  %s  ", tableau[i][j]);
      }
      System.out.println();
    }

    System.out.println();
    upperBounds.forEach((v, u) -> System.out.printf("%s <= %s%n", v, u));
    lowerBounds.forEach((v, l) -> System.out.printf("%s >= %s%n", v, l));

    System.out.println("---------------------");
  }

  record Violation(String variable, boolean increase) {
  }
}
