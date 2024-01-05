package me.paultristanwagner.satchecking.theory.solver;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MaximizingConstraint;
import me.paultristanwagner.satchecking.theory.LinearConstraint.MinimizingConstraint;
import me.paultristanwagner.satchecking.theory.SimplexResult;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.Map.Entry;

import static me.paultristanwagner.satchecking.theory.arithmetic.Number.ONE;
import static me.paultristanwagner.satchecking.theory.arithmetic.Number.ZERO;

public class SimplexOptimizationSolver implements TheorySolver<LinearConstraint> {

  private final List<String> allVariables;
  private final List<String> basicVariables;

  private final Set<String> originalVariables;
  private final Set<String> slackVariables;
  private final Set<String> nonBasicVariables;

  private final BiMap<String, String> substitutions;
  private final Map<String, Number> offsets;

  private final Map<String, Pair<String, String>> unbounded;

  private int rows;
  private int columns;
  private Number[][] tableau;

  private final List<LinearConstraint> originalConstraints;
  private List<LinearConstraint> constraints;
  private LinearConstraint originalObjective;
  private LinearConstraint objective;

  public SimplexOptimizationSolver() {
    this.allVariables = new ArrayList<>();

    this.originalVariables = new HashSet<>();
    this.slackVariables = new HashSet<>();

    this.basicVariables = new ArrayList<>();
    this.nonBasicVariables = new HashSet<>();

    this.originalConstraints = new ArrayList<>();
    this.constraints = new ArrayList<>();

    this.substitutions = HashBiMap.create();
    this.offsets = new HashMap<>();

    this.unbounded = new HashMap<>();
  }

  public void maximize(LinearConstraint f) {
    if (!(f instanceof MaximizingConstraint)) {
      throw new IllegalArgumentException("The objective function must be a maximizing constraint.");
    }

    if (originalObjective != null) {
      throw new IllegalStateException("The objective function has already been set.");
    }

    this.originalObjective = f;
    this.objective = f;
  }

  public void minimize(LinearConstraint f) {
    if (!(f instanceof MinimizingConstraint)) {
      throw new IllegalArgumentException("The objective function must be a minimizing constraint.");
    }

    if (originalObjective != null) {
      throw new IllegalStateException("The objective function has already been set.");
    }

    this.originalObjective = f;
    this.objective = new LinearConstraint(f);

    for (String variable : objective.getVariables()) {
      objective.setCoefficient(variable, objective.getCoefficients().get(variable).negate());
    }
  }

  @Override
  public void clear() {
    this.allVariables.clear();

    this.originalVariables.clear();
    this.slackVariables.clear();

    this.basicVariables.clear();
    this.nonBasicVariables.clear();

    this.originalConstraints.clear();
    this.constraints.clear();

    this.substitutions.clear();
    this.offsets.clear();

    this.unbounded.clear();

    this.originalObjective = null;
    this.objective = null;
  }

