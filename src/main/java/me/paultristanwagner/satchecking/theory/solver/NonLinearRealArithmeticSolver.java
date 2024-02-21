package me.paultristanwagner.satchecking.theory.solver;

import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.TheoryResult;
import me.paultristanwagner.satchecking.theory.nonlinear.CAD;
import me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomial;
import me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomialConstraint;
import me.paultristanwagner.satchecking.theory.nonlinear.RealAlgebraicNumber;

import java.util.HashSet;
import java.util.Set;

import static me.paultristanwagner.satchecking.theory.TheoryResult.satisfiable;
import static me.paultristanwagner.satchecking.theory.TheoryResult.unsatisfiable;
import static me.paultristanwagner.satchecking.theory.nonlinear.MultivariatePolynomialConstraint.Comparison.EQUALS;

public class NonLinearRealArithmeticSolver implements TheorySolver<MultivariatePolynomialConstraint> {

  private final Set<MultivariatePolynomialConstraint> constraints = new HashSet<>();

  @Override
  public void clear() {
    constraints.clear();
  }

  @Override
  public void load(Set<MultivariatePolynomialConstraint> constraints) {
    clear();
    this.constraints.addAll(constraints);
  }

  @Override
  public void addConstraint(MultivariatePolynomialConstraint constraint) {
    constraints.add(constraint);
  }

  @Override
  public TheoryResult<MultivariatePolynomialConstraint> solve() {
    boolean onlyEqualities = true;
    Set<MultivariatePolynomial> polynomials = new HashSet<>();
    for (MultivariatePolynomialConstraint constraint : constraints) {
      polynomials.add(constraint.getPolynomial());
      if (!constraint.getComparison().equals(EQUALS)) {
        onlyEqualities = false;
      }
    }

    CAD cad = new CAD(polynomials);
    Set<VariableAssignment<RealAlgebraicNumber>> result = cad.compute(constraints, onlyEqualities);

    for (VariableAssignment<RealAlgebraicNumber> realAlgebraicNumberVariableAssignment : result) {
      boolean satisfied = true;
      for (MultivariatePolynomialConstraint constraint : constraints) {
        int sign = constraint.getPolynomial().evaluateSign(realAlgebraicNumberVariableAssignment);
        if(!constraint.getComparison().evaluateSign(sign)) {
          satisfied = false;
          break;
        }
      }

      if(satisfied) {
        return satisfiable(realAlgebraicNumberVariableAssignment);
      }
    }

    return unsatisfiable(constraints);
  }
}
