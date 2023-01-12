package me.paultristanwagner.satchecking.sat.solver;

import me.paultristanwagner.satchecking.sat.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import static me.paultristanwagner.satchecking.sat.Result.SAT;
import static me.paultristanwagner.satchecking.sat.Result.UNSAT;

public class DPLLCDCLSolver implements SATSolver {

  private static final boolean DEFAULT_DECISION_VALUE = false;

  private CNF cnf;
  private Assignment assignment;

  private Map<Literal, List<Clause>> watchedInMap;
  private Map<Clause, WatchedLiteralPair> watchedLiteralsMap;

  private Clause conflictingClause;
  private final Queue<Pair<Clause, Literal>> unitClauses = new ArrayDeque<>();

  @Override
  public void load(CNF cnf) {
    this.cnf = cnf;
    this.assignment = new Assignment();
    this.watchedInMap = new HashMap<>();
    this.watchedLiteralsMap = new HashMap<>();

    initializeWatchedLiterals();
  }

  private void initializeWatchedLiterals() {
    for (Clause clause : cnf.getClauses()) {
      initializeWatchedLiterals(clause);
    }
  }

  private void initializeWatchedLiterals(Clause clause) {
    WatchedLiteralPair wlp = new WatchedLiteralPair(clause, assignment);
    watchedLiteralsMap.put(clause, wlp);

    for (Literal literal : wlp.getWatched()) {
      List<Clause> watchedIn = watchedInMap.getOrDefault(literal, new ArrayList<>());
      watchedIn.add(clause);
      watchedInMap.put(literal, watchedIn);
    }

    Literal unitLiteral = wlp.getUnitLiteral(assignment);
    if (unitLiteral != null) {
      unitClauses.add(Pair.of(clause, unitLiteral));
    }
  }

  @Override
  public Assignment nextModel() {
    if (!assignment.isEmpty()) {
      unitClauses.clear();

      conflictingClause = assignment.not(); // blocking last assignment
      resolveConflict();
    }

    Result result = check();
    if (!result.isSatisfiable()) {
      return null;
    }
    return result.getAssignment();
  }

  public PartialAssignment nextPartialAssignment() {
    if (assignment.isEmpty()) {
      while (!bcp()) {
        if (!resolveConflict()) {
          return null;
        }
      }

      if (assignment.fits(cnf)) {
        return PartialAssignment.complete(assignment);
      } else {
        return PartialAssignment.incomplete(assignment);
      }
    }

    decide(assignment);
    while (!bcp()) {
      if (!resolveConflict()) {
        return null;
      }
    }

    if (assignment.fits(cnf)) {
      return PartialAssignment.complete(assignment);
    } else {
      return PartialAssignment.incomplete(assignment);
    }
  }

  public static Result check(CNF cnf) {
    DPLLCDCLSolver solver = new DPLLCDCLSolver();
    solver.load(cnf);
    return solver.check();
  }

  private Result check() {
    while (!bcp()) {
      if (!resolveConflict()) {
        return UNSAT;
      }
    }

    while (true) {
      if (assignment.fits(cnf)) {
        return SAT(assignment);
      }
      decide(assignment);
      while (!bcp()) {
        if (!resolveConflict()) {
          return UNSAT;
        }
      }
    }
  }

  private void decide(Assignment assignment) {
    Optional<Literal> unassignedOptional =
        cnf.getLiterals().stream().filter(lit -> !assignment.assigns(lit)).findFirst();
    if (unassignedOptional.isEmpty()) {
      throw new IllegalStateException("Cannot decide because all literals are assigned");
    }

    Literal literal = unassignedOptional.get();

    // Assign the literal with the default decision value and update the watched literals
    assignment.assign(literal, DEFAULT_DECISION_VALUE);
    updateWatchedLiterals(new Literal(literal.getName(), DEFAULT_DECISION_VALUE));
  }