  @Override
  public SimplexResult solve() {
    this.originalConstraints.addAll(constraints);

    // Collect variables
    Set<String> tempSet = new HashSet<>();
    for (LinearConstraint constraint : constraints) {
      originalVariables.addAll(constraint.getVariables());

      tempSet.addAll(constraint.getVariables());
      nonBasicVariables.addAll(constraint.getVariables());
    }
    allVariables.addAll(tempSet);

    // Infer bounds
    Pair<Map<String, LinearConstraint>, Map<String, LinearConstraint>> inferedBounds =
        inferBounds();

    SimplexResult result = checkBoundsConsistency(inferedBounds);
    if (result != null && !result.isFeasible()) {
      return result;
    }

    List<String> withoutLowerBounds = new ArrayList<>(allVariables);
    withoutLowerBounds.removeAll(inferedBounds.getLeft().keySet());

    // Transform constraints, where a single variable has a bound other than zero
    transformOffsetVariables(inferedBounds);

    // Replace unbounded variables
    replaceUnboundedVariables(withoutLowerBounds);

    // Add slack variables
    for (LinearConstraint constraint : constraints) {
      String slackVariable = freshVariable("slack");
      slackVariables.add(slackVariable);
      allVariables.add(slackVariable);
      basicVariables.add(slackVariable);
    }

    // Sort variables
    allVariables.sort(
        (o1, o2) -> {
          if (slackVariables.contains(o1) && slackVariables.contains(o2)) {
            return o1.compareTo(o2);
          } else if (slackVariables.contains(o1)) {
            return 1;
          } else if (slackVariables.contains(o2)) {
            return -1;
          }
          return o1.compareTo(o2);
        });

    // Create tableau
    createTableau();

    // First phase, find feasible solution
    while (true) {
      int violatingRow = -1;
      for (int i = 0; i < basicVariables.size(); i++) {
        Number value = tableau[i + 1][allVariables.size()];

        if (value.lessThan(ZERO())) {
          violatingRow = i + 1;
          break;
        }
      }

      if (violatingRow == -1) {
        VariableAssignment solution = calculateSolution();
        result = SimplexResult.feasible(solution);
        break;
      }

      // check for negative coefficient
      int pivotColumn = -1;
      for (int j = 0; j < allVariables.size(); j++) {
        if (tableau[violatingRow][j].lessThan(ZERO())) {
          pivotColumn = j;
          break;
        }
      }

      if (pivotColumn == -1) {
        Set<LinearConstraint> explanation = calculateExplanation(violatingRow);
        result = SimplexResult.infeasible(explanation);

        return result;
      }

      pivot(violatingRow, pivotColumn);
    }

    // Second phase, maximize
    if (objective == null) {
      return result;
    }

    while (true) {
      // Find negative entry in objective row
      int pivotColumn = -1;

      Number smallestValue = null;
      for (int j = 0; j < allVariables.size(); j++) {
        Number f_j = tableau[0][j];
        if(f_j.lessThan(ZERO()) && (smallestValue == null || f_j.lessThan(smallestValue))) {
          pivotColumn = j;
          smallestValue = f_j;
        }
      }

      if (pivotColumn == -1) {
        VariableAssignment solution = calculateSolution();

        Number optimum = calculateObjectiveValue();

        result = SimplexResult.optimal(solution, optimum);

        return result;
      }

      // Find row with the smallest ratio
      int pivotRow = -1;
      Number minRatio = null;
      for (int i = 1; i < rows; i++) {
        if (tableau[i][pivotColumn].lessThanOrEqual(ZERO())) {
          continue;
        }

        Number ratio = tableau[i][columns - 1].divide(tableau[i][pivotColumn]);
        if (ratio.isNonNegative() && (minRatio == null || ratio.lessThan(minRatio))) {
          pivotRow = i;
          minRatio = ratio;
        }
      }

      if (pivotRow == -1) {
        VariableAssignment solution = calculateSolution();
        Set<LinearConstraint> allConstraints = new HashSet<>(originalConstraints);
        result = SimplexResult.unbounded(solution, allConstraints);
        return result;
      }

      pivot(pivotRow, pivotColumn);
    }
  }

  private void pivot(int pivotRow, int pivotColumn) {
    String leaving = basicVariables.get(pivotRow - 1);
    String entering = allVariables.get(pivotColumn);

    nonBasicVariables.remove(entering);
    nonBasicVariables.add(leaving);
    basicVariables.set(pivotRow - 1, entering);

    Number temp = tableau[pivotRow][pivotColumn];

    for (int j = 0; j < allVariables.size() + 1; j++) {
      tableau[pivotRow][j] = tableau[pivotRow][j].divide(temp);
    }

    for (int i = 0; i < constraints.size() + 1; i++) {
      if (i == pivotRow) continue;

      Number factor = tableau[i][pivotColumn].divide(tableau[pivotRow][pivotColumn]);
      for (int j = 0; j < allVariables.size() + 1; j++) {
        Number minuend = factor.multiply(tableau[pivotRow][j]);
        tableau[i][j] = tableau[i][j].subtract(minuend);
      }
    }
  }

