package me.paultristanwagner.satchecking.smt;

public interface SMTSolver<T extends Constraint> {

    VariableAssignment solve( TheoryCNF<T> cnf );
}
