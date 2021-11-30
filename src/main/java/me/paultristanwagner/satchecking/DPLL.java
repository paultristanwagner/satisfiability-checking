package me.paultristanwagner.satchecking;

import java.util.List;
import java.util.Optional;

import static me.paultristanwagner.satchecking.DPLLResult.SAT;
import static me.paultristanwagner.satchecking.DPLLResult.UNSAT;

public class DPLL {
    
    public static DPLLResult enumeration( CNF cnf ) {
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
        assignment.assign( literal, false );
    }
    
    public DPLLResult dpll( CNF cnf ) {
        // todo
        return null;
    }
}