  private boolean bcp() {
    // While we can find unit clauses
    while (true) {
      boolean foundUnitClause = false;

      if (conflictingClause != null) {
        return false;
      } else {
        if (!unitClauses.isEmpty()) {
          foundUnitClause = true;
          Pair<Clause, Literal> pair = unitClauses.poll();
          Clause unitClause = pair.getLeft();
          Literal literal = pair.getRight();
          if (assignment.assigns(literal)) {
            continue;
          }

          assignment.propagate(literal, unitClause);
          updateWatchedLiterals(literal.not());
        }
      }

      if (!foundUnitClause) {
        return true;
      }
    }
  }

  private boolean resolveConflict() {
    if (assignment.getDecisionLevel() == 0) {
      return false;
    }

    Clause currentClause = conflictingClause;
    conflictingClause = null;
    Literal assertingLiteral;
    while ((assertingLiteral = currentClause.isAsserting(assignment)) == null) {
      LiteralAssignment literalAssignment =
          assignment.getLastAssignmentOnCurrentLevel(currentClause);
      Clause antcedent = literalAssignment.getAntecedent();
      currentClause = resolution(currentClause, antcedent, literalAssignment);
    }

    int targetLevel = 0;
    if (currentClause.getLiterals().size() > 1) {
      // search for second-highest assignment level
      for (Literal literal : currentClause.getLiterals()) {
        int level = assignment.getAssignmentLevelOf(literal);
        if (level != assignment.getDecisionLevel() && level > targetLevel) {
          targetLevel = level;
        }
      }
    }

    // Backjumping
    while (assignment.getDecisionLevel() > targetLevel) {
      assignment.undoLastDecision();
    }

    unitClauses.clear();
    unitClauses.add(Pair.of(currentClause, assertingLiteral));
    conflictingClause = null;

    learnClause(currentClause);

    return true;
  }

  // todo: Experimental
  public boolean excludeClause(Clause clause) {
    learnClause(clause);

    conflictingClause = clause;
    return resolveConflict();
  }

  private void learnClause(Clause clause) {
    cnf.learnClause(clause);

    // Initialize watched literals for new clause
    WatchedLiteralPair wlp = new WatchedLiteralPair(clause, assignment);
    watchedLiteralsMap.put(clause, wlp);

    for (Literal literal : wlp.getWatched()) {
      List<Clause> watchedIn = watchedInMap.getOrDefault(literal, new ArrayList<>());
      watchedIn.add(clause);
      watchedInMap.put(literal, watchedIn);
    }
  }

  private Clause resolution(Clause clause1, Clause clause2, LiteralAssignment la) {
    List<Literal> literals = new ArrayList<>();
    for (Literal literal : clause1.getLiterals()) {
      if (!literal.getName().equals(la.getLiteralName()) || literal.isNegated() != la.getValue()) {
        if (!literals.contains(literal)) {
          literals.add(literal);
        }
      }
    }
    for (Literal literal : clause2.getLiterals()) {
      if (!literal.getName().equals(la.getLiteralName()) || literal.isNegated() == la.getValue()) {
        if (!literals.contains(literal)) {
          literals.add(literal);
        }
      }
    }
    return new Clause(literals);
  }

  private void updateWatchedLiterals(Literal literal) {
    List<Clause> watchedInCopy =
        new ArrayList<>(watchedInMap.getOrDefault(literal, new ArrayList<>()));
    for (Clause clause : watchedInCopy) {
      updateWatchedLiterals(literal, clause);
    }
  }

  private void updateWatchedLiterals(Literal literal, Clause clause) {
    WatchedLiteralPair wlp = watchedLiteralsMap.get(clause);
    Literal replacement = wlp.attemptReplace(literal, assignment);
    if (replacement != null) {
      watchedInMap.get(literal).remove(clause);

      List<Clause> watchedIn = watchedInMap.getOrDefault(replacement, new ArrayList<>());
      watchedIn.add(clause);
      watchedInMap.put(replacement, watchedIn);
      return;
    }

    if (wlp.isConflicting(assignment)) {
      conflictingClause = clause;
    } else {
      Literal unitLiteral = wlp.getUnitLiteral(assignment);
      if (unitLiteral != null) {
        unitClauses.add(Pair.of(clause, unitLiteral));
      }
    }
  }
}
