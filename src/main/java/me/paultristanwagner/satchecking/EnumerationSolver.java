package me.paultristanwagner.satchecking;

import static me.paultristanwagner.satchecking.Result.SAT;
import static me.paultristanwagner.satchecking.Result.UNSAT;

public class EnumerationSolver {
    
    public static Result check( CNF cnf ) {
        Assignment assignment = new Assignment( cnf );
        while ( true ) {
            if ( assignment.fits( cnf ) ) {
                boolean evaluation = assignment.evaluate( cnf );
                if ( evaluation ) {
                    return SAT( assignment );
                } else if ( !backtrack( assignment ) ) {
                    return UNSAT;
                }
            } else {
                assignment.decide( cnf );
            }
        }
    }
    
    private static boolean backtrack( Assignment assignment ) {
        if ( assignment.isEmpty() ) {
            return true;
        }
        
        while ( !assignment.isEmpty() ) {
            LiteralAssignment la = assignment.getLastDecision();
            if ( la.wasPreviouslyAssigned() ) {
                assignment.undoLastDecision();
            } else {
                la.toggleValue();
                la.setPreviouslyAssigned();
                return true;
            }
        }
        return false;
    }
}
