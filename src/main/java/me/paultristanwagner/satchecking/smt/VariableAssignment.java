package me.paultristanwagner.satchecking.smt;

import java.util.HashMap;
import java.util.Map;

public class VariableAssignment {

    private final Map<String, Double> assignments = new HashMap<>();

    public void assign( String variable, double value ) {
        assignments.put( variable, value );
    }

    public double getAssignment( String variable ) {
        return assignments.get( variable );
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        this.assignments.forEach( ( variable, value ) ->
                builder.append( variable ).append( "=" ).append( value ).append( "; " )
        );

        return builder.toString();
    }
}
