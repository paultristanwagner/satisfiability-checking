package me.paultristanwagner.satchecking.theory.solver;

import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.EqualityConstraint;
import me.paultristanwagner.satchecking.theory.TheoryResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class EqualityLogicSolver implements TheorySolver<EqualityConstraint> {

  private final List<EqualityConstraint> constraints;
  private final List<EqualityConstraint> equalities;
  private final List<EqualityConstraint> inequalities;
  private final Map<String, String> pointers;
  private final Map<String, Integer> ranks;

  public EqualityLogicSolver() {
    this.constraints = new LinkedList<>();
    this.equalities = new LinkedList<>();
    this.inequalities = new LinkedList<>();
    this.pointers = new HashMap<>();
    this.ranks = new HashMap<>();
  }

  private void addEquality(EqualityConstraint equality) {
    this.equalities.add(equality);
    union(equality.getLeft(), equality.getRight());
  }

  private void addInequality(EqualityConstraint inequality) {
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

  @Override
  public void addConstraint(EqualityConstraint constraint) {
    this.constraints.add(constraint);

    String lhs = constraint.getLeft();
    String rhs = constraint.getRight();
    this.pointers.putIfAbsent(lhs, lhs);
    this.pointers.putIfAbsent(rhs, rhs);

    this.ranks.putIfAbsent(lhs, 1);
    this.ranks.putIfAbsent(rhs, 1);

    if (constraint.areEqual()) {
      addEquality(constraint);
    } else {
      addInequality(constraint);
    }
  }

  @Override
  public TheoryResult<EqualityConstraint> solve() {
    for (EqualityConstraint inequality : inequalities) {
      String aRoot = find(inequality.getLeft());
      String bRoot = find(inequality.getRight());
      if (aRoot.equals(bRoot)) {
        Set<EqualityConstraint> explanation =
            constructEqualityPath(inequality.getLeft(), inequality.getRight());
        explanation.add(inequality);
        return TheoryResult.unsatisfiable(explanation);
      }
    }

    // todo: Put this code into an extra method
    VariableAssignment assignment = new VariableAssignment();
    Map<String, Integer> rootMapping = new HashMap<>();
    int i = 0;
    for (String variable : pointers.keySet()) {
      String root = find(variable);
      if (rootMapping.containsKey(root)) {
        int v = rootMapping.get(root);
        assignment.assign(variable, v);
      } else {
        assignment.assign(variable, i);
        rootMapping.put(root, i);
        i++;
      }
    }
    return TheoryResult.satisfiable(assignment);
  }

  private Set<EqualityConstraint> constructEqualityPath(String a, String b) {
    Map<String, List<Pair<String, EqualityConstraint>>> neighbors = new HashMap<>();
    Map<String, Boolean> visited = new HashMap<>();
    Map<String, Pair<String, EqualityConstraint>> previous = new HashMap<>();
    for (EqualityConstraint equality : equalities) {
      if (!neighbors.containsKey(equality.getLeft())) {
        neighbors.put(equality.getLeft(), new ArrayList<>());
        visited.put(equality.getLeft(), false);
        previous.put(equality.getLeft(), null);
      }
      if (!neighbors.containsKey(equality.getRight())) {
        neighbors.put(equality.getRight(), new ArrayList<>());
        visited.put(equality.getRight(), false);
        previous.put(equality.getRight(), null);
      }

      neighbors.get(equality.getLeft()).add(Pair.of(equality.getRight(), equality));
      neighbors.get(equality.getRight()).add(Pair.of(equality.getLeft(), equality));
    }

    Queue<String> queue = new LinkedList<>();
    queue.add(a);
    visited.put(a, true);
    while (!queue.isEmpty()) {
      String current = queue.poll();
      if (current.equals(b)) {
        break;
      }

      for (Pair<String, EqualityConstraint> neighbor : neighbors.get(current)) {
        if (!visited.get(neighbor.getLeft())) {
          queue.add(neighbor.getLeft());
          visited.put(neighbor.getLeft(), true);
          previous.put(neighbor.getLeft(), Pair.of(current, neighbor.getRight()));
        }
      }
    }

    Set<EqualityConstraint> path = new HashSet<>();
    String current = b;
    while (!current.equals(a)) {
      Pair<String, EqualityConstraint> p = previous.get(current);
      path.add(p.getRight());
      current = p.getLeft();
    }

    return path;
  }

  private String find(String variable) {
    String current = variable;
    while (!pointers.get(current).equals(current)) {
      current = pointers.get(current);
    }
    return current;
  }

  private void union(String a, String b) {
    String aRoot = find(a);
    String bRoot = find(b);
    if (aRoot.equals(bRoot)) {
      return;
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
  }
}
