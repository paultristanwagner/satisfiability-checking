package me.paultristanwagner.satchecking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Clause {
    
    private final List<Literal> literals;
    
    public Clause( Literal... literals ) {
        this.literals = Arrays.asList( literals );
    }
    
    public List<Literal> getUnassignedLiterals( Assignment assignment ) {
        List<Literal> unassigned = new ArrayList<>();
        for ( Literal literal : literals ) {
            if ( !assignment.assigns( literal ) ) {
                unassigned.add( literal );
            }
        }
        return unassigned;
    }
    
    public boolean isUnit( Assignment assignment ) {
        // todo: when is a clause unit
        // when it is still unsatisfied and there is exactly one unassigned literal left
        int assignedLiterals = 0;
        for ( Literal literal : literals ) {
            if ( assignment.assigns( literal ) ) {
                if ( assignment.evaluate( literal ) ) {
                    return false;
                }
                assignedLiterals++;
            }
        }
        return getLiterals().size() - assignedLiterals == 1;
    }
    
    public static Clause parse( String string ) {
        CNF cnf = CNF.parse( "(" + string + ")" );
        return cnf.getClauses().get( 0 );
    }
    
    public List<Literal> getLiterals() {
        return literals;
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
