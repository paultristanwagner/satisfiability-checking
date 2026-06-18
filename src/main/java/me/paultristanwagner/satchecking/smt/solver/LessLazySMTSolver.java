package me.paultristanwagner.satchecking.smt.solver;

import me.paultristanwagner.satchecking.sat.Clause;
import me.paultristanwagner.satchecking.sat.Literal;
import me.paultristanwagner.satchecking.sat.PartialAssignment;
import me.paultristanwagner.satchecking.sat.solver.DPLLCDCLSolver;
import me.paultristanwagner.satchecking.smt.SMTResult;
import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.Constraint;
import me.paultristanwagner.satchecking.theory.SimplexResult;
import me.paultristanwagner.satchecking.theory.TheoryResult;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LessLazySMTSolver<C extends Constraint> extends SMTSolver<C> {

  @Override
  public SMTResult<C> solve() {
    satSolver.load(cnf.getBooleanStructure());

    // Optimization tracking (see issue #20). For objective-bearing instances we must compare
    // optima across all theory-consistent complete Boolean models rather than returning the
    // first one. The currently wired theory for this solver (QF_EQ) carries no objective, so this
    // path is dormant there; it is kept consistent with FullLazySMTSolver for robustness.
    boolean objectivePresent = false;
    boolean maximizing = true;
    boolean foundOptimizationSolution = false;
    Number bestOptimum = null;
    VariableAssignment bestSolution = null;

    int lastLevel = 0;
    PartialAssignment assignment;
    while ((assignment = satSolver.nextPartialAssignment()) != null) {
      int newLevel = assignment.getDecisionLevel();
      if (newLevel < lastLevel) {
        theorySolver.clear();

        // Re-adding all up-until the new level
        // todo: Even better: just pop the constraint from undone levels
        for (Literal trueLiteral : assignment.getTrueLiterals()) {
          C constraint = cnf.getConstraintLiteralMap().inverse().get(trueLiteral.getName());

          theorySolver.addConstraint(constraint);
        }
      } else {
        List<Literal> trueLiteralsOnCurrentLevel = assignment.getTrueLiteralsOnCurrentLevel();
        for (Literal trueLiteral : trueLiteralsOnCurrentLevel) {
          C constraint = cnf.getConstraintLiteralMap().inverse().get(trueLiteral.getName());

          theorySolver.addConstraint(constraint);
        }
      }
      lastLevel = newLevel;

      TheoryResult<C> theoryResult = theorySolver.solve();
      if (theoryResult.isUnknown()) { // If the theory solver is unknown, we can't do anything
        return SMTResult.unknown();
      }

      boolean optimizationResult =
          theoryResult instanceof SimplexResult simplexResult
              && (simplexResult.isOptimal() || simplexResult.isUnbounded());

      if (optimizationResult && assignment.isComplete()) {
        // An objective is present: do not return on the first theory-SAT complete model; track the
        // best optimum and force the SAT solver to enumerate a different complete model by
        // excluding the current one.
        SimplexResult simplexResult = (SimplexResult) theoryResult;
        if (!objectivePresent) {
          objectivePresent = true;
          maximizing = simplexResult.isOptimal() ? isMaximizing(assignment) : true;
        }

        if (simplexResult.isUnbounded()) {
          foundOptimizationSolution = true;
          bestSolution = simplexResult.getSolution();
          break;
        }

        Number optimum = simplexResult.getOptimum();
        if (bestOptimum == null
            || (maximizing ? optimum.greaterThan(bestOptimum) : optimum.lessThan(bestOptimum))) {
          bestOptimum = optimum;
          bestSolution = simplexResult.getSolution();
        }
        foundOptimizationSolution = true;

        // Exclude the current complete model so enumeration advances.
        Clause blocking = new Clause(blockingLiterals(assignment));
        boolean resolvable = ((DPLLCDCLSolver) satSolver).excludeClause(blocking);
        if (!resolvable) {
          break;
        }
        theorySolver.clear();
        lastLevel = 0;
      } else if (theoryResult.isSatisfiable()) {
        if (assignment.isComplete()) {
          return SMTResult.satisfiable(theoryResult.getSolution());
        }
      } else {
        Set<C> explanation = theoryResult.getExplanation();

        List<Literal> literals = new ArrayList<>();
        for (C equalityConstraint : explanation) {
          String literalName = cnf.getConstraintLiteralMap().get(equalityConstraint);
          literals.add(new Literal(literalName).not());
        }

        Clause clause = new Clause(literals);
        boolean resolvable = ((DPLLCDCLSolver) satSolver).excludeClause(clause);
        if (!resolvable) {
          break;
        }
      }
    }

    if (objectivePresent && foundOptimizationSolution) {
      return SMTResult.satisfiable(bestSolution);
    }

    return SMTResult.unsatisfiable();
  }

  private List<Literal> blockingLiterals(PartialAssignment assignment) {
    List<Literal> literals = new ArrayList<>();
    for (Literal trueLiteral : assignment.getTrueLiterals()) {
      literals.add(new Literal(trueLiteral.getName()).not());
    }
    return literals;
  }

  private boolean isMaximizing(PartialAssignment assignment) {
    for (Literal trueLiteral : assignment.getTrueLiterals()) {
      if (!cnf.getConstraintLiteralMap().inverse().containsKey(trueLiteral.getName())) {
        continue;
      }
      C constraint = cnf.getConstraintLiteralMap().inverse().get(trueLiteral.getName());
      if (constraint
          instanceof me.paultristanwagner.satchecking.theory.LinearConstraint.MaximizingConstraint) {
        return true;
      }
      if (constraint
          instanceof me.paultristanwagner.satchecking.theory.LinearConstraint.MinimizingConstraint) {
        return false;
      }
    }
    return true;
  }
}
