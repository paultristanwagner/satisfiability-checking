package me.paultristanwagner.satchecking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DPLL {
    
    public static void main( String[] args ) {
        CNF cnf = new CNF(
                new Clause( Literal.of( 'a' ) ),
                new Clause( Literal.not( 'a' ), Literal.not( 'b' ) )
        );
        
        List<Character> characters = new ArrayList<>( cnf.getCharacters() );
        Collections.sort( characters );
        
        Assignment assignment = new Assignment();
        
        Character recent = characters.get( 0 );
        boolean eval;
        do {
            while ( !assignment.fits( cnf ) ) {
                do {
                    boolean assignSuccess = assignment.assignNext( recent );
                    if ( !assignSuccess ) {
                        System.out.println( "no success on assign" );
                        assignment.undo();
                        recent = assignment.getLastAssigned();
                        System.out.println( assignment );
                    }
                    recent = characters.get( ( characters.indexOf( recent ) + 1 ) % characters.size() );
                    System.out.println( recent );
                } while ( recent != characters.get( 0 ) );
            }
            eval = assignment.evaluate( cnf );
            
            System.out.println( eval + " " + assignment );
            
            if ( !eval ) {
                assignment.undo();
                recent = characters.get( ( characters.indexOf( recent ) + 1 ) % characters.size() );
            }
        } while ( !eval );
    }
    
    public DPLLResult check( CNF cnf ) {
        return null;
    }
}
