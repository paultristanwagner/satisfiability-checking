package me.paultristanwagner.satchecking.theory.solver;

import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.TheoryResult;

import java.util.HashSet;
import java.util.Set;

public class LinearIntegerSolver implements TheorySolver<LinearConstraint> {
  
  private Set<LinearConstraint> constraints;
  
  public LinearIntegerSolver() {
    this.constraints = new HashSet<>();
  }
  
  @Override
  public void clear() {
    this.constraints.clear();
  }
  
  @Override
  public void addConstraint( LinearConstraint constraint ) {
    this.constraints.add( constraint );
  }
  
  @Override
  public TheoryResult<LinearConstraint> solve() {
    SimplexOptimizationSolver simplexSolver = new SimplexOptimizationSolver();
    simplexSolver.load( constraints );
    TheoryResult<LinearConstraint> result = simplexSolver.solve();
    
    if( !result.isSatisfiable() ) {
      return result;
    }
  
    VariableAssignment assignment = result.getSolution();
    boolean integral = true;
    String firstNonIntegralVariable = null;
    double nonIntegralValue = 0;
    
    for(String variable : assignment.getVariables()) {
      double value = assignment.getAssignment( variable );
      if(value != Math.floor( value )) { // todo: maybe add epsilon here
        integral = false;
        firstNonIntegralVariable = variable;
        nonIntegralValue = value;
        break;
      }
    }
    
    if(integral) {
      return result;
    }
    
    LinearConstraint upperBound = new LinearConstraint();
    upperBound.setCoefficient( firstNonIntegralVariable, 1 );
    upperBound.setValue( Math.floor( nonIntegralValue ) );
    
    LinearIntegerSolver solverA = new LinearIntegerSolver();
    solverA.load( constraints );
    solverA.addConstraint( upperBound );
   
    TheoryResult<LinearConstraint> aResult = solverA.solve();
    if(aResult.isSatisfiable()) {
      return aResult;
    }
    
    LinearConstraint lowerBound = new LinearConstraint();
    lowerBound.setCoefficient( firstNonIntegralVariable, 1 );
    lowerBound.setValue( Math.ceil( nonIntegralValue ) );
    
    LinearIntegerSolver solverB = new LinearIntegerSolver();
    solverB.load( constraints );
    solverB.addConstraint( lowerBound );
    
    TheoryResult<LinearConstraint> bResult = solverB.solve();
    if(bResult.isSatisfiable()) {
      return bResult;
    }
    
    return TheoryResult.unsatisfiable( constraints );
  }
}
