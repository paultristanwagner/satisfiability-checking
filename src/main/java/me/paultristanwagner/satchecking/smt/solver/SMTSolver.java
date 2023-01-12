package me.paultristanwagner.satchecking.smt.solver;

import me.paultristanwagner.satchecking.sat.solver.SATSolver;
import me.paultristanwagner.satchecking.smt.TheoryCNF;
import me.paultristanwagner.satchecking.smt.VariableAssignment;
import me.paultristanwagner.satchecking.theory.Constraint;
import me.paultristanwagner.satchecking.theory.solver.TheorySolver;

public abstract class SMTSolver<C extends Constraint> {

  protected SATSolver satSolver;
  protected TheorySolver<C> theorySolver;
  protected TheoryCNF<C> cnf;

  public void setSATSolver(SATSolver satSolver) {
    this.satSolver = satSolver;
  }

  public void setTheorySolver(TheorySolver<C> theorySolver) {
    this.theorySolver = theorySolver;
  }

  public void load(TheoryCNF<C> theoryCNF) {
    this.cnf = theoryCNF;
  }

  public abstract VariableAssignment solve();
}