  private Pair<Map<String, LinearConstraint>, Map<String, LinearConstraint>> inferBounds() {
    Map<String, LinearConstraint> lowerBounds = new HashMap<>();
    Map<String, LinearConstraint> upperBounds = new HashMap<>();

    List<LinearConstraint> keptConstraints = new ArrayList<>();
    for (String variable : allVariables) {
      Iterator<LinearConstraint> iterator = constraints.iterator();
      while (iterator.hasNext()) {
        LinearConstraint constraint = iterator.next();
        if (constraint.getCoefficients().size() != 1) {
          iterator.remove();
          keptConstraints.add(constraint);
          continue;
        }

        String constraintVariable = constraint.getVariables().iterator().next();
        if (!variable.equals(constraintVariable)) continue;

        Number bound = constraint.getBoundOn(constraintVariable);

        if (constraint.getBound()
            != LinearConstraint.Bound.UPPER
            == constraint.getCoefficients().get(constraintVariable).greaterThan(ZERO())) {
          if (!lowerBounds.containsKey(variable)
              || lowerBounds.get(variable).getBoundOn(variable).lessThan(ZERO())) {
            lowerBounds.put(variable, constraint);
          }
        } else {
          if (!upperBounds.containsKey(variable)
              || upperBounds.get(variable).getBoundOn(variable).greaterThan(bound)) {
            upperBounds.put(variable, constraint);
          }
        }
      }
    }

    keptConstraints.addAll(lowerBounds.values());
    keptConstraints.addAll(upperBounds.values());
    constraints = keptConstraints;

    return Pair.of(lowerBounds, upperBounds);
  }

  private SimplexResult checkBoundsConsistency(
      Pair<Map<String, LinearConstraint>, Map<String, LinearConstraint>> inferredBounds) {
    Map<String, LinearConstraint> lowerBounds = inferredBounds.getLeft();
    Map<String, LinearConstraint> upperBounds = inferredBounds.getRight();
    for (String variable : allVariables) {
      LinearConstraint lowerBound = lowerBounds.get(variable);
      LinearConstraint upperBound = upperBounds.get(variable);

      // todo: check if this is right

      if(lowerBound == null || upperBound == null) continue;

      Number lowerBoundValue = lowerBounds.get(variable).getBoundOn(variable);
      Number upperBoundValue = upperBounds.get(variable).getBoundOn(variable);

      if (lowerBoundValue.greaterThan(upperBoundValue)) {
        Set<LinearConstraint> explanation = new HashSet<>();
        explanation.add(lowerBound.getRoot());
        explanation.add(upperBound.getRoot());
        return SimplexResult.infeasible(explanation);
      }
    }

    return null;
  }

  private void transformOffsetVariables(
      Pair<Map<String, LinearConstraint>, Map<String, LinearConstraint>> inferredBounds) {
    Map<String, LinearConstraint> lowerBounds = inferredBounds.getLeft();
    for (ListIterator<String> iterator = allVariables.listIterator(); iterator.hasNext(); ) {
      String variable = iterator.next();
      if (!lowerBounds.containsKey(variable)) continue;

      LinearConstraint lowerBound = lowerBounds.get(variable);
      Number bound = lowerBound.getBoundOn(variable);

      if (bound.isZero()) continue;

      String substitute = freshVariable("subst");
      offsets.put(substitute, bound);
      substitutions.put(variable, substitute);

      iterator.remove();
      iterator.add(substitute);

      nonBasicVariables.remove(variable);
      nonBasicVariables.add(substitute);

      for (int i = 0; i < constraints.size(); i++) {
        LinearConstraint linearConstraint = constraints.get(i);
        if (linearConstraint.getCoefficients().containsKey(variable)) {
          LinearConstraint offsetConstraint = linearConstraint.offset(variable, substitute, bound);
          constraints.set(i, offsetConstraint);
        }
      }

      if (objective != null) {
        objective = objective.offset(variable, substitute, bound);
      }
    }
  }

