package me.paultristanwagner.satchecking.smt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        List<String> variables = new ArrayList<>( assignments.keySet() );
        variables.sort( String::compareTo );

        for ( String variable : variables ) {
            Double value = assignments.get( variable );
            builder.append( variable ).append( "=" ).append( value ).append( "; " );
        }

        return builder.toString();
    }
}
