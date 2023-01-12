package me.paultristanwagner.satchecking.theory.solver;

import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.EqualityFunctionConstraint;
import me.paultristanwagner.satchecking.theory.EqualityFunctionConstraint.Function;
import me.paultristanwagner.satchecking.theory.TheoryResult;

import java.util.*;

public class EqualityFunctionSolver implements TheorySolver<EqualityFunctionConstraint> {

  private final List<EqualityFunctionConstraint> constraints;
  private final List<EqualityFunctionConstraint> equalities;
  private final List<EqualityFunctionConstraint> inequalities;
  private final Map<Function, Function> pointers;
  private final Map<Function, Integer> ranks;

  private final Map<String, Integer> parametersByName;
  private final Map<String, Set<Function>> functionsByName;

  public EqualityFunctionSolver() {
    this.constraints = new LinkedList<>();
    this.equalities = new LinkedList<>();
    this.inequalities = new LinkedList<>();
    this.pointers = new HashMap<>();
    this.ranks = new HashMap<>();

    this.parametersByName = new HashMap<>();
    this.functionsByName = new HashMap<>();
  }

  private void addEquality(EqualityFunctionConstraint equality) {
    this.equalities.add(equality);
    union(equality.getLeft(), equality.getRight());
  }

  private void addInequality(EqualityFunctionConstraint inequality) {
    this.inequalities.add(inequality);
  }

  @Override
  public void clear() {
    this.constraints.clear();
    this.equalities.clear();
    this.inequalities.clear();
    this.pointers.clear();
    this.ranks.clear();
  }

  private void addFunction(Function function) {
    if (parametersByName.containsKey(function.name())
        && parametersByName.get(function.name()) != function.parameters().size()) {
      throw new IllegalArgumentException(
          "Function "
              + function.name()
              + " has already been defined with a different number of parameters");
    } else {
      parametersByName.put(function.name(), function.parameters().size());
    }

    Set<Function> functions = this.functionsByName.getOrDefault(function.name(), new HashSet<>());
    functions.add(function);
    functionsByName.put(function.name(), functions);

    this.pointers.putIfAbsent(function, function);
    this.ranks.putIfAbsent(function, 1);
  }

  @Override
  public void addConstraint(EqualityFunctionConstraint constraint) {
    this.constraints.add(constraint);

    Function lhs = constraint.getLeft();
    Function rhs = constraint.getRight();

    Set<Function> lhsSubTerms = lhs.getAllSubTerms();
    Set<Function> rhsSubTerms = rhs.getAllSubTerms();
    lhsSubTerms.forEach(this::addFunction);
    rhsSubTerms.forEach(this::addFunction);

    if (constraint.areEqual()) {
      addEquality(constraint);
    } else {
      addInequality(constraint);
    }
  }

  // todo: Check for consistency of function parameter count
  // todo: List of function terms by names: name -> list<function>

  private boolean consolidate() {
    boolean changed = false;
    for (String functionName : functionsByName.keySet()) {
      List<Function> functions = new ArrayList<>(functionsByName.get(functionName));
      int numParameters = parametersByName.get(functionName);

      for (int i = 0; i < functions.size(); i++) {
        otherFunctionLoop:
        for (int j = i + 1; j < functions.size(); j++) {
          Function a = functions.get(i);
          Function b = functions.get(j);

          for (int k = 0; k < numParameters; k++) {
            Function aParam = a.parameters().get(k);
            Function bParam = b.parameters().get(k);

            Function aParamRoot = this.pointers.get(aParam);
            Function bParamRoot = this.pointers.get(bParam);

            if (!aParamRoot.equals(bParamRoot)) {
              continue otherFunctionLoop;
            }
          }

          changed = changed || union(a, b);
        }
      }
    }

    return changed;
  }

  @Override
  public TheoryResult<EqualityFunctionConstraint> solve() {
    boolean changed;
    do {
      changed = consolidate();
    } while (changed);

    for (EqualityFunctionConstraint inequality : inequalities) {
      Function aRoot = find(inequality.getLeft());
      Function bRoot = find(inequality.getRight());
      if (aRoot.equals(bRoot)) {
        // todo: Compute a minimal subset
        Set<EqualityFunctionConstraint> explanation = new HashSet<>(equalities);
        explanation.add(inequality);
        return TheoryResult.unsatisfiable(explanation);
      }
    }

    // todo: Put this code into an extra method
    VariableAssignment assignment = new VariableAssignment();
    Map<Function, Integer> rootMapping = new HashMap<>();
    int i = 0;
    for (Function variable : pointers.keySet()) {
      Function root = find(variable);
      if (rootMapping.containsKey(root)) {
        int v = rootMapping.get(root);
        assignment.assign(variable.toString(), v);
      } else {
        assignment.assign(variable.toString(), i);
        rootMapping.put(root, i);
        i++;
      }
    }
    return TheoryResult.satisfiable(assignment);
  }

  private Function find(Function function) {
    Function current = function;
    while (!pointers.get(current).equals(current)) {
      current = pointers.get(current);
    }
    return current;
  }

  private boolean union(Function a, Function b) {
    Function aRoot = find(a);
    Function bRoot = find(b);
    if (aRoot.equals(bRoot)) {
      return false;
    }

    int aRank = ranks.get(aRoot);
    int bRank = ranks.get(bRoot);
    if (aRank > bRank) {
      pointers.put(bRoot, aRoot);
      ranks.put(aRoot, aRank + bRank);
    } else {
      pointers.put(aRoot, bRoot);
      ranks.put(bRoot, aRank + bRank);
    }

    return true;
  }
}
