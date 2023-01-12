package me.paultristanwagner.satchecking.smt.solver;

import me.paultristanwagner.satchecking.sat.Assignment;
import me.paultristanwagner.satchecking.sat.Clause;
import me.paultristanwagner.satchecking.sat.Literal;
import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.Constraint;
import me.paultristanwagner.satchecking.theory.TheoryResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FullLazySMTSolver<C extends Constraint> extends SMTSolver<C> {

  @Override
  public VariableAssignment solve() {
    satSolver.load(cnf.getBooleanStructure());

    Assignment assignment;
    while ((assignment = satSolver.nextModel()) != null) {
      List<Literal> trueLiterals = assignment.getTrueLiterals();

      theorySolver.clear();

      Set<C> selectedConstraints = new HashSet<>();
      for (Literal trueLiteral : trueLiterals) {
        C constraint = cnf.getConstraintLiteralMap().inverse().get(trueLiteral.getName());

        selectedConstraints.add(constraint);
      }

      theorySolver.load(selectedConstraints);
      TheoryResult<C> theoryResult = theorySolver.solve();

      if (theoryResult.isSatisfiable()) {
        return theoryResult.getSolution();
      }

      // Exclude explanation
      Set<C> explanation = theoryResult.getExplanation();

      List<Literal> literals = new ArrayList<>();
      for (C constraint : explanation) {
        String literalName = cnf.getConstraintLiteralMap().get(constraint);
        literals.add(new Literal(literalName).not());
      }

      Clause clause = new Clause(literals);
      cnf.getBooleanStructure().learnClause(clause);
    }

    return null;
  }
}
