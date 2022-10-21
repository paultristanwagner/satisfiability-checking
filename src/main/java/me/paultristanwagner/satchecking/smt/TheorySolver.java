package me.paultristanwagner.satchecking.smt;

import java.util.Set;

// todo: proof of concept
public interface TheorySolver<T extends Constraint> {
    
    VariableAssignment solve( Set<T> constraints );
}
