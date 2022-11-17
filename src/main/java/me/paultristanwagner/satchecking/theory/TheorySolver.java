package me.paultristanwagner.satchecking.theory;

import me.paultristanwagner.satchecking.smt.Constraint;
import me.paultristanwagner.satchecking.smt.VariableAssignment;

import java.util.Set;

// todo: proof of concept
public interface TheorySolver<T extends Constraint> {

    VariableAssignment solve( Set<T> constraints );
}
