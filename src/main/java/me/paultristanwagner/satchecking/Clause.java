package me.paultristanwagner.satchecking;

import java.util.List;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class Clause {
    
    private final List<Literal> literals;
    
    public Clause( List<Literal> literals ) {
        this.literals = literals;
    }
    
    public static Clause parse( String string ) {
        CNF cnf = CNF.parse( "(" + string + ")" );
        return cnf.getClauses().stream().findFirst().get();
    }
    
    public List<Literal> getLiterals() {
        return literals;
    }
    
    public boolean contains( String literalName ) {
        for ( Literal literal : literals ) {
            if ( literal.getName().equals( literalName ) ) {
                return true;
            }
        }
        return false;
    }
    
    public Literal isAsserting( Assignment assignment ) {
        int intersectionSize = 0;
        Literal assertingLiteral = null;
        for ( Literal literal : literals ) {
            if ( assignment.getAssignmentLevelOf( literal ) == assignment.getDecisionLevel() ) {
                intersectionSize++;
                assertingLiteral = literal;
                
                if ( intersectionSize == 2 ) {
                    return null;
                }
            }
        }
        
        if ( intersectionSize == 1 ) {
            return assertingLiteral;
        }
        return null;
    }
    
    @Override
    public String toString() {
        if ( literals.isEmpty() ) {
            return "()";
        }
        
        StringBuilder sb = new StringBuilder();
        for ( Literal literal : literals ) {
            sb.append( " | " ).append( literal );
        }
        return sb.substring( 3 );
    }
}
