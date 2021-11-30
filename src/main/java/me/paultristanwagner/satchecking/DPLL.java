package me.paultristanwagner.satchecking;

import java.util.List;

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
        List<Character> characters = cnf.getCharacters();
        characters.removeAll( assignment.getAssignedCharacters() );
        if ( characters.isEmpty() ) {
            throw new IllegalStateException( "Cannot decide because all characters are assigned" );
        }
        Character c = characters.stream().findAny().get();
        assignment.assign( c, false );
    }
    
    public DPLLResult dpll( CNF cnf ) {
        // todo
        return null;
    }
}
