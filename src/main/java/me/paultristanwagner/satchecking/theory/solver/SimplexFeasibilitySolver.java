package me.paultristanwagner.satchecking.theory.solver;

import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.SimplexResult;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import java.util.*;

import static me.paultristanwagner.satchecking.theory.arithmetic.Number.*;
import static me.paultristanwagner.satchecking.theory.arithmetic.Number.ZERO;

public class SimplexFeasibilitySolver implements TheorySolver<LinearConstraint> {

  private int rows;
  private int columns;
  private Number[][] tableau;

  private List<String> basicVariables;
  private List<String> nonBasicVariables;
  private List<Number> values;

  private Map<String, Number> lowerBounds;
  private Map<String, Number> upperBounds;

  private final List<LinearConstraint> constraints = new ArrayList<>();

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
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
      values.add(ZERO());
    }

    // Initialize slack variables, tableau and constraints
    basicVariables = new ArrayList<>();
    tableau = new Number[constraints.size()][variableSet.size()];
    for (int i = 0; i < constraints.size(); i++) {
      String slackName = "s" + i;
      LinearConstraint constraint = constraints.get(i);
      basicVariables.add(slackName);

      for (int j = 0; j < variableSet.size(); j++) {
        String variable = variables.get(j);
        tableau[i][j] = constraint.getCoefficients().getOrDefault(variable, ZERO());
      }

      if (constraint.getBound() == LinearConstraint.Bound.EQUAL) {
        lowerBounds.put(slackName, constraint.getValue());
        upperBounds.put(slackName, constraint.getValue());
      } else if (constraint.getBound() == LinearConstraint.Bound.UPPER) {
        upperBounds.put(slackName, constraint.getValue());
      } else if (constraint.getBound() == LinearConstraint.Bound.LOWER) {
        lowerBounds.put(slackName, constraint.getValue());
      }
    }

    this.rows = tableau.length;
    this.columns = tableau[0].length;

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

    VariableAssignment variableAssignment = new VariableAssignment();

    for (String variable : variables) {
      Number value;
      if (basicVariables.contains(variable)) {
        value = getBasicValue(basicVariables.indexOf(variable));
      } else {
        value = values.get(nonBasicVariables.indexOf(variable));
      }
      variableAssignment.assign(variable, value);
    }
    return SimplexResult.feasible(variableAssignment);
  }

  private Set<LinearConstraint> calculateExplanation(Violation violation) {
    Set<LinearConstraint> explanation = new HashSet<>();

    int constraintIndex = Integer.parseInt(violation.variable().split("s")[1]);
    LinearConstraint constraint = constraints.get(constraintIndex);

    explanation.add(constraint);

    int basicIndex = basicVariables.indexOf(violation.variable());
    for (int j = 0; j < columns; j++) {
      Number a = tableau[basicIndex][j];
      if (a.isNonZero()) {
        String variable = nonBasicVariables.get(j);
        constraintIndex = Integer.parseInt(variable.split("s")[1]);
        constraint = constraints.get(constraintIndex);

        explanation.add(constraint);
      }
    }

    return explanation;
  }

  private Violation getViolation() {
    Violation result = null;
    for (int i = 0; i < rows; i++) {
      String variable = basicVariables.get(i);
      Number value = getBasicValue(i);
      if (result != null && result.variable().compareTo(variable) < 0) {
        continue;
      }

      if (upperBounds.containsKey(variable)) {
        Number u = upperBounds.get(variable);
        if (value.greaterThan(u)) {
          result = new Violation(variable, false);
        }
      }

      if (lowerBounds.containsKey(variable)) {
        Number l = lowerBounds.get(variable);
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
      Number l = lowerBounds.get(basic);
      values.set(nonBasicIndex, l);
    } else {
      Number u = upperBounds.get(basic);
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
      Number u = upperBounds.get(variable);
      Number value = values.get(nonBasicVariables.indexOf(variable));

      return value.lessThan(u);
    }
    return true;
  }

  private boolean canBeDecreased(String variable) {
    if (lowerBounds.containsKey(variable)) {
      Number l = lowerBounds.get(variable);
      Number value = values.get(nonBasicVariables.indexOf(variable));

      return value.greaterThan(l);
    }
    return true;
  }

  public Number getBasicValue(int row) {
    Number result = ZERO();
    for (int i = 0; i < columns; i++) {
      Number summand = tableau[row][i].multiply(values.get(i));
      result = result.add(summand);
    }
    return result;
  }

  public void printTableau() {
    System.out.println("---------------------");
    System.out.print("       ");
    for (int i = 0; i < nonBasicVariables.size(); i++) {
      String variable = nonBasicVariables.get(i);
      System.out.printf(" %s[%.2f]", variable, values.get(i));
    }
    System.out.println();

    for (int i = 0; i < rows; i++) {
      System.out.printf("%s[%.2f] ", basicVariables.get(i), getBasicValue(i));
      for (int j = 0; j < columns; j++) {
        System.out.printf("  %.2f  ", tableau[i][j]);
      }
      System.out.println();
    }

    System.out.println();
    upperBounds.forEach((v, u) -> System.out.printf("%s <= %.2f%n", v, u));
    lowerBounds.forEach((v, l) -> System.out.printf("%s >= %.2f%n", v, l));

    System.out.println("---------------------");
  }

  record Violation(String variable, boolean increase) {}
}