  private void replaceUnboundedVariables(List<String> withoutLowerBounds) {
    for (String unboundedVariable : withoutLowerBounds) {
      String positive = freshVariable("p_" + unboundedVariable);
      String negative = freshVariable("n_" + unboundedVariable);
      unbounded.put(unboundedVariable, Pair.of(positive, negative));

      allVariables.add(positive);
      allVariables.add(negative);
      allVariables.remove(unboundedVariable);

      nonBasicVariables.add(positive);
      nonBasicVariables.add(negative);
      nonBasicVariables.remove(unboundedVariable);

      for (int i = 0; i < constraints.size(); i++) {
        LinearConstraint linearConstraint = constraints.get(i);
        if (linearConstraint.getCoefficients().containsKey(unboundedVariable)) {
          LinearConstraint positiveNegative =
              linearConstraint.positiveNegativeSubstitute(unboundedVariable, positive, negative);
          constraints.set(i, positiveNegative);
        }
      }

      if (objective != null) {
        if (objective.getCoefficients().containsKey(unboundedVariable)) {
          objective = objective.positiveNegativeSubstitute(unboundedVariable, positive, negative);
        }
      }
    }
  }

  private void createTableau() {
    rows = constraints.size() + 1;
    columns = allVariables.size() + 1;

    this.tableau = new Number[rows][columns];

    // fill tableau
    for(int i = 0; i < rows; i++) {
      for(int j = 0; j < columns; j++) {
        tableau[i][j] = ZERO();
      }
    }

    // Enter target function to tableau
    if (objective != null) {
      for (int i = 0; i < allVariables.size(); i++) {
        String variable = allVariables.get(i);
        tableau[0][i] = objective.getCoefficients().getOrDefault(variable, ZERO()).negate();
      }
    }

    for (int i = 0; i < constraints.size(); i++) {
      LinearConstraint constraint = constraints.get(i);

      if (constraint.getBound() == LinearConstraint.Bound.LOWER) {
        for (int j = 0; j < allVariables.size(); j++) {
          String variable = allVariables.get(j);
          tableau[i + 1][j] = constraint.getCoefficients().getOrDefault(variable, ZERO()).negate();
        }
        tableau[i + 1][allVariables.size()] = constraint.getValue().negate();
      } else {
        for (int j = 0; j < allVariables.size(); j++) {
          String variable = allVariables.get(j);
          tableau[i + 1][j] = constraint.getCoefficients().getOrDefault(variable, ZERO());
        }
        tableau[i + 1][allVariables.size()] = constraint.getValue();
      }
      tableau[i + 1][nonBasicVariables.size() + i] = ONE();
    }
  }

  private Number getPureValue(String variable) {
    if (nonBasicVariables.contains(variable)) {
      return ZERO();
    } else {
      int basisIndex = basicVariables.indexOf(variable);
      return tableau[basisIndex + 1][columns - 1];
    }
  }

  private Number getValue(String variable) {
    Number value;
    if (substitutions.containsKey(variable)) {
      String substitute = substitutions.get(variable);
      Number offset = offsets.get(substitute);
      value = offset.add(getPureValue(substitute));
    } else if (unbounded.containsKey(variable)) {
      Number positive = getPureValue(unbounded.get(variable).getLeft());
      Number negative = getPureValue(unbounded.get(variable).getRight());
      value = positive.subtract(negative);
    } else {
      value = getPureValue(variable);
    }

    return value;
  }

  private VariableAssignment calculateSolution() {
    VariableAssignment assignment = new VariableAssignment();

    for (String variable : originalVariables) {
      Number value = getValue(variable);
      assignment.assign(variable, value);
    }

    return assignment;
  }

