package me.paultristanwagner.satchecking;

import static me.paultristanwagner.satchecking.Result.SAT;
import static me.paultristanwagner.satchecking.Result.UNSAT;

public class EnumerationSolver {
    
    public static Result check( CNF cnf ) {
        Assignment assignment = new Assignment();
        while ( true ) {
            if ( assignment.fits( cnf ) ) {
                boolean evaluation = assignment.evaluate( cnf );
                if ( evaluation ) {
                    return SAT( assignment );
                } else if ( !assignment.backtrack() ) {
                    return UNSAT;
                }
            } else {
                assignment.decide( cnf );
            }
        }
    }
}
