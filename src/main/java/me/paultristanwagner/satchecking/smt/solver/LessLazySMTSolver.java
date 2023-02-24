package me.paultristanwagner.satchecking.smt.solver;

import me.paultristanwagner.satchecking.sat.Clause;
import me.paultristanwagner.satchecking.sat.Literal;
import me.paultristanwagner.satchecking.sat.PartialAssignment;
import me.paultristanwagner.satchecking.sat.solver.DPLLCDCLSolver;
import me.paultristanwagner.satchecking.smt.SMTResult;
import me.paultristanwagner.satchecking.theory.Constraint;
import me.paultristanwagner.satchecking.theory.TheoryResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LessLazySMTSolver<C extends Constraint> extends SMTSolver<C> {

  @Override
  public SMTResult<C> solve() {
    satSolver.load(cnf.getBooleanStructure());

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

      if (theoryResult.isSatisfiable()) {
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

    return SMTResult.unsatisfiable();
  }
}
