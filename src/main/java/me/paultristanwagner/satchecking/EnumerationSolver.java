package me.paultristanwagner.satchecking;

import java.util.List;
import java.util.Optional;

import static me.paultristanwagner.satchecking.Result.SAT;
import static me.paultristanwagner.satchecking.Result.UNSAT;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class EnumerationSolver {
    
    public static Result check( CNF cnf ) {
        Assignment assignment = new Assignment();
        while ( true ) {
            if ( assignment.fits( cnf ) ) {
                boolean evaluation = assignment.evaluate( cnf );
                if ( evaluation ) {
                    return SAT( assignment );
                } else if ( !backtrack( assignment ) ) {
                    return UNSAT;
                }
            } else {
                decide( cnf, assignment );
            }
        }
    }

    private static void decide( CNF cnf, Assignment assignment ) {
        List<Literal> literals = cnf.getLiterals();
        Optional<Literal> unassignedOptional = literals.stream().filter( lit -> !assignment.assigns( lit ) ).findAny();
        if ( unassignedOptional.isEmpty() ) {
            throw new IllegalStateException( "Cannot decide because all literals are assigned" );
        }
        Literal literal = unassignedOptional.get();
        assignment.assign( literal, true );
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
