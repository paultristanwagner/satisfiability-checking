package me.paultristanwagner.satchecking;

import java.util.Set;

import static me.paultristanwagner.satchecking.DPLLResult.SAT;
import static me.paultristanwagner.satchecking.DPLLResult.UNSAT;

public class DPLL {
    
    public static void main( String[] args ) {
        CNF cnf = new CNF(
                new Clause( Literal.of( 'a' ) ),
                new Clause( Literal.not( 'a' ), Literal.not( 'b' ) )
        );
        
        DPLLResult result = enumeration( cnf );
        System.out.println( result );
    }
    
    private static DPLLResult enumeration( CNF cnf ) {
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
        Set<Character> characters = cnf.getCharacters();
        characters.removeAll( assignment.getAssignedCharacters() );
        if ( characters.isEmpty() ) {
            throw new IllegalStateException( "Cannot decide because all characters are assigned" );
        }
        Character c = characters.stream().findAny().get();
        assignment.assign( c, false );
    }
    
    public DPLLResult check( CNF cnf ) {
        return null;
    }
}
