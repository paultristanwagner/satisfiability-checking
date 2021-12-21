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
    
    public boolean isAsserting( Assignment assignment ) {
        int intersectionSize = 0;
        List<LiteralAssignment> assignmentsOnCurrentLevel = assignment.getAssignmentsOnCurrentLevel();
        for ( Literal literal : literals ) {
            if ( assignmentsOnCurrentLevel.stream().anyMatch( la -> la.getLiteralName().equals( literal.getName() ) ) ) {
                intersectionSize++;
                if ( intersectionSize == 2 ) {
                    return false;
                }
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for ( Literal literal : literals ) {
            sb.append( " | " ).append( literal );
        }
        return sb.substring( 3 );
    }
}
