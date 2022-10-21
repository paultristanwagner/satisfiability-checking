package me.paultristanwagner.satchecking.smt;

import java.util.Set;

// todo: proof of concept
public class EqualityLogicSolver<T extends Constraint> implements TheorySolver<T> {
    
    @Override
    public VariableAssignment solve( Set<T> constraints ) {
        throw new UnsupportedOperationException();
    }
}
