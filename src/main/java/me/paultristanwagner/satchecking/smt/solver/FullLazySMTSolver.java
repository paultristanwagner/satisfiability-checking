package me.paultristanwagner.satchecking.smt.solver;

import me.paultristanwagner.satchecking.sat.Assignment;
import me.paultristanwagner.satchecking.sat.Clause;
import me.paultristanwagner.satchecking.sat.Literal;
import me.paultristanwagner.satchecking.smt.SMTResult;
import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.Constraint;
import me.paultristanwagner.satchecking.theory.SimplexResult;
import me.paultristanwagner.satchecking.theory.TheoryResult;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FullLazySMTSolver<C extends Constraint> extends SMTSolver<C> {

  @Override
  public SMTResult<C> solve() {
    satSolver.load(cnf.getBooleanStructure());

    // Tracking for the optimization case: we cannot return on the first theory-satisfiable
    // Boolean model, because a different Boolean model may yield a better optimum. We therefore
    // enumerate all theory-consistent Boolean models and keep the best optimum seen.
    boolean objectivePresent = false;
    boolean maximizing = false;
    boolean foundOptimizationSolution = false;
    boolean unbounded = false;
    Number bestOptimum = null;
    VariableAssignment bestSolution = null;

    Assignment assignment;
    while ((assignment = satSolver.nextModel()) != null) {
      theorySolver.clear();

      // Build the selected constraints by inspecting the assignment of EVERY atom literal. A
      // TRUE-assigned atom is asserted as-is; a FALSE-assigned atom is asserted as its negation
      // (issue #9) when the constraint is negatable (exact for equality logics), otherwise skipped
      // to preserve the sound positive-only behavior for theories without exact atom negation.
      Set<C> selectedConstraints = new HashSet<>();
      for (var entry : cnf.getConstraintLiteralMap().entrySet()) {
        C constraint = entry.getKey();
        String name = entry.getValue();

        if (!assignment.assigns(new Literal(name))) {
          continue;
        }

        boolean value = assignment.getValue(name);
        if (value) {
          selectedConstraints.add(constraint);
        } else if (constraint.isNegatable()) {
          @SuppressWarnings("unchecked")
          C negated = (C) constraint.negate();
          selectedConstraints.add(negated);
        }
      }

      theorySolver.load(selectedConstraints);
      TheoryResult<C> theoryResult = theorySolver.solve();
      if (theoryResult.isUnknown()) { // If the theory solver is unknown, we can't do anything
        return SMTResult.unknown();
      }

      // Determine whether this run carries an optimization objective. A SimplexResult that is
      // optimal (or unbounded) signals that an objective function was attached to the model.
      boolean optimizationResult =
          theoryResult instanceof SimplexResult simplexResult
              && (simplexResult.isOptimal() || simplexResult.isUnbounded());

      if (optimizationResult) {
        SimplexResult simplexResult = (SimplexResult) theoryResult;

        if (!objectivePresent) {
          objectivePresent = true;
          maximizing = isMaximizing(selectedConstraints);
        }

        if (simplexResult.isUnbounded()) {
          // The objective is unbounded for this model; no finite optimum can beat it.
          unbounded = true;
          foundOptimizationSolution = true;
          bestSolution = simplexResult.getSolution();
          // Keep enumerating is pointless once unbounded; the global optimum is unbounded.
          break;
        }

        Number optimum = simplexResult.getOptimum();
        if (bestOptimum == null
            || (maximizing
                ? optimum.greaterThan(bestOptimum)
                : optimum.lessThan(bestOptimum))) {
          bestOptimum = optimum;
          bestSolution = simplexResult.getSolution();
        }
        foundOptimizationSolution = true;

        // Do NOT return: continue enumerating alternative Boolean models. nextModel() blocks the
        // current complete assignment, so the enumeration terminates.
        continue;
      }

      if (theoryResult.isSatisfiable()) {
        // No optimization objective: keep the original behavior and return on the first
        // theory-satisfiable Boolean model.
        return SMTResult.satisfiable(theoryResult.getSolution());
      }

      // Exclude explanation. The explanation may reference negated atoms (constraints produced by
      // negate() that are NOT keys of the constraint map). For each explanation constraint we must
      // recover the literal of the POSITIVE atom in the map and use the correct polarity:
      //  - constraint present in map  -> it was assigned TRUE -> block with its negative literal.
      //  - constraint is a negation of a mapped atom -> the atom was assigned FALSE -> block with
      //    its positive literal.
      // Mapping to neither must not happen for the equality logics; we log and skip to avoid
      // producing a Literal(null) that would corrupt the blocking clause and break termination.
      Set<C> explanation = theoryResult.getExplanation();

      List<Literal> literals = new ArrayList<>();
      for (C constraint : explanation) {
        String literalName = cnf.getConstraintLiteralMap().get(constraint);
        if (literalName != null) {
          literals.add(new Literal(literalName).not());
        } else if (constraint.isNegatable()) {
          @SuppressWarnings("unchecked")
          C positive = (C) constraint.negate();
          String positiveName = cnf.getConstraintLiteralMap().get(positive);
          if (positiveName != null) {
            literals.add(new Literal(positiveName));
          } else {
            System.err.println(
                "FullLazySMTSolver: explanation constraint not in atom map, skipping: "
                    + constraint);
          }
        } else {
          System.err.println(
              "FullLazySMTSolver: explanation constraint not in atom map, skipping: " + constraint);
        }
      }

      Clause clause = new Clause(literals);
      cnf.getBooleanStructure().learnClause(clause);
    }

    if (objectivePresent && foundOptimizationSolution) {
      // Return the best optimum found across all enumerated theory-consistent models.
      // The unbounded case is reported as satisfiable with the witnessing solution.
      return SMTResult.satisfiable(bestSolution);
    }

    return SMTResult.unsatisfiable();
  }

  private boolean isMaximizing(Set<C> selectedConstraints) {
    for (C constraint : selectedConstraints) {
      if (constraint instanceof me.paultristanwagner.satchecking.theory.LinearConstraint.MaximizingConstraint) {
        return true;
      }
      if (constraint instanceof me.paultristanwagner.satchecking.theory.LinearConstraint.MinimizingConstraint) {
        return false;
      }
    }
    // Default: SimplexOptimizationSolver internally maximizes; without an explicit objective
    // constraint in the selected set we conservatively treat it as maximization.
    return true;
  }
}
