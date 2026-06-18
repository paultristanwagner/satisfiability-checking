package me.paultristanwagner.satchecking.theory.solver;

import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.SimplexResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Dispatcher for QF_LRA. Accumulates linear constraints and routes them to the appropriate Simplex
 * backend on {@link #solve()}:
 *
 * <ul>
 *   <li>If any constraint is an optimization objective ({@link
 *       LinearConstraint.MaximizingConstraint} / {@link LinearConstraint.MinimizingConstraint}),
 *       delegate to a fresh {@link SimplexOptimizationSolver}.
 *   <li>Otherwise (objective-free feasibility) delegate to a fresh {@link
 *       SimplexFeasibilitySolver}, which supports strict inequalities via the delta-rational
 *       method.
 * </ul>
 */
public class LinearArithmeticSolver implements TheorySolver<LinearConstraint> {

  private final List<LinearConstraint> constraints = new ArrayList<>();

  @Override
  public void clear() {
    constraints.clear();
  }

  @Override
  public void addConstraint(LinearConstraint constraint) {
    constraints.add(constraint);
  }

  @Override
  public SimplexResult solve() {
    boolean optimization = false;
    for (LinearConstraint constraint : constraints) {
      if (constraint instanceof LinearConstraint.MaximizingConstraint
          || constraint instanceof LinearConstraint.MinimizingConstraint) {
        optimization = true;
        break;
      }
    }

    if (!optimization) {
      // Objective-free feasibility: the feasibility solver supports strict inequalities exactly
      // via the delta-rational method.
      SimplexFeasibilitySolver delegate = new SimplexFeasibilitySolver();
      for (LinearConstraint constraint : constraints) {
        delegate.addConstraint(constraint);
      }
      return (SimplexResult) delegate.solve();
    }

    // Optimization: the SimplexOptimizationSolver does NOT support strict bounds. Strict
    // inequalities only enter this path through Boolean-negated atoms during SMT enumeration.
    // We relax each strict bound to its non-strict closure (LESS -> LESS_EQUALS, GREATER ->
    // GREATER_EQUALS) so the optimum is computed over the closure of the feasible region. This
    // matches the documented scope: strict support lives in the feasibility solver only.
    SimplexOptimizationSolver delegate = new SimplexOptimizationSolver();
    for (LinearConstraint constraint : constraints) {
      delegate.addConstraint(relaxStrict(constraint));
    }
    return (SimplexResult) delegate.solve();
  }

  private static LinearConstraint relaxStrict(LinearConstraint constraint) {
    return switch (constraint.getBound()) {
      case LESS ->
          LinearConstraint.lessThanOrEqual(
              constraint.getLeftHandSide(), constraint.getRightHandSide());
      case GREATER ->
          LinearConstraint.greaterThanOrEqual(
              constraint.getLeftHandSide(), constraint.getRightHandSide());
      default -> constraint;
    };
  }
}