  private Number calculateObjectiveValue() {
    Number objectiveValue = ZERO();

    for (Entry<String, Number> pair : originalObjective.getCoefficients().entrySet()) {
      Number value = getValue(pair.getKey());
      objectiveValue = objectiveValue.add(pair.getValue().multiply(value));
    }

    return objectiveValue;
  }

  // todo: clean up this method
  private Set<LinearConstraint> calculateExplanation(int pivotRow) {
    Set<LinearConstraint> explanation = new HashSet<>();

    explanation.add(constraints.get(pivotRow - 1).getRoot());
    for (int j = 0; j < columns - 1; j++) {
      String variable = allVariables.get(j);

      if (tableau[pivotRow][j].isZero()) {
        continue;
      }

      if (j >= nonBasicVariables.size()) {
        int basisIndex = j - nonBasicVariables.size();
        explanation.add(constraints.get(basisIndex).getRoot());
      } else {
        String actual = substitutions.inverse().getOrDefault(variable, variable);
        for (LinearConstraint originalConstraint : originalConstraints) {
          if (originalConstraint.getBound() == LinearConstraint.Bound.UPPER) continue;
          // if (originalConstraint.getCoefficients().size() != 1) continue;

          String onlyVariable = originalConstraint.getCoefficients().keySet().iterator().next();
          if (onlyVariable.equals(actual)) {
            explanation.add(originalConstraint.getRoot());
          }
        }
      }
    }

    return explanation;
  }

  public void printTableau() {
    for (int i = 0; i < allVariables.size() + 1; i++) {
      System.out.print("-----------");
    }
    System.out.println();

    System.out.println("basic variables: " + basicVariables);

    for (String allVariable : allVariables) {
      System.out.printf("%1$10s ", allVariable);
    }
    System.out.printf("%1$10s", "b");
    System.out.println();

    for (Number[] row : tableau) {
      for (Number d : row) {
        System.out.printf(d.toString() + " ");
      }
      System.out.println();
    }

    for (int i = 0; i < allVariables.size() + 1; i++) {
      System.out.print("-----------");
    }
    System.out.println();
  }

  @Override
  public void addConstraint(LinearConstraint constraint) {
    if (constraint instanceof MaximizingConstraint maximizingConstraint) {
      maximize(maximizingConstraint);
      return;
    } else if (constraint instanceof MinimizingConstraint minimizingConstraint) {
      minimize(minimizingConstraint);
      return;
    }

    if (constraint.getBound() == LinearConstraint.Bound.EQUAL) {
      LinearConstraint first = new LinearConstraint();
      LinearConstraint second = new LinearConstraint();
      first.setDerivedFrom(constraint);
      second.setDerivedFrom(constraint);

      first.setBound(LinearConstraint.Bound.UPPER);
      second.setBound(LinearConstraint.Bound.LOWER);

      constraint
          .getCoefficients()
          .forEach(
              (variable, coefficient) -> {
                first.setCoefficient(variable, coefficient);
                second.setCoefficient(variable, coefficient);
              });
      first.setValue(constraint.getValue());
      second.setValue(constraint.getValue());

      constraints.add(first);
      constraints.add(second);
    } else {
      constraints.add(constraint);
    }
  }

  private String freshVariable(String prefix) {
    int i = 0;
    String fresh;
    do {
      fresh = prefix + i;
      i += 1;
    } while (allVariables.contains(fresh));

    return fresh;
  }
  
  public LinearConstraint getOriginalObjective() {
    return originalObjective;
  }
  
  public boolean hasObjectiveFunction() {
    return objective != null;
  }
  
  public boolean isMaximization() {
    return originalObjective instanceof MaximizingConstraint;
  }
  
  public boolean isMinimization() {
    return originalObjective instanceof MinimizingConstraint;
  }
}
