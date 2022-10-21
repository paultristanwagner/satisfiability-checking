package me.paultristanwagner.satchecking.smt;

import java.util.List;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class TheoryClause<T extends Constraint> {
    
    private final List<T> constraints;
    
    public TheoryClause( List<T> constraints ) {
        this.constraints = constraints;
    }
    
    public static <T extends Constraint> TheoryClause<T> parse( String string ) {
        throw new UnsupportedOperationException();
    }
    
    public List<T> getConstraints() {
        return constraints;
    }
    
    @Override
    public String toString() {
        if ( constraints.isEmpty() ) {
            return "()";
        }
        
        StringBuilder sb = new StringBuilder();
        for ( Constraint constraint : constraints ) {
            sb.append( " | [" ).append( constraint ).append( "]" );
        }
        return sb.substring( 3 );
    }
}
